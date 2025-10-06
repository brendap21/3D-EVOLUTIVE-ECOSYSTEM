package entities;

import math.Vector3;
import java.awt.Color;
import render.SoftwareRenderer;
import math.Camera;

public class Cubo implements Renderable {
    public Vector3 posicion;
    public int tamano;
    public Color color;

    public Cubo(Vector3 pos, int tamano, Color color) {
        this.posicion = pos;
        this.tamano = tamano;
        this.color = color;
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        // Calculamos los 8 vértices del cubo
        double t = tamano / 2.0;
        Vector3[] vertices = new Vector3[]{
            new Vector3(posicion.x - t, posicion.y - t, posicion.z - t),
            new Vector3(posicion.x + t, posicion.y - t, posicion.z - t),
            new Vector3(posicion.x + t, posicion.y + t, posicion.z - t),
            new Vector3(posicion.x - t, posicion.y + t, posicion.z - t),
            new Vector3(posicion.x - t, posicion.y - t, posicion.z + t),
            new Vector3(posicion.x + t, posicion.y - t, posicion.z + t),
            new Vector3(posicion.x + t, posicion.y + t, posicion.z + t),
            new Vector3(posicion.x - t, posicion.y + t, posicion.z + t)
        };

        renderer.drawCube(vertices, cam, color);
    }
}
