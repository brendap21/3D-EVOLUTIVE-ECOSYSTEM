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
    // Add button for spawning animals
    private int addButtonX = -1, addButtonY = -1, addButtonW = 40, addButtonH = 40;
    // reference to the world so we can save/load animals from the pause menu
    private simulation.Mundo mundo;

    public RenderPanel(int ancho, int alto){
        this.ancho = ancho;
        this.alto = alto;
        renderer = new SoftwareRenderer(ancho, alto);
        setPreferredSize(new java.awt.Dimension(ancho, alto));
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (width > 0 && height > 0 && (width != ancho || height != alto)) {
            this.ancho = width;
            this.alto = height;
            renderer = new SoftwareRenderer(ancho, alto);
        }
    }

    public void setMundo(simulation.Mundo m){ this.mundo = m; }

    // Ahora acepta controles para saber si mostrar la mira (crosshair)
    public void render(List<Renderable> entidades, Camera cam, Controles controles){
        renderer.clear(Color.BLACK);
        for(Renderable e : entidades){
            if (controles == null || !controles.isPaused()) {
                e.update();
            }
            e.render(renderer, cam);
        }

        // Skip expensive hole-filling for performance (trade visual perfection for speed)
        // renderer.fillTinyHoles();
        // renderer.fillHorizontalSeams();

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

            // draw buttons via helper to reduce repetition
            drawButton(renderer, btnX, btnY, btnW, btnH, new Color(50,50,80), contTxt, 2);
            int saveY = btnY + btnH + 12;
            drawButton(renderer, btnX, saveY, btnW, btnH, new Color(40,80,40), "GUARDAR", 2);
            int loadY = saveY + btnH + 12;
            drawButton(renderer, btnX, loadY, btnW, btnH, new Color(40,40,80), "CARGAR", 2);
            int exitY = loadY + btnH + 12;
            drawButton(renderer, btnX, exitY, btnW, btnH, new Color(80,40,40), exitTxt, 2);

            // store last button rects for hit-testing
            this.lastContX = btnX; this.lastContY = btnY; this.lastContW = btnW; this.lastContH = btnH;
            this.lastSaveX = btnX; this.lastSaveY = saveY; this.lastSaveW = btnW; this.lastSaveH = btnH;
            this.lastLoadX = btnX; this.lastLoadY = loadY; this.lastLoadW = btnW; this.lastLoadH = btnH;
            this.lastExitX = btnX; this.lastExitY = exitY; this.lastExitW = btnW; this.lastExitH = btnH;
        } else {
            // Draw "+" button in bottom-left corner when not paused
            addButtonX = 8;
            addButtonY = alto - addButtonH - 8;
            renderer.fillRect(addButtonX, addButtonY, addButtonW, addButtonH, new Color(60, 100, 60));
            renderer.drawLine2D(addButtonX + 2, addButtonY + addButtonH/2, addButtonX + addButtonW - 2, addButtonY + addButtonH/2, Color.WHITE);
            renderer.drawLine2D(addButtonX + addButtonW/2, addButtonY + 2, addButtonX + addButtonW/2, addButtonY + addButtonH - 2, Color.WHITE);
            
            // Draw status if waiting for spawn
            if (mundo != null && mundo.isWaitingForSpawn()) {
                String msg = "Click terrain to spawn";
                int msgScale = 1;
                PixelFont.drawText(renderer, 50, alto - 30, msg, msgScale, Color.YELLOW);
            }
        }

        // Swap buffers so the completed backBuffer becomes the displayed frontBuffer
        renderer.swapBuffers();
        repaint();
    }

    @Override
    protected void paintComponent(java.awt.Graphics g){
        super.paintComponent(g);
        g.drawImage(renderer.getBuffer(), 0, 0, getWidth(), getHeight(), null);
    }

    // Handle mouse pressed events routed from the outer app. We don't register a
    // MouseListener here directly because EcosistemaApp previously attached one.
    public void handleMousePressed(java.awt.event.MouseEvent e, Controles controles){
        int mx = e.getX();
        int my = e.getY();
        int buttonCode = e.getButton();
        
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
                System.exit(0);
            }
            return;
        }
        
        // Left click: select animal or spawn position or click "+" button
        if (buttonCode == java.awt.event.MouseEvent.BUTTON1) {
            // Check if clicked on "+" button
            if (mx >= addButtonX && mx < addButtonX + addButtonW && my >= addButtonY && my < addButtonY + addButtonH) {
                if (mundo != null) {
                    mundo.setWaitingForSpawn(true);
                }
                return;
            }
            
            if (mundo != null && mundo.isWaitingForSpawn()) {
                // Click to set spawn position
                handleSpawnPositionClick(mx, my, controles);
            } else {
                // Try to select an animal
                handleAnimalSelection(mx, my, controles);
            }
            return;
        }
        
        // Right click or other: lock mouse for camera control
        if (controles != null) controles.lockMouse(true);
    }
    
    private void handleAnimalSelection(int mx, int my, Controles controles){
        if (mundo == null || controles == null) return;
        
        Camera cam = controles.getCamera(); // Need to add this to Controles
        if (cam == null) return;
        
        // Simple AABB click detection: check if any animal is at this screen position
        java.util.List<entities.Animal> animals = mundo.getAnimals();
        double minDist = Double.MAX_VALUE;
        entities.Animal closest = null;
        
        for (entities.Animal animal : animals) {
            Vector3 pos = animal.getPosicion();
            double[] proj = renderer.project(pos, cam);
            if (proj != null) {
                double px = proj[0];
                double py = proj[1];
                double dx = mx - px;
                double dy = my - py;
                double dist = dx*dx + dy*dy;
                
                if (dist < 400 && dist < minDist) { // 20 pixel radius
                    minDist = dist;
                    closest = animal;
                }
            }
        }
        
        // Toggle selection
        if (closest != null) {
            if (mundo.getSelectedAnimal() == closest) {
                mundo.setSelectedAnimal(null);
            } else {
                mundo.setSelectedAnimal(closest);
            }
        } else {
            mundo.setSelectedAnimal(null);
        }
    }
    
    private void handleSpawnPositionClick(int mx, int my, Controles controles){
        if (mundo == null || controles == null) return;
        
        Camera cam = controles.getCamera();
        if (cam == null) return;
        
        // Raycast to find where user clicked on the ground plane (y=0)
        Vector3 pos = cam.getPosicion();
        double[] screenToWorld = raycastToGround(mx, my, cam);
        
        if (screenToWorld != null) {
            double spawnX = screenToWorld[0];
            double spawnZ = screenToWorld[1];
            
            // Get terrain height at spawn position
            double terrainHeight = mundo.getHeightAt(spawnX, spawnZ);
            if (terrainHeight == Double.NEGATIVE_INFINITY) terrainHeight = 0.0;
            double spawnY = terrainHeight + 1.0; // 1 unit above terrain
            
            // Spawn a new animal at this position
            long seed = System.currentTimeMillis();
            int type = new java.util.Random().nextInt(10);
            main.Renderable animal = main.EcosistemaApp.createAnimalOfType(type, new Vector3(spawnX, spawnY, spawnZ), seed);
            if (animal instanceof entities.Animal) {
                mundo.addAnimal((entities.Animal) animal);
            }
            
            mundo.setWaitingForSpawn(false);
        }
    }
    
    private double[] raycastToGround(int screenX, int screenY, Camera cam){
        // Simple raycast: unproject screen point and find intersection with ground plane (y=0)
        // This is a simplified version - in production you'd do proper ray-plane intersection
        
        Vector3 camPos = cam.getPosicion();
        // Approximate: shoot a ray from camera position through the screen pixel
        // For simplicity, assume the ray direction is based on screen position
        
        double nx = ((double)screenX / ancho - 0.5) * 2; // -1 to 1
        double ny = ((double)(alto - screenY) / alto - 0.5) * 2; // -1 to 1
        
        // Rough ray direction (would need proper unproject for accuracy)
        Vector3 rayDir = new Vector3(nx * 50, -50, ny * 50 - 100).normalize();
        
        // Find intersection with y=0 plane
        // Ray: P = camPos + t*rayDir
        // Plane: y = 0
        // 0 = camPos.y + t*rayDir.y
        // t = -camPos.y / rayDir.y
        
        if (Math.abs(rayDir.y) < 0.001) return null;
        double t = -camPos.y / rayDir.y;
        if (t < 0) return null;
        
        double hitX = camPos.x + t * rayDir.x;
        double hitZ = camPos.z + t * rayDir.z;
        
        return new double[]{hitX, hitZ};
    }

    // Helper: draw a rectangular pixel-button with centered pixel-font text.
    private void drawButton(render.SoftwareRenderer renderer, int x, int y, int w, int h, java.awt.Color bg, String text, int textScale){
        renderer.fillRect(x, y, w, h, bg);
        renderer.drawLine2D(x, y, x + w - 1, y, java.awt.Color.WHITE);
        renderer.drawLine2D(x, y + h - 1, x + w - 1, y + h - 1, java.awt.Color.WHITE);
        renderer.drawLine2D(x, y, x, y + h - 1, java.awt.Color.WHITE);
        renderer.drawLine2D(x + w - 1, y, x + w - 1, y + h - 1, java.awt.Color.WHITE);
        int charW = 5*textScale + 1*textScale;
        int tw = text.length() * charW;
        int tx = x + (w - tw)/2;
        int ty = y + (h - 7*textScale)/2;
        render.PixelFont.drawText(renderer, tx, ty, text, textScale, java.awt.Color.WHITE);
    }
}
