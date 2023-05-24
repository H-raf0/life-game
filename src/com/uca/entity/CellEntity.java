package com.uca.entity;

import java.util.List;
import java.util.ArrayList;

/**
 * Represente une cellule de coordonné x et y
*/
public class CellEntity {
    private final int x;
    private final int y;

    public CellEntity(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public String toString() {
        return x+","+y;
    }
}
