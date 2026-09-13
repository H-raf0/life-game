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

    /**
     * insert revived cells or deleted the dead ones
     * @param newGen list of cells to be changed
     * @param connect connection of a specific session
    */
    public void updateCellsStates(List<CellEntity> newGen, Connection connect){

        try {
            String query1 = "INSERT INTO grid (x, y) VALUES (?, ?)";
            String query2 = "DELETE FROM grid WHERE x = ? AND y = ?;";
            PreparedStatement statement1 = connect.prepareStatement(query1);
            PreparedStatement statement2 = connect.prepareStatement(query2);
            for (CellEntity cell : newGen) {
                if(cell.getState()==1){ // if the cell should be revived 
                    statement1.setInt(1, cell.getX());
                    statement1.setInt(2, cell.getY());
                    statement1.addBatch();
                }else{                 // if the cell should be killed
                    statement2.setInt(1, cell.getX());
                    statement2.setInt(2, cell.getY());
                    statement2.addBatch();
                }
            }
            // Exécuter les inserts par lots
            statement1.executeBatch();
            statement2.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connect.rollback(); // Rollback the transaction if an exception occurs
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}