package com.uca;

import com.uca.dao._Initializer;
import com.uca.dao._Connector;
import com.uca.gui.*;

import com.uca.core.GridCore;
import com.uca.entity.CellEntity;

import com.google.gson.Gson;

import java.sql.*;
import static spark.Spark.*;
import spark.*;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


public class StartServer {


    public static void main(String[] args) {
        //Configuration de Spark
        staticFiles.location("/static/");
        port(8081);

        // Création de la base de données, si besoin
        _Initializer.Init();

        /**
         * Définition des routes
         */

        // index de l'application
        get("/", (req, res) -> {
                return IndexGUI.getIndex();
            });

        // retourne l'état de la grille
        get("/grid", (req, res) -> {
                res.type("application/json");
                return new Gson().toJson(GridCore.getGrid(getConnection(req)));
            });

        // inverse l'état d'une cellule 
        put("/grid/change", (req, res) -> {
                Gson gson = new Gson();
                CellEntity selectedCell = (CellEntity) gson.fromJson(req.body(), CellEntity.class);

                GridCore.toggleCellState(selectedCell, getConnection(req));

                return "";
            });

        // sauvegarde les modifications de la grille 
        post("/grid/save", (req, res) -> {

                Connection c = getConnection(req);
                c.commit();
                
                return "";
            });

        // annule les modifications de la grille 
        post("/grid/cancel", (req, res) -> {
                Connection c = getConnection(req);
                c.rollback();
                return "";
            });

        // charge un fichier rle depuis un URL
        put("/grid/rle", (req, res) -> {
                Connection c = getConnection(req);
                GridCore.emptyGrid(c);

                String RLEUrl = req.body();
                GridCore.loadFromRLE(RLEUrl, c);

                return "";
            });

        // vide la grille
        post("/grid/empty", (req, res) -> {
                GridCore.emptyGrid(getConnection(req));
                return "";
            });

        // met à jour la grille en la remplaçant par la génération suivante
        post("/grid/next", (req, res) -> {
                GridCore.goNext(getConnection(req));
                return "";
            });

    }

    /**
     * retourne le numéro de session
     * il y a un numéro de session différent pour chaque onglet de navigateur
     * ouvert sur l'application
     */
    public static int getSession(Request req) {
        return Integer.parseInt(req.queryParams("session"));
    }

    // on stock pour chaque session une connection à la BDD
    public static HashMap<Integer, Connection> connectedSession = new HashMap<>();

    /**
     * retourne la connection de chaque session
     * s'il n'existe pas il cree une nouvelle connection
     */
    public static Connection getConnection(Request req) throws SQLException{
        int session = getSession(req);
        if (connectedSession.containsKey(session)){
            return connectedSession.get(session);
        
        }else{
            Connection c = _Connector.getNewConnection();
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            c.setAutoCommit(false);
            connectedSession.put(session, c);
            return c;
        }
    }
}
