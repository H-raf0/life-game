package com.uca.dao;

import java.sql.*;

public class _Initializer {
    // nom de la table contenant la grille
    final static String TABLE = "grid";
    // taille de grille
    final static int SIZE = 100;

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

                // Remplir la table avec l'état initial
                fillTable(connection);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        /*finally {
            // Fermer la connexion à la base de données
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }*/
    }

    /**
     * Crée la structure de la table
     */
    private static void createTable(Connection connection) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("CREATE TABLE " + TABLE + " (x INT NOT NULL, y INT NOT NULL, state INT NOT NULL, PRIMARY KEY (x, y))");
        statement.executeUpdate();
        statement.close();
        System.out.println("La table " + TABLE + " a été créée avec succès.");
    }

    /**
     * Remplit la table avec l'état initial
     */
    private static void fillTable(Connection connection) throws SQLException {
        // le niveau d'isolation des transactions
        connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        connection.setAutoCommit(false);

        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO " + TABLE + " (x, y, state) VALUES (?, ?, ?)");

            // Ajouter des cellules mortes sur un carré fini
            for (int i = 0; i < SIZE; i++) {
                for (int j = 0; j < SIZE; j++) {
                    statement.setInt(1, i);
                    statement.setInt(2, j);
                    statement.setInt(3, 0); // État initial pour une cellule morte
                    statement.addBatch();
                }
            }
            // Exécuter les inserts par lots
            statement.executeBatch();

            // Valider la transaction
            connection.commit();
            System.out.println("La table " + TABLE + " a été remplie avec succès.");
        } catch (SQLException e) {
            // En cas d'erreur, annuler la transaction
            connection.rollback();
            throw e;
        } finally {
            // Rétablir le mode de commit automatique
            connection.setAutoCommit(true);
        }
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
