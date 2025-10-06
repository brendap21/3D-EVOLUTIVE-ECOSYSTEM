package entities;

import math.Vector3;
import java.awt.Color;

public class Cilindro {
    public Vector3 posicion; // centro base
    public int radio;
    public int altura;
    public Color color;

    public Cilindro(Vector3 posicion, int radio, int altura, Color color) {
        this.posicion = posicion;
        this.radio = radio;
        this.altura = altura;
        this.color = color;
    }
}
