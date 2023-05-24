package com.uca.entity;

import java.util.List;
import java.util.ArrayList;

/**
 * Représente l'état de la grille
 * Toutes les cellules de l'état de la grille sont vivantes
 */
public class GridEntity {
    // état de la grille : liste des cellules vivantes
    private final List<CellEntity> state;

    public GridEntity() {
        state = new ArrayList<>();
    }

    /**
     * retourne la grille
     */
    public List<CellEntity> getCells() {
        return state;
    }

    /**
     * ajoute une cellule vivante à la grille 
     */
    public void addCell(CellEntity c) {
        state.add(c);
    }
}
