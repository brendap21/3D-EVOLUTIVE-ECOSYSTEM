package entities;

import main.Renderable;
import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;

public class Cubo implements Renderable {
    private Vector3 posicion;
    private int tamano;
    private Color color;
    private double rotY = 0; // rotación en Y

    public Cubo(Vector3 posicion, int tamano, Color color){
        this.posicion = posicion;
        this.tamano = tamano;
        this.color = color;
    }

    @Override
    public void update(){
        rotY += 0.01; // incrementa rotación cada frame
        if(rotY > 2*Math.PI) rotY -= 2*Math.PI;
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam){
        Vector3[] vertices = renderer.getCubeVertices(posicion, tamano, rotY);
        renderer.drawCube(vertices, cam, color);
    }
}
