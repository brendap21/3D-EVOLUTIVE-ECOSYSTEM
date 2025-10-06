package entities;

import java.awt.Color;
import math.Vector3;
import math.Camera;
import render.SoftwareRenderer;

public class Cilindro implements Renderable {
    public Vector3 posicion;
    public int radio, altura;
    public Color color;
    private double rotY = 0;

    public Cilindro(Vector3 pos, int radio, int altura, Color color){
        this.posicion = pos;
        this.radio = radio;
        this.altura = altura;
        this.color = color;
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam){
        Vector3[] top = renderer.getCylinderTopVertices(posicion, radio, altura, rotY);
        Vector3[] bottom = renderer.getCylinderBottomVertices(posicion, radio, altura, rotY);
        renderer.drawCylinder(top, bottom, cam, color);
    }

    @Override
    public void update(){
        rotY += 0.01; // Rotación continua
    }
}
