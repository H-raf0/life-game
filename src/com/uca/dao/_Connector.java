package com.uca.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

public class _Connector {

    private static String url = "jdbc:postgresql://localhost/LifeGame";
    private static String user = "Hraf";
    private static String passwd = "achraf";

    public static Connection connect;

    public static Connection getMainConnection(){
        if(connect == null){
            connect = getNewConnection();
        }
        return connect;
    }

    public static Connection getNewConnection() {
        Connection c;
        try {
            c = DriverManager.getConnection(url, user, passwd);
        } catch (SQLException e) {
            System.err.println("Erreur en ouvrant une nouvelle connection.");
            throw new RuntimeException(e);
        }
        return c;
    }
}
