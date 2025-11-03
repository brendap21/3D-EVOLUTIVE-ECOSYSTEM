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
    private int lastSaveX = -1, lastSaveY = -1, lastSaveW = 0, lastSaveH = 0;
    private int lastLoadX = -1, lastLoadY = -1, lastLoadW = 0, lastLoadH = 0;
    // reference to the world so we can save/load animals from the pause menu
    private simulation.Mundo mundo;

    public RenderPanel(int ancho, int alto){
        this.ancho = ancho;
        this.alto = alto;
        renderer = new SoftwareRenderer(ancho, alto);
    }

    public void setMundo(simulation.Mundo m){ this.mundo = m; }

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

        // Debug overlay: show camera/terrain info and draw camera axes projected to screen
        if(controles != null && controles.isDebugOverlayEnabled()){
            int margin = 8;
            int sx = margin, sy = margin;
            String yawS = String.format("yaw: %.3f", cam.getYaw());
            String pitchS = String.format("pitch: %.3f", cam.getPitch());
            String camYS = String.format("camY: %.3f", cam.getPosicion().y);
            double terrainH = Double.NEGATIVE_INFINITY;
            if(mundo != null) terrainH = mundo.getHeightAt(cam.getPosicion().x, cam.getPosicion().z);
            String terrS = String.format("terrain: %.3f", terrainH);
            int textScale = 2;
            PixelFont.drawText(renderer, sx, sy, yawS, textScale, Color.WHITE);
            PixelFont.drawText(renderer, sx, sy + 12, pitchS, textScale, Color.WHITE);
            PixelFont.drawText(renderer, sx, sy + 24, camYS, textScale, Color.WHITE);
            PixelFont.drawText(renderer, sx, sy + 36, terrS, textScale, Color.WHITE);

            // Draw small camera axes from screen center: right (red), up (green), forward (blue)
            int cx = ancho/2, cy = alto/2;
            math.Vector3 camPos = cam.getPosicion();
            math.Vector3 forward = cam.getForward().normalize();
            math.Vector3 worldUp = new math.Vector3(0,1,0);
            math.Vector3 right = worldUp.cross(forward).normalize();
            math.Vector3 up = forward.cross(right).normalize();

            // Project small points ahead in each axis
            double scale = 40.0;
            double[] pR = renderer.project(new Vector3(camPos.x + right.x*scale, camPos.y + right.y*scale, camPos.z + right.z*scale), cam);
            double[] pU = renderer.project(new Vector3(camPos.x + up.x*scale, camPos.y + up.y*scale, camPos.z + up.z*scale), cam);
            double[] pF = renderer.project(new Vector3(camPos.x + forward.x*scale, camPos.y + forward.y*scale, camPos.z + forward.z*scale), cam);
            if(pR != null) renderer.drawLine2D(cx, cy, (int)pR[0], (int)pR[1], Color.RED);
            if(pU != null) renderer.drawLine2D(cx, cy, (int)pU[0], (int)pU[1], Color.GREEN);
            if(pF != null) renderer.drawLine2D(cx, cy, (int)pF[0], (int)pF[1], Color.BLUE);
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

            // Save button below Continue
            int saveY = btnY + btnH + 12;
            renderer.fillRect(btnX, saveY, btnW, btnH, new Color(40,80,40));
            renderer.drawLine2D(btnX, saveY, btnX+btnW-1, saveY, Color.WHITE);
            renderer.drawLine2D(btnX, saveY+btnH-1, btnX+btnW-1, saveY+btnH-1, Color.WHITE);
            renderer.drawLine2D(btnX, saveY, btnX, saveY+btnH-1, Color.WHITE);
            renderer.drawLine2D(btnX+btnW-1, saveY, btnX+btnW-1, saveY+btnH-1, Color.WHITE);
            String saveTxt = "GUARDAR";
            int saveScale = 2;
            int saveCharW = 5*saveScale + 1*saveScale;
            int saveW = saveTxt.length()*saveCharW;
            int saveTextX = cx - saveW/2;
            int saveTextY = saveY + (btnH - 7*saveScale)/2;
            PixelFont.drawText(renderer, saveTextX, saveTextY, saveTxt, saveScale, Color.WHITE);

            // Load button below Save
            int loadY = saveY + btnH + 12;
            renderer.fillRect(btnX, loadY, btnW, btnH, new Color(40,40,80));
            renderer.drawLine2D(btnX, loadY, btnX+btnW-1, loadY, Color.WHITE);
            renderer.drawLine2D(btnX, loadY+btnH-1, btnX+btnW-1, loadY+btnH-1, Color.WHITE);
            renderer.drawLine2D(btnX, loadY, btnX, loadY+btnH-1, Color.WHITE);
            renderer.drawLine2D(btnX+btnW-1, loadY, btnX+btnW-1, loadY+btnH-1, Color.WHITE);
            String loadTxt = "CARGAR";
            int loadScale = 2;
            int loadCharW = 5*loadScale + 1*loadScale;
            int loadW = loadTxt.length()*loadCharW;
            int loadTextX = cx - loadW/2;
            int loadTextY = loadY + (btnH - 7*loadScale)/2;
            PixelFont.drawText(renderer, loadTextX, loadTextY, loadTxt, loadScale, Color.WHITE);

            // Exit button below
            int exitY = loadY + btnH + 12;
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
            this.lastSaveX = btnX; this.lastSaveY = saveY; this.lastSaveW = btnW; this.lastSaveH = btnH;
            this.lastLoadX = btnX; this.lastLoadY = loadY; this.lastLoadW = btnW; this.lastLoadH = btnH;
            this.lastExitX = btnX; this.lastExitY = exitY; this.lastExitW = btnW; this.lastExitH = btnH;
        }

        // Swap buffers so the completed backBuffer becomes the displayed frontBuffer
        renderer.swapBuffers();
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
            // If inside Save
            if (mx >= lastSaveX && mx < lastSaveX + lastSaveW && my >= lastSaveY && my < lastSaveY + lastSaveH){
                if(mundo != null){
                    java.io.File f = new java.io.File("animals.txt");
                    simulation.Persistencia.saveAnimals(f, mundo.getAnimals());
                }
                return;
            }
            // If inside Load
            if (mx >= lastLoadX && mx < lastLoadX + lastLoadW && my >= lastLoadY && my < lastLoadY + lastLoadH){
                if(mundo != null){
                    java.io.File f = new java.io.File("animals.txt");
                    java.util.List<entities.Animal> loaded = simulation.Persistencia.loadAnimals(f);
                    for(entities.Animal a : loaded){
                        mundo.addAnimal(a);
                    }
                }
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
