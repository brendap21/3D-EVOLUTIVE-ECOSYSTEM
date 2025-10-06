package entities;

import main.Renderable;
import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;

public class Cilindro implements Renderable {
    private Vector3 posicion;
    private int radio, altura;
    private Color color;
    private int segmentos = 12;
    private double rotY = 0;

    public Cilindro(Vector3 posicion, int radio, int altura, Color color){
        this.posicion = posicion;
        this.radio = radio;
        this.altura = altura;
        this.color = color;
    }

    @Override
    public void update(){ 
        rotY += 0.005; // rotación más lenta que cubo
        if(rotY > 2*Math.PI) rotY -= 2*Math.PI;
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam){
        Vector3[] top = renderer.getCylinderTopVertices(posicion, radio, altura, rotY);
        Vector3[] bottom = renderer.getCylinderBottomVertices(posicion, radio, altura, rotY);
        renderer.drawCylinder(top, bottom, cam, color);
    }
}
