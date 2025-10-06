package entities;

import java.awt.Color;
import math.Vector3;
import math.Camera;
import render.SoftwareRenderer;

public class Cubo implements Renderable {
    public Vector3 posicion;
    public int tamano;
    public Color color;
    private double rotY = 0;

    public Cubo(Vector3 pos, int tamano, Color color){
        this.posicion = pos;
        this.tamano = tamano;
        this.color = color;
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam){
        Vector3[] vertices = renderer.getCubeVertices(posicion, tamano, rotY);
        renderer.drawCube(vertices, cam, color);
    }

    @Override
    public void update(){
        rotY += 0.01; // Rotación continua
    }
}
