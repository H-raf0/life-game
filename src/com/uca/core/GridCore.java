package com.uca.core;

import com.uca.dao.*;
import com.uca.entity.*;

import java.net.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.regex.*;


public class GridCore {

    /**
     * load alive cells et return them
     * @param connect connection of a specific session
     * @return list of alive cells
    */
    public static List<CellEntity> getGrid(Connection connect) throws SQLException {
        GridEntity grid = new GridEntity();
        loadCells(grid, connect);

        return grid.getCells();
    }

    public static List<CellEntity> getGrid(int minX, int maxX, int minY, int maxY, Connection connect) {
        GridEntity grid = new GridEntity();
        new GridDAO().getLivingCellsInArea(grid, minX, maxX, minY, maxY, connect);
        return grid.getCells();
    }

    public static int[] getGridBounds(Connection connect) throws SQLException {
        return new GridDAO().getGridBounds(connect);
    }

    /**
     * load all alive cells
     * @param connect connection of a specific session
     * @param grid the entity that will hold the loaded alive cells
    */
    public static void loadCells(GridEntity grid, Connection connect){
        new GridDAO().getAllLivingCells(grid, connect);
    }

    /**
     * switch the state of a cell
     * @param connect connection of a specific session
     * @param cell cell which we will cahnge its state
    */
    public static void toggleCellState(CellEntity cell, Connection connect){
        new GridDAO().updateCellState(cell.getX(), cell.getY(), connect);
    }

    /**
     * clear the grid
     * @param connect connection of a specific session
    */
    public static void emptyGrid(Connection connect){
        new GridDAO().clearGrid(connect);
    }

    /**
     * load an RLE
     * @param connect connection of a specific session
     * @param RLEUrl RLEUrl that contains the shape we want
    */
    public static void loadFromRLE(String RLEUrl, Connection connect) throws Exception {
        //decoding RLEUrl to cells list
        List<CellEntity> decodedCells = decodeRLEUrl(RLEUrl);
        if (decodedCells.isEmpty()) {
            throw new IllegalArgumentException("Le fichier RLE ne contient aucune cellule.");
        }
        //applying those changes to the data base
        new GridDAO().loadCellsRLE(decodedCells, connect);
    }
    
    /**
     * jump to the next generation
     * @param connect connection of a specific session
    */
    public static void goNext(Connection connect) {
        long started = System.nanoTime();
        GridEntity grid = new GridEntity();
        loadCells(grid, connect);
        List<CellEntity> aliveCells = grid.getCells();
        long loaded = System.nanoTime();
        if (shouldUseDenseGeneration(aliveCells)) {
            goNextDense(aliveCells, connect);
            long finished = System.nanoTime();
            System.out.println("[perf] generation: " + aliveCells.size() + " cells, load "
                + ((loaded - started) / 1_000_000) + " ms, dense compute+update "
                + ((finished - loaded) / 1_000_000) + " ms");
            return;
        }

        Set<Long> aliveSet = new HashSet<>(aliveCells.size() * 2);
        Map<Long, Integer> neighborCounts = new HashMap<>(aliveCells.size() * 4);
        for (CellEntity cell : aliveCells) {
            aliveSet.add(coordinateKey(cell.getX(), cell.getY()));
        }
        for (CellEntity cell : aliveCells) {
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    if (x == 0 && y == 0) continue;
                    long neighbor = coordinateKey(cell.getX() + x, cell.getY() + y);
                    neighborCounts.put(neighbor, neighborCounts.getOrDefault(neighbor, 0) + 1);
                }
            }
        }

        List<CellEntity> changes = new ArrayList<>();
        for (CellEntity cell : aliveCells) {
            long key = coordinateKey(cell.getX(), cell.getY());
            int neighbors = neighborCounts.getOrDefault(key, 0);
            if (neighbors < 2 || neighbors > 3) {
                cell.setState(0);
                changes.add(cell);
            }
        }
        for (Map.Entry<Long, Integer> entry : neighborCounts.entrySet()) {
            if (entry.getValue() == 3 && !aliveSet.contains(entry.getKey())) {
                CellEntity cell = new CellEntity((int) (entry.getKey() >> 32), (int) (long) entry.getKey());
                cell.setState(1);
                changes.add(cell);
            }
        }
        long computed = System.nanoTime();
        new GridDAO().updateCellsStates(changes, connect);
        long finished = System.nanoTime();
        System.out.println("[perf] generation: " + aliveCells.size() + " cells, load "
            + ((loaded - started) / 1_000_000) + " ms, sparse compute "
            + ((computed - loaded) / 1_000_000) + " ms, update "
            + ((finished - computed) / 1_000_000) + " ms");
    }

    private static boolean shouldUseDenseGeneration(List<CellEntity> aliveCells) {
        if (aliveCells.isEmpty()) return false;
        int minX = aliveCells.get(0).getX();
        int maxX = minX;
        int minY = aliveCells.get(0).getY();
        int maxY = minY;
        for (CellEntity cell : aliveCells) {
            minX = Math.min(minX, cell.getX());
            maxX = Math.max(maxX, cell.getX());
            minY = Math.min(minY, cell.getY());
            maxY = Math.max(maxY, cell.getY());
        }
        long area = (long) (maxX - minX + 3) * (maxY - minY + 3);
        return area <= 10_000_000L && area <= aliveCells.size() * 8L;
    }

    private static void goNextDense(List<CellEntity> aliveCells, Connection connect) {
        int minX = aliveCells.get(0).getX();
        int maxX = minX;
        int minY = aliveCells.get(0).getY();
        int maxY = minY;
        for (CellEntity cell : aliveCells) {
            minX = Math.min(minX, cell.getX());
            maxX = Math.max(maxX, cell.getX());
            minY = Math.min(minY, cell.getY());
            maxY = Math.max(maxY, cell.getY());
        }

        int width = maxX - minX + 3;
        int height = maxY - minY + 3;
        boolean[] alive = new boolean[width * height];
        for (CellEntity cell : aliveCells) {
            int x = cell.getX() - minX + 1;
            int y = cell.getY() - minY + 1;
            alive[y * width + x] = true;
        }

        List<CellEntity> changes = new ArrayList<>();
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int index = y * width + x;
                int neighbors = 0;
                neighbors += alive[index - width - 1] ? 1 : 0;
                neighbors += alive[index - width] ? 1 : 0;
                neighbors += alive[index - width + 1] ? 1 : 0;
                neighbors += alive[index - 1] ? 1 : 0;
                neighbors += alive[index + 1] ? 1 : 0;
                neighbors += alive[index + width - 1] ? 1 : 0;
                neighbors += alive[index + width] ? 1 : 0;
                neighbors += alive[index + width + 1] ? 1 : 0;

                boolean nextAlive = neighbors == 3 || (alive[index] && neighbors == 2);
                if (nextAlive != alive[index]) {
                    CellEntity cell = new CellEntity(x + minX - 1, y + minY - 1);
                    cell.setState(nextAlive ? 1 : 0);
                    changes.add(cell);
                }
            }
        }
        new GridDAO().updateCellsStates(changes, connect);
    }

    public static List<CellEntity> nextGeneration(List<CellEntity> aliveCells) {
        Set<Long> aliveSet = new HashSet<>(aliveCells.size() * 2);
        Map<Long, Integer> neighborCounts = new HashMap<>(aliveCells.size() * 4);
        for (CellEntity cell : aliveCells) {
            aliveSet.add(coordinateKey(cell.getX(), cell.getY()));
        }
        for (CellEntity cell : aliveCells) {
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    if (x == 0 && y == 0) continue;
                    long neighbor = coordinateKey(cell.getX() + x, cell.getY() + y);
                    neighborCounts.put(neighbor, neighborCounts.getOrDefault(neighbor, 0) + 1);
                }
            }
        }
        List<CellEntity> next = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : neighborCounts.entrySet()) {
            if (entry.getValue() == 3 ||
                (entry.getValue() == 2 && aliveSet.contains(entry.getKey()))) {
                next.add(new CellEntity((int) (entry.getKey() >> 32), (int) (long) entry.getKey()));
            }
        }
        return next;
    }

    private static long coordinateKey(int x, int y) {
        return ((long) x << 32) ^ (y & 0xffffffffL);
    }


    /**
     * count the number of neighbors, and also if she finds a dead one she will tries to revive it
     * @param cell is the cell that we will count how much alive neighbors she have
     * @param aliveCells is the list of alive cells
     * @param newGen if we modifie any cell, we will add it here
     * @param countOnly if true then it will only count neighbors and not try to revive dead ones (usefull to avoid loops)
     * @return the number of neighbors of an alive cell
    */
    private static int countAndReviveNeighbors(CellEntity cell, List<CellEntity> aliveCells, List<CellEntity> newGen, boolean countOnly) {
        int count = 0; // number of neighbors
        for (int x = -1; x <= 1; x++) {  // double for to access all neighbors x and y
            for (int y = -1; y <= 1; y++) {
                if (x == 0 && y == 0) { //if the cell and her neighbor are the same
                    continue; // Skip the current cell
                }
                CellEntity neighbor = new CellEntity(cell.getX() + x, cell.getY() + y); // create a cell for the neighbor
                if (aliveCells.contains(neighbor)) { // if the neighbor is alive
                    count++;
                }else if (!countOnly){ // if the neighbor is dead and count !=1
                    neighbor.setState(0); // change state to 0 which means the cell is dead
                    reviveCell(neighbor, aliveCells, newGen); //we try to revive it
                }
            }
        }
        return count;
    }


    /**
     * tries to revives a dead cell
     * @param cell is the cell that we will try to revive
     * @param aliveCells is the list of alive cells
     * @param newGen if we modifie any cell, we will add it here
    */
    private static void reviveCell(CellEntity cell, List<CellEntity> aliveCells, List<CellEntity> newGen){
        int neighborsCount = countAndReviveNeighbors(cell, aliveCells, newGen, true); // we only count the neighbors
        if (shouldBeAlive(cell, neighborsCount) == 1 && !newGen.contains(cell)){ // we check if a cell should be revived
            cell.setState(1); // we revive it
            newGen.add(cell); // and add it to "nexGen"
        }
    }

    /**
     * check what will happens to a cell in the next generation
     * @param cell is the cell that we will check what its next status is gonna be
     * @param aliveNeighbors is the number of alive neighbors
     * @return 0 it will dies, 1 it will be revived, 2 nothing will changes
    */
    private static int shouldBeAlive(CellEntity cell, int aliveNeighbors) {
        if (cell.getState() == 1){ // if the cell is alive
            if (aliveNeighbors < 2 || aliveNeighbors > 3) { // overpopulation or underpopulation
                return 0; //dies
            }
            // Any live cell with two or three live neighbors remains alive
            else {        // (aliveNeighbors == 2 || aliveNeighbors == 3)
                return 2; // stays the same
            }
        }
        else { // if the cell is dead
            if (aliveNeighbors == 3) {
                return 1; // revived
            }else{
                return 2; // stays the same
            }
        }
    }







    /**
     * Décode le contenu d'un fichier RLE sous forme de cases à partir d'un URL
     * @param url - url d'un fichier RLE, ex : https://copy.sh/life/examples/glider.rle
     */
    public static List<CellEntity> decodeRLEUrl(String url) throws Exception {
        URL u = new URL(url);
        BufferedReader in = new BufferedReader(
        new InputStreamReader(u.openStream()));

        StringBuffer sb = new StringBuffer();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            sb.append(inputLine);
            //System.out.println(inputLine);
            sb.append("\n");
        }
        
        in.close();

        return decodeRLE(sb.toString());
    }
    
    /**
     * Décode le contenu d'un fichier RLE sous forme de cases
     * @param rle - un chaîne représentant une serialisation RLE
     */
    public static List<CellEntity> decodeRLE(String rle) {
        List<CellEntity> cells = new ArrayList<>();
        boolean ignore = false;
        int step = 1;
        int x = 50;
        int y = 50;
        String number;
        Pattern pattern = Pattern.compile("^[0-9]+");
        int i = -1; 
        while (i < rle.length() - 1) {
            i++;
            if (ignore) {
                if (rle.charAt(i) == '\n') {
                    ignore = false;
                }
                continue;
            }
            switch (rle.charAt(i)) {
            case '#':
            case 'x':
            case '!':
                ignore = true;
                continue;
            case '$':
                x = 50;
                y += step;
                step = 1;
                continue;
            case 'b':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
                x += step;
                step = 1;
                continue;
            case 'o':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
                for (int j = 0; j < step; j++) {
                    CellEntity c = new CellEntity(x++, y);
                    //System.out.println(c);
                    cells.add(c);
                }
                //System.out.println(rle.substring(Math.max(0, rle.lastIndexOf("$",i)))); 
                step = 1;
                continue;
            }
            Matcher matcher = pattern.matcher(rle.substring(i));
            if (matcher.find()) {
                number = matcher.group();
                step = Integer.parseInt(number);
                i += number.length() - 1;
            }
        }
        return cells;
    }
}
