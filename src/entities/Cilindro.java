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

    public Cilindro(Vector3 posicion, int radio, int altura, Color color){
        this.posicion = posicion;
        this.radio = radio;
        this.altura = altura;
        this.color = color;
    }

    @Override
    public void update(){ }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam){
        Vector3[] top = new Vector3[segmentos];
        Vector3[] bottom = new Vector3[segmentos];
        for(int i=0;i<segmentos;i++){
            double angle = 2*Math.PI*i/segmentos;
            double dx = Math.cos(angle)*radio;
            double dz = Math.sin(angle)*radio;
            top[i] = new Vector3(posicion.x+dx, posicion.y+altura, posicion.z+dz);
            bottom[i] = new Vector3(posicion.x+dx, posicion.y, posicion.z+dz);
        }
        renderer.drawCylinder(top, bottom, cam, color);
    }
}
