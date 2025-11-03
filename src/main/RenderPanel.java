package main;

import javax.swing.JPanel;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.util.List;
import math.Camera;
import render.SoftwareRenderer;
import render.PixelFont;
import ui.Controles;
import math.Vector3;
import java.awt.event.MouseEvent;

public class RenderPanel extends JPanel {
    private int ancho, alto;
    private SoftwareRenderer renderer;
    // Last rendered pause-button rectangles (for hit testing)
    private int lastContX = -1, lastContY = -1, lastContW = 0, lastContH = 0;
    private int lastExitX = -1, lastExitY = -1, lastExitW = 0, lastExitH = 0;

    public RenderPanel(int ancho, int alto){
        this.ancho = ancho;
        this.alto = alto;
        renderer = new SoftwareRenderer(ancho, alto);
    }

    // Ahora acepta controles para saber si mostrar la mira (crosshair)
    public void render(List<Renderable> entidades, Camera cam, Controles controles){
        renderer.clear(Color.BLACK);
        for(Renderable e : entidades){
            // If paused, skip updates to freeze rotations and simulation.
            if (controles == null || !controles.isPaused()) {
                e.update();
            }
            e.render(renderer, cam);
        }

        // Draw crosshair axis lines if requested (always centered on screen)
        if(controles != null && controles.isCrosshairVisible()){
            int cx = ancho / 2;
            int cy = alto / 2;
            int len = 12; // pixel length for each small axis line

            // HUD crosshair: keep X and Y as screen-space lines centered on camera
            renderer.drawLine2D(cx - len, cy, cx + len, cy, Color.RED);
            renderer.drawLine2D(cx, cy - len, cx, cy + len, Color.GREEN);
            // Blue depth marker: draw a small pixel block exactly at the center
            int blueSize = 3;
            int half = blueSize/2;
            for(int yy = -half; yy <= half; yy++){
                for(int xx = -half; xx <= half; xx++){
                    renderer.drawPixel(cx + xx, cy + yy, Color.BLUE);
                }
            }
        }

        // If paused, draw a pixel-font pause menu overlay (drawn with our own methods).
        if (controles != null && controles.isPaused()){
            int cx = ancho/2;
            int cy = alto/2;
            // dark panel
            int pad = 40;
            int pw = ancho - pad*2;
            int ph = alto - pad*2;
            renderer.fillRect(pad, pad, pw, ph, new Color(10,10,15));

            // Title: PAUSA
            String title = "PAUSA";
            int scale = 4;
            int charWidth = 5*scale + 1*scale;
            int titleW = title.length() * charWidth;
            int titleX = cx - titleW/2;
            int titleY = pad + 20;
            PixelFont.drawText(renderer, titleX, titleY, title, scale, Color.WHITE);

            // Buttons: CONTINUAR and SALIR
            String contTxt = "CONTINUAR";
            String exitTxt = "SALIR";
            int btnW = 240;
            int btnH = 48;
            int btnX = cx - btnW/2;
            int btnY = titleY + 80;

            // Continue button
            renderer.fillRect(btnX, btnY, btnW, btnH, new Color(50,50,80));
            // border
            renderer.drawLine2D(btnX, btnY, btnX+btnW-1, btnY, Color.WHITE);
            renderer.drawLine2D(btnX, btnY+btnH-1, btnX+btnW-1, btnY+btnH-1, Color.WHITE);
            renderer.drawLine2D(btnX, btnY, btnX, btnY+btnH-1, Color.WHITE);
            renderer.drawLine2D(btnX+btnW-1, btnY, btnX+btnW-1, btnY+btnH-1, Color.WHITE);
            // center text
            int contScale = 2;
            int contCharW = 5*contScale + 1*contScale;
            int contW = contTxt.length()*contCharW;
            int contTextX = cx - contW/2;
            int contTextY = btnY + (btnH - 7*contScale)/2;
            PixelFont.drawText(renderer, contTextX, contTextY, contTxt, contScale, Color.WHITE);

            // Exit button below
            int exitY = btnY + btnH + 16;
            renderer.fillRect(btnX, exitY, btnW, btnH, new Color(80,40,40));
            renderer.drawLine2D(btnX, exitY, btnX+btnW-1, exitY, Color.WHITE);
            renderer.drawLine2D(btnX, exitY+btnH-1, btnX+btnW-1, exitY+btnH-1, Color.WHITE);
            renderer.drawLine2D(btnX, exitY, btnX, exitY+btnH-1, Color.WHITE);
            renderer.drawLine2D(btnX+btnW-1, exitY, btnX+btnW-1, exitY+btnH-1, Color.WHITE);
            int exitScale = 2;
            int exitCharW = 5*exitScale + 1*exitScale;
            int exitW = exitTxt.length()*exitCharW;
            int exitTextX = cx - exitW/2;
            int exitTextY = exitY + (btnH - 7*exitScale)/2;
            PixelFont.drawText(renderer, exitTextX, exitTextY, exitTxt, exitScale, Color.WHITE);

            // store last button rects for hit-testing
            this.lastContX = btnX; this.lastContY = btnY; this.lastContW = btnW; this.lastContH = btnH;
            this.lastExitX = btnX; this.lastExitY = exitY; this.lastExitW = btnW; this.lastExitH = btnH;
        }

        repaint();
    }

    @Override
    protected void paintComponent(java.awt.Graphics g){
        super.paintComponent(g);
        g.drawImage(renderer.getBuffer(), 0, 0, null);
    }

    // Handle mouse pressed events routed from the outer app. We don't register a
    // MouseListener here directly because EcosistemaApp previously attached one.
    public void handleMousePressed(java.awt.event.MouseEvent e, Controles controles){
        int mx = e.getX();
        int my = e.getY();
        if (controles != null && controles.isPaused()){
            // If inside Continue
            if (mx >= lastContX && mx < lastContX + lastContW && my >= lastContY && my < lastContY + lastContH){
                controles.setPaused(false);
                return;
            }
            // If inside Exit
            if (mx >= lastExitX && mx < lastExitX + lastExitW && my >= lastExitY && my < lastExitY + lastExitH){
                // Exit the application
                System.exit(0);
            }
            return;
        }
        // Not paused: clicking the panel should lock the mouse and enter mouse-look
        if (controles != null) controles.lockMouse(true);
    }
}
