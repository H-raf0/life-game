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
    public static void loadFromRLE(String RLEUrl, Connection connect) {

        try {
            //decoding RLEUrl to cells list
            List<CellEntity> decodedCells = decodeRLEUrl(RLEUrl);
            //applying those changes to the data baseS
            new GridDAO().loadCellsRLE(decodedCells, connect);
        } catch (Exception e) {

            e.printStackTrace();
        }
    }
    
    /**
     * jump to the next generation
     * @param connect connection of a specific session
    */
    public static void goNext(Connection connect){
        // loading the current alive cells
        GridEntity grid = new GridEntity();
        loadCells(grid, connect);

        // stocking those cells in the list "alivecells"
        List<CellEntity> aliveCells = grid.getCells();
        // cretaing a list where we will store all the changed lists
        List<CellEntity> newGen = new ArrayList<>();

        // deciding the future state of the cells
        for(CellEntity cell : aliveCells){
            int neighborsCount = countAndReviveNeighbors(cell, aliveCells, newGen, false);
            if (shouldBeAlive(cell, neighborsCount) == 0){
                cell.setState(0);
                newGen.add(cell);
            }
        }
        // updating cells in the data base bae of the informations we have from newGen
        new GridDAO().updateCellsStates(newGen, connect);
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
                x += step;
                step = 1;
                continue;
            case 'o':
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
