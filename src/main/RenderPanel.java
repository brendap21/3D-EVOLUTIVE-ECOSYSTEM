package main;

import javax.swing.JPanel;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.util.List;
import math.Camera;
import render.SoftwareRenderer;

public class RenderPanel extends JPanel {
    private int ancho, alto;
    private SoftwareRenderer renderer;

    public RenderPanel(int ancho, int alto){
        this.ancho = ancho;
        this.alto = alto;
        renderer = new SoftwareRenderer(ancho, alto);
    }

    public void render(List<Renderable> entidades, Camera cam){
        renderer.clear(Color.BLACK);
        for(Renderable e : entidades){
            e.update();
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
