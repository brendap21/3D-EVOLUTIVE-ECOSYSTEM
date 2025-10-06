package entities;

import java.awt.Color;
import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;

public class Cubo implements Renderable {
    public Vector3 posicion;
    public int tamano;
    public Color color;

    public Cubo(Vector3 posicion, int tamano, Color color) {
        this.posicion = posicion;
        this.tamano = tamano;
        this.color = color;
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        Vector3[] vertices = new Vector3[]{
            posicion,
            posicion.add(new Vector3(tamano,0,0)),
            posicion.add(new Vector3(tamano,tamano,0)),
            posicion.add(new Vector3(0,tamano,0)),
            posicion.add(new Vector3(0,0,tamano)),
            posicion.add(new Vector3(tamano,0,tamano)),
            posicion.add(new Vector3(tamano,tamano,tamano)),
            posicion.add(new Vector3(0,tamano,tamano))
        };
        renderer.drawCube(vertices, cam, color);
    }
}
