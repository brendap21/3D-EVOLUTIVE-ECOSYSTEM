package main;

import javax.swing.JPanel;
import java.awt.image.BufferedImage;
import java.util.List;
import render.SoftwareRenderer;
import math.Camera;
import entities.Renderable;

public class RenderPanel extends JPanel {
    private SoftwareRenderer renderer;
    private int ancho, alto;

    public RenderPanel(int ancho, int alto) {
        this.ancho = ancho;
        this.alto = alto;
        this.renderer = new SoftwareRenderer(ancho, alto);
    }

    public void render(List<Renderable> entidades, Camera cam){
        renderer.clear(java.awt.Color.BLACK);

        for(Renderable e : entidades){
            e.render(renderer, cam);
        }

        repaint();
    }

    public BufferedImage getBuffer() {
        return renderer.getBuffer();
    }

    @Override
    protected void paintComponent(java.awt.Graphics g){
        super.paintComponent(g);
        g.drawImage(renderer.getBuffer(), 0, 0, null);
    }
}
