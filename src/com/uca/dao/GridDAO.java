package com.uca.dao;


import java.sql.*;
import java.sql.Connection;

import java.util.ArrayList;
import java.util.List;

import com.uca.entity.CellEntity;
import com.uca.entity.GridEntity;

public class GridDAO {

    public Connection connect = _Connector.getMainConnection();

    public void getAllLivingCells(GridEntity grid){

        try {
            PreparedStatement preparedStatement = this.connect.prepareStatement("SELECT * FROM grid WHERE state = 1;");
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int x = resultSet.getInt("x");
                int y = resultSet.getInt("y");
                CellEntity entity = new CellEntity(x, y);

                grid.addCell(entity);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}