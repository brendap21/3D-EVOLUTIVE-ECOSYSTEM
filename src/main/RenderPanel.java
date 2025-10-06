package main;

import javax.swing.JPanel;
import java.awt.Color;
import render.SoftwareRenderer;
import entities.Renderable;
import math.Camera;
import java.util.List;

public class RenderPanel extends JPanel {
    private int ancho, alto;
    private SoftwareRenderer renderer;

    public RenderPanel(int ancho, int alto){
        this.ancho = ancho;
        this.alto = alto;
        renderer = new SoftwareRenderer(ancho, alto);
    }

    public void render(List<Renderable> objetos, Camera cam){
        renderer.clear(Color.BLACK);
        for(Renderable e : objetos){
            e.render(renderer, cam);
        }
        repaint();
    }

    @Override
    protected void paintComponent(java.awt.Graphics g){
        super.paintComponent(g);
        g.drawImage(renderer.getBuffer(), 0, 0, null);
    }
}
