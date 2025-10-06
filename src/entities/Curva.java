package entities;

import math.Vector3;
import java.awt.Color;

public class Curva {
    public Vector3[] puntos;
    public Color color;

    public Curva(Vector3[] puntos, Color color) {
        this.puntos = puntos;
        this.color = color;
    }
}
