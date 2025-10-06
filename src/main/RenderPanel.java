package main;

import javax.swing.JPanel;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.util.List;
import math.Camera;
import entities.Renderable;
import render.SoftwareRenderer;

public class RenderPanel extends JPanel {
    private BufferedImage buffer;
    private int ancho, alto;
    private Camera cam;
    private SoftwareRenderer renderer;

    public RenderPanel(int ancho, int alto, Camera cam){
        this.ancho = ancho;
        this.alto = alto;
        this.cam = cam;
        this.buffer = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        this.renderer = new SoftwareRenderer(ancho, alto); // Inicializamos SoftwareRenderer con ancho y alto
    }

    public void render(List<Renderable> entidades){
        // Limpiamos el buffer del renderer con negro
        renderer.clear(Color.BLACK);

        // Renderiza cada entidad usando SoftwareRenderer
        for(Renderable e : entidades){
            e.render(renderer, cam);
        }

        // Copiamos el buffer del renderer al panel
        BufferedImage rendImg = renderer.getBuffer();
        for(int y = 0; y < alto; y++){
            for(int x = 0; x < ancho; x++){
                buffer.setRGB(x, y, rendImg.getRGB(x, y));
            }
        }

        repaint();
    }

    @Override
    protected void paintComponent(java.awt.Graphics g){
        super.paintComponent(g);
        g.drawImage(buffer, 0, 0, null);
    }
}
