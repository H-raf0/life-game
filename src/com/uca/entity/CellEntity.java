package com.uca.entity;

import java.util.List;
import java.util.ArrayList;

/**
 * Represente une cellule de coordonné x et y
*/
public class CellEntity {
    private final int x;
    private final int y;
    private int state;

    public CellEntity(int x, int y) {
        this.x = x;
        this.y = y;
        this.state = 1;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getState(){
        return this.state;
    }

    public void setState(int s){
        this.state = s;
    }

    // 2 cells are equal if they have the same coordinates
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CellEntity other = (CellEntity) obj;
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + x;
        result = 31 * result + y;
        return result;
    }

    public String toString() {
        return x+","+y+","+state;
    }
}
