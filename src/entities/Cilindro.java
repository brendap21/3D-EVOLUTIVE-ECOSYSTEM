package entities;

import math.Vector3;
import java.awt.Color;
import render.SoftwareRenderer;
import math.Camera;

public class Cilindro implements Renderable {
    public Vector3 posicion; // centro base
    public int radio;
    public int altura;
    public Color color;
    public int segmentos = 16; // resolución del cilindro

    public Cilindro(Vector3 posicion, int radio, int altura, Color color) {
        this.posicion = posicion;
        this.radio = radio;
        this.altura = altura;
        this.color = color;
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        Vector3[] topVertices = new Vector3[segmentos];
        Vector3[] bottomVertices = new Vector3[segmentos];

        for (int i = 0; i < segmentos; i++) {
            double angle = 2 * Math.PI * i / segmentos;
            double x = posicion.x + radio * Math.cos(angle);
            double z = posicion.z + radio * Math.sin(angle);
            topVertices[i] = new Vector3(x, posicion.y + altura, z);
            bottomVertices[i] = new Vector3(x, posicion.y, z);
        }

        renderer.drawCylinder(topVertices, bottomVertices, cam, color);
    }
}
