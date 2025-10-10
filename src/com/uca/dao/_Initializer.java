package com.uca.dao;

import java.sql.*;

public class _Initializer {
    // nom de la table contenant la grille
    final static String TABLE = "grid";

    /**
     * cette méthode permet d'initialiser en créant une table pour la grille si elle n'existe pas
     */
    public static void Init() {
        Connection connection = _Connector.getMainConnection();
        try {
            // Vérifier si la table existe déjà
            if (!tableExists(connection, TABLE)) {
                // Créer la table
                createTable(connection);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            // Fermer la connexion à la base de données
            if (connection != null) {
                try {
                    connection.close();
                    _Connector.connect = null;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Crée la structure de la table
     */
    private static void createTable(Connection connection) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("CREATE TABLE " + TABLE + " (x INT NOT NULL, y INT NOT NULL, PRIMARY KEY (x, y))");
        statement.executeUpdate();
        statement.close();
        System.out.println("La table " + TABLE + " a été créée avec succès.");
    }

    /**
     * teste si une table existe dans la base de données
     */
    private static boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        ResultSet resultSet = meta.getTables(null, null, tableName, new String[]{"TABLE"});
        return resultSet.next();
    }
}
