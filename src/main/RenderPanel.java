package main;

import javax.swing.JPanel;
import java.awt.image.BufferedImage;
import java.awt.Color;
import math.Camera;
import math.Transform;
import math.Vector3;
import entities.Cubo;

public class RenderPanel extends JPanel {
    private BufferedImage buffer;
    private int ancho, alto;
    private Camera cam;

    public RenderPanel(int ancho, int alto, Camera cam){
        this.ancho = ancho;
        this.alto = alto;
        this.cam = cam;
        this.buffer = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
    }

    public void clear(Color c){
        int rgb = c.getRGB();
        for(int y=0;y<alto;y++){
            for(int x=0;x<ancho;x++){
                buffer.setRGB(x, y, rgb);
            }
        }
    }

    public void render(Cubo[] cubos){
        clear(Color.BLACK);

        for(Cubo c : cubos){
            Vector3 p2d = Transform.project(c.posicion, cam, ancho, alto);
            drawVoxel((int)p2d.x, (int)p2d.y, c.tamano, c.color);
        }

        repaint();
    }

    private void drawVoxel(int x, int y, int tam, Color color){
        int rgb = color.getRGB();
        for(int i=0;i<tam;i++){
            for(int j=0;j<tam;j++){
                int px = x+i;
                int py = y+j;
                if(px>=0 && px<ancho && py>=0 && py<alto){
                    buffer.setRGB(px, py, rgb);
                }
            }
        }
    }

    @Override
    protected void paintComponent(java.awt.Graphics g){
        super.paintComponent(g);
        g.drawImage(buffer, 0, 0, null);
    }
}
