package com.uca.dao;


import java.sql.*;
import java.sql.Connection;

import java.util.ArrayList;
import java.util.List;

import com.uca.entity.CellEntity;
import com.uca.entity.GridEntity;

public class GridDAO {

    /**
     * load all living cells and store them in the list inside grid
     * @param grid the entity that contains the list of alive cells
     * @param connect connection of a specific session
    */
    public void getAllLivingCells(GridEntity grid, Connection connect){

        try {
            PreparedStatement preparedStatement = connect.prepareStatement("SELECT x,y FROM grid;");
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int x = resultSet.getInt("x");
                int y = resultSet.getInt("y");
                CellEntity entity = new CellEntity(x, y);

                grid.addCell(entity);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connect.rollback(); // Rollback the transaction if an exception occurs
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void getLivingCellsInArea(GridEntity grid, int minX, int maxX, int minY, int maxY, Connection connect){
        try {
            PreparedStatement statement = connect.prepareStatement(
                "SELECT x,y FROM grid WHERE x BETWEEN ? AND ? AND y BETWEEN ? AND ?;");
            statement.setInt(1, minX);
            statement.setInt(2, maxX);
            statement.setInt(3, minY);
            statement.setInt(4, maxY);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                grid.addCell(new CellEntity(resultSet.getInt("x"), resultSet.getInt("y")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int[] getGridBounds(Connection connect) throws SQLException {
        PreparedStatement statement = connect.prepareStatement(
            "SELECT MIN(x), MAX(x), MIN(y), MAX(y) FROM grid;");
        ResultSet resultSet = statement.executeQuery();
        if (!resultSet.next() || resultSet.getObject(1) == null) return null;
        return new int[] {
            resultSet.getInt(1), resultSet.getInt(2),
            resultSet.getInt(3), resultSet.getInt(4)
        };
    }

    /**
     * if a cell is alive (exists) we kill it (remove it) if no we revive it (add it)
     * @param X x of the cell
     * @param Y y of the cell
     * @param connect connection of a specific session 
    */
    public void updateCellState(int X, int Y, Connection connect){

        try {
            // if delete the row if it exists, if else it adds it
            String query = "WITH deleted_rows AS ( " +
               "    DELETE FROM grid " +
               "    WHERE x = ? AND y = ? " +
               "    RETURNING * " +
               ")" +
               "INSERT INTO grid (x, y)" +
               "SELECT ?, ? " +
               "WHERE NOT EXISTS (" +
               "    SELECT 1 FROM deleted_rows" +
               ");";
               
            PreparedStatement statement = connect.prepareStatement(query);
            statement.setInt(1, X);
            statement.setInt(3, X);
            statement.setInt(2, Y);
            statement.setInt(4, Y);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connect.rollback(); // Rollback the transaction if an exception occurs
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

    }

    /**
     * clear the grid
     * @param connect connection of a specific session
    */
    public void clearGrid(Connection connect){
        try {            
            PreparedStatement statement = connect.prepareStatement("DELETE FROM grid;");
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connect.rollback(); // Rollback the transaction if an exception occurs
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * load cells from a RLE
     * @param cells list of cells that should be alive
     * @param connect connection of a specific session
    */
    public void loadCellsRLE(List<CellEntity> cells, Connection connect) throws SQLException {
        PreparedStatement statement = connect.prepareStatement("INSERT INTO grid (x, y) VALUES (?, ?)");
        for (CellEntity cell : cells) {
            statement.setInt(1, cell.getX());
            statement.setInt(2, cell.getY());
            statement.addBatch();
        }
        // Exécuter les inserts par lots
        statement.executeBatch();
    }

    public void replaceGrid(List<CellEntity> cells, Connection connect) throws SQLException {
        Statement clear = connect.createStatement();
        clear.executeUpdate("DELETE FROM grid");
        clear.close();
        PreparedStatement insert = connect.prepareStatement("INSERT INTO grid (x, y) VALUES (?, ?)");
        for (CellEntity cell : cells) {
            insert.setInt(1, cell.getX());
            insert.setInt(2, cell.getY());
            insert.addBatch();
        }
        insert.executeBatch();
        insert.close();
    }

    /**
     * insert revived cells or deleted the dead ones
     * @param newGen list of cells to be changed
     * @param connect connection of a specific session
    */
    public void updateCellsStates(List<CellEntity> newGen, Connection connect){
        if (newGen.isEmpty()) return;
        try {
            Statement setup = connect.createStatement();
            setup.executeUpdate(
                "CREATE TEMP TABLE IF NOT EXISTS grid_changes (" +
                "x INT NOT NULL, y INT NOT NULL, state INT NOT NULL, " +
                "PRIMARY KEY (x, y)) ON COMMIT DELETE ROWS");
            setup.executeUpdate("DELETE FROM grid_changes");
            setup.close();

            PreparedStatement changes = connect.prepareStatement(
                "INSERT INTO grid_changes (x, y, state) VALUES (?, ?, ?)");
            for (CellEntity cell : newGen) {
                changes.setInt(1, cell.getX());
                changes.setInt(2, cell.getY());
                changes.setInt(3, cell.getState());
                changes.addBatch();
            }
            changes.executeBatch();
            changes.close();

            Statement apply = connect.createStatement();
            apply.executeUpdate(
                "DELETE FROM grid g USING grid_changes c " +
                "WHERE c.state = 0 AND g.x = c.x AND g.y = c.y");
            apply.executeUpdate(
                "INSERT INTO grid (x, y) " +
                "SELECT x, y FROM grid_changes WHERE state = 1 " +
                "ON CONFLICT (x, y) DO NOTHING");
            apply.close();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connect.rollback(); // Rollback the transaction if an exception occurs
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void advanceGeneration(Connection connect) throws SQLException {
        Statement statement = connect.createStatement();
        statement.executeUpdate(
            "CREATE TEMP TABLE IF NOT EXISTS grid_next_generation (" +
            "x INT NOT NULL, y INT NOT NULL, PRIMARY KEY (x, y)" +
            ") ON COMMIT DELETE ROWS");
        statement.executeUpdate("DELETE FROM grid_next_generation");
        statement.executeUpdate(
            "WITH neighbor_counts AS (" +
            " SELECT g.x + offsets.dx AS x, g.y + offsets.dy AS y, COUNT(*) AS neighbors" +
            " FROM grid g CROSS JOIN (VALUES" +
            " (-1,-1),(-1,0),(-1,1),(0,-1),(0,1),(1,-1),(1,0),(1,1)" +
            " ) AS offsets(dx, dy)" +
            " GROUP BY g.x + offsets.dx, g.y + offsets.dy" +
            ") INSERT INTO grid_next_generation (x, y)" +
            " SELECT x, y FROM neighbor_counts WHERE neighbors = 3" +
            " UNION" +
            " SELECT n.x, n.y FROM neighbor_counts n" +
            " JOIN grid g ON g.x = n.x AND g.y = n.y" +
            " WHERE n.neighbors = 2");
        statement.executeUpdate("DELETE FROM grid");
        statement.executeUpdate("INSERT INTO grid (x, y) SELECT x, y FROM grid_next_generation");
        statement.close();
    }
}