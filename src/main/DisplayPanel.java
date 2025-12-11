package main;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

/**
 * DisplayPanel: Panel que muestra el BufferedImage renderizado.
 * ÚNICA responsabilidad: mostrar en pantalla la imagen pre-renderizada.
 * No dibuja contenido, solo copia la imagen.
 */
public class DisplayPanel extends JPanel {
    private BufferedImage renderedImage;
    private int width;
    private int height;

    public DisplayPanel(int width, int height) {
        this.width = width;
        this.height = height;
        this.renderedImage = null;
        setPreferredSize(new Dimension(width, height));
    }

    public void setImage(BufferedImage img) {
        this.renderedImage = img;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (renderedImage != null) {
            g.drawImage(renderedImage, 0, 0, width, height, null);
        }
    }
}
