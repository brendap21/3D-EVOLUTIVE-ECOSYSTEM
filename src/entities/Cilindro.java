package entities;

import java.awt.Color;
import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;

public class Cilindro implements Renderable {
    public Vector3 posicion;
    public int radio;
    public int altura;
    public Color color;
    public int lados; // Número de lados del cilindro

    public Cilindro(Vector3 posicion, int radio, int altura, int lados, Color color) {
        this.posicion = posicion;
        this.radio = radio;
        this.altura = altura;
        this.lados = lados;
        this.color = color;
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        Vector3[] top = new Vector3[lados];
        Vector3[] bottom = new Vector3[lados];
        double angleStep = 2*Math.PI/lados;

        for(int i=0;i<lados;i++){
            double angle = i*angleStep;
            top[i] = new Vector3(
                posicion.x + radio * Math.cos(angle),
                posicion.y + altura,
                posicion.z + radio * Math.sin(angle)
            );
            bottom[i] = new Vector3(
                posicion.x + radio * Math.cos(angle),
                posicion.y,
                posicion.z + radio * Math.sin(angle)
            );
        }

        renderer.drawCylinder(top, bottom, cam, color);
    }
}
