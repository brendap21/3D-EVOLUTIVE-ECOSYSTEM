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

    public Cubo(Vector3 posicion, int tamano, Color color){
        this.posicion = posicion;
        this.tamano = tamano;
        this.color = color;
    }

    @Override
    public void update(){
        // Aquí puedes animar el cubo si quieres
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam){
        Vector3[] vertices = new Vector3[8];
        double x = posicion.x, y = posicion.y, z = posicion.z;
        int t = tamano;
        vertices[0] = new Vector3(x,y,z);
        vertices[1] = new Vector3(x+t,y,z);
        vertices[2] = new Vector3(x+t,y+t,z);
        vertices[3] = new Vector3(x,y+t,z);
        vertices[4] = new Vector3(x,y,z+t);
        vertices[5] = new Vector3(x+t,y,z+t);
        vertices[6] = new Vector3(x+t,y+t,z+t);
        vertices[7] = new Vector3(x,y+t,z+t);

        renderer.drawCube(vertices, cam, color);
    }
}
