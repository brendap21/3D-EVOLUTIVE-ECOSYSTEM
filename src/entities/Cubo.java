package entities;

import math.Vector3;
import java.awt.Color;

public class Cubo {
    public Vector3 posicion;
    public int tamano;
    public Color color;

    public Cubo(Vector3 pos, int tamano, Color color){
        this.posicion = pos;
        this.tamano = tamano;
        this.color = color;
    }
}
