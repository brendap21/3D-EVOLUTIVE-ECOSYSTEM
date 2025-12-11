package main;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import entities.BaseAnimal;
import entities.Collidable;
import math.Camera;
import math.Vector3;
import render.PixelFont;
import render.SoftwareRenderer;
import simulation.Mundo;
import ui.AnimalSpawnerMenu;
import ui.Controles;
import ui.SaveGameDialog;
import ui.LoadGameDialog;

/**
 * RenderPanel: Renderizador 3D puro. No hereda de JPanel.
 * Todo el renderizado es píxel a píxel sin usar librerías gráficas.
 * Solo usa BufferedImage.setRGB() para dibujar.
 */
public class RenderPanel {
    private final SoftwareRenderer renderer;
    private final int ancho;
    private final int alto;
    private Mundo mundo;
    private final Random rng = new Random();

    private List<MenuItemBounds> lastMenuBounds = Collections.emptyList();
    private SpawnTarget latestSpawnTarget = null;

    private String transientMessage = "";
    private Color transientMessageColor = Color.WHITE;
    private long transientMessageUntil = 0L;

    private static final Color SKY_COLOR = new Color(120, 170, 255);

    private static class MenuItemBounds {
        final int index;
        final int x;
        final int y;
        final int w;
        final int h;

        MenuItemBounds(int index, int x, int y, int w, int h) {
            this.index = index;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        boolean contains(int px, int py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }

    private static class SpawnTarget {
        Vector3 position;
        boolean hasHit;
        boolean valid;
        String reason;
        double screenX;
        double screenY;
        int pixelRadius;
    }

    private static class ButtonBounds {
        final int x;
        final int y;
        final int w;
        final int h;
        final String action;

        ButtonBounds(int x, int y, int w, int h, String action) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.action = action;
        }

        boolean contains(int px, int py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }

    private List<ButtonBounds> pauseMenuButtons = Collections.emptyList();
    
    // Animal selection (can be BaseAnimal or Depredador)
    private Renderable hoveredAnimal = null;
    private Renderable selectedAnimal = null;
    private double animalPanelSlideProgress = 0.0;
    private ButtonBounds deleteAnimalButton = null;
    private boolean animalPanelActive = false; // Si el panel está activo
    
    // Referencias para guardar/cargar
    private Camera camera = null;
    private simulation.Simulador simulador = null;

    public RenderPanel(int ancho, int alto) {
        this.ancho = ancho;
        this.alto = alto;
        this.renderer = new SoftwareRenderer(ancho, alto);
    }

    public void setMundo(Mundo m) {
        this.mundo = m;
    }
    
    public void setCamera(Camera cam) {
        this.camera = cam;
    }
    
    public void setSimulador(simulation.Simulador sim) {
        this.simulador = sim;
    }

    public void render(List<Renderable> entidades, Camera cam, Controles controles) {
        renderer.clear(SKY_COLOR);
        
        // Detect hovered animal under cursor
        updateHoveredAnimal(entidades, cam, controles);
        
        // Update animal panel slide animation
        double targetSlide = selectedAnimal != null ? 1.0 : 0.0;
        if (animalPanelSlideProgress < targetSlide) {
            animalPanelSlideProgress = Math.min(1.0, animalPanelSlideProgress + 0.08);
        } else if (animalPanelSlideProgress > targetSlide) {
            animalPanelSlideProgress = Math.max(0.0, animalPanelSlideProgress - 0.08);
        }

        if (entidades != null) {
            for (Renderable r : entidades) {
                try {
                    r.update();
                    r.render(renderer, cam);
                } catch (Exception ex) {
                    // Skip problematic entity to avoid breaking render loop
                }
            }
        }

        AnimalSpawnerMenu menu = controles != null ? controles.getSpawnerMenu() : null;

        if (menu != null && menu.isWaitingForPosition()) {
            latestSpawnTarget = computeSpawnTarget(cam);
            drawSpawnIndicator(menu, cam);
        } else {
            latestSpawnTarget = null;
        }

        if (menu != null && menu.isOpen()) {
            drawSpawnerMenu(menu);
        } else {
            lastMenuBounds = Collections.emptyList();
        }

        // Draw pause menu if paused
        if (controles != null && controles.isPaused()) {
            drawPauseMenu();
        }
        
        // Draw animal info panel
        if (animalPanelSlideProgress > 0.01) {
            drawAnimalInfoPanel();
        }

        drawHUD(controles, menu, cam);

        renderer.swapBuffers();
    }

    public BufferedImage getRenderedImage() {
        return renderer.getBuffer();
    }

    private void drawHUD(Controles controles, AnimalSpawnerMenu menu, Camera cam) {
        PixelFont.drawText(renderer, 10, 10, "Y: Agregar animal", 2, Color.WHITE);

        if (controles != null && controles.isDebugOverlayEnabled() && cam != null) {
            Vector3 p = cam.getPosicion();
            String dbg = "Pos: " + (int) p.x + "," + (int) p.y + "," + (int) p.z;
            PixelFont.drawText(renderer, 10, 30, dbg, 2, Color.WHITE);
        }

        boolean showCrosshair = controles == null || controles.isCrosshairVisible();
        if (menu != null && menu.isOpen()) {
            showCrosshair = false;
        }
        if (showCrosshair) {
            drawCrosshair();
        }

        if (System.currentTimeMillis() < transientMessageUntil && !transientMessage.isEmpty()) {
            drawCenteredText(transientMessage, transientMessageColor, alto - 20, 2);
        }
    }

    private void drawCrosshair() {
        int cx = ancho / 2;
        int cy = alto / 2;
        int len = 8;
        renderer.drawLine2D(cx - len, cy, cx + len, cy, Color.BLACK);
        renderer.drawLine2D(cx, cy - len, cx, cy + len, Color.BLACK);
        renderer.drawLine2D(cx - len + 1, cy, cx + len - 1, cy, Color.WHITE);
        renderer.drawLine2D(cx, cy - len + 1, cx, cy + len - 1, Color.WHITE);
    }

    private void drawSpawnerMenu(AnimalSpawnerMenu menu) {
        String title = "Elige un animal";
        int optionCount = AnimalSpawnerMenu.ANIMAL_OPTIONS.length;
        int optionHeight = 18;
        int padding = 10;
        int panelWidth = 260;
        int panelHeight = padding * 2 + 30 + optionCount * optionHeight;
        int startX = (ancho - panelWidth) / 2;
        int startY = (alto - panelHeight) / 2;

        renderer.fillRect(startX, startY, panelWidth, panelHeight, new Color(25, 25, 25));
        PixelFont.drawText(renderer, startX + padding, startY + padding, title, 2, Color.WHITE);

        List<MenuItemBounds> bounds = new ArrayList<>();
        for (int i = 0; i < optionCount; i++) {
            int y = startY + padding + 20 + i * optionHeight;
            Color bg = (i == menu.getSelectedIndex()) ? new Color(60, 60, 120) : new Color(40, 40, 40);
            renderer.fillRect(startX + padding, y, panelWidth - padding * 2, optionHeight - 4, bg);
            PixelFont.drawText(renderer, startX + padding + 8, y + 4, AnimalSpawnerMenu.ANIMAL_OPTIONS[i], 2, Color.WHITE);
            bounds.add(new MenuItemBounds(i, startX + padding, y, panelWidth - padding * 2, optionHeight - 4));
        }
        lastMenuBounds = bounds;
    }

    private void drawSpawnIndicator(AnimalSpawnerMenu menu, Camera cam) {
        if (latestSpawnTarget == null || !latestSpawnTarget.hasHit) {
            drawCenteredText("Apunta al terreno visible", new Color(255, 80, 80), alto - 40, 2);
            return;
        }

        Color ring = latestSpawnTarget.valid ? new Color(50, 200, 50) : new Color(220, 60, 60);
        Color spokes = latestSpawnTarget.valid ? new Color(140, 255, 140) : new Color(255, 140, 140);

        // Draw ring projected in 3D over the ground plane at the hit position
        Vector3 center = latestSpawnTarget.position;
        double radiusWorld = 12.0;
        Vector3 right = cam.getRight();
        Vector3 forward = cam.getForward();
        Vector3 rxz = new Vector3(right.x, 0, right.z).normalize();
        Vector3 fxz = new Vector3(forward.x, 0, forward.z).normalize();
        if (Math.abs(rxz.x) < 1e-4 && Math.abs(rxz.z) < 1e-4) rxz = new Vector3(1, 0, 0);
        if (Math.abs(fxz.x) < 1e-4 && Math.abs(fxz.z) < 1e-4) fxz = new Vector3(0, 0, 1);

        int segments = 40;
        for (int i = 0; i < segments; i++) {
            double a0 = (2 * Math.PI * i) / segments;
            double a1 = (2 * Math.PI * (i + 1)) / segments;
            Vector3 p0 = center.add(rxz.scale(Math.cos(a0) * radiusWorld)).add(fxz.scale(Math.sin(a0) * radiusWorld));
            Vector3 p1 = center.add(rxz.scale(Math.cos(a1) * radiusWorld)).add(fxz.scale(Math.sin(a1) * radiusWorld));
            renderer.drawLine3D(p0, p1, cam, ring);
        }

        // Draw a few spokes to hint at fill/validity
        for (int i = 0; i < 6; i++) {
            double a = (2 * Math.PI * i) / 6;
            Vector3 p0 = center;
            Vector3 p1 = center.add(rxz.scale(Math.cos(a) * radiusWorld * 0.9)).add(fxz.scale(Math.sin(a) * radiusWorld * 0.9));
            renderer.drawLine3D(p0, p1, cam, spokes);
        }

        String msg = latestSpawnTarget.valid ? "Click derecho para colocar" : latestSpawnTarget.reason;
        Color txt = latestSpawnTarget.valid ? Color.WHITE : new Color(255, 80, 80);
        drawCenteredText(msg, txt, alto - 40, 2);
    }

    private void drawPauseMenu() {
        int cx = ancho / 2;
        int cy = alto / 2;
        int pad = 40;
        int pw = ancho - pad * 2;
        int ph = alto - pad * 2;
        
        // Dark background panel
        renderer.fillRect(pad, pad, pw, ph, new Color(10, 10, 15, 220));
        
        // Title
        String title = "PAUSA";
        int titleScale = 4;
        int titleW = PixelFont.measureTextWidth(title, titleScale);
        int titleX = cx - titleW / 2;
        int titleY = pad + 20;
        PixelFont.drawText(renderer, titleX, titleY, title, titleScale, Color.WHITE);
        
        // Buttons
        int btnW = 240;
        int btnH = 48;
        int btnX = cx - btnW / 2;
        int btnY = titleY + 80;
        
        List<ButtonBounds> buttons = new ArrayList<>();
        
        int contY = btnY;
        drawButton(btnX, contY, btnW, btnH, new Color(50, 50, 80), "CONTINUAR", 2);
        buttons.add(new ButtonBounds(btnX, contY, btnW, btnH, "continue"));
        
        int saveY = btnY + btnH + 12;
        drawButton(btnX, saveY, btnW, btnH, new Color(40, 80, 40), "GUARDAR", 2);
        buttons.add(new ButtonBounds(btnX, saveY, btnW, btnH, "save"));
        
        int loadY = btnY + (btnH + 12) * 2;
        drawButton(btnX, loadY, btnW, btnH, new Color(40, 40, 80), "CARGAR", 2);
        buttons.add(new ButtonBounds(btnX, loadY, btnW, btnH, "load"));
        
        int exitY = btnY + (btnH + 12) * 3;
        drawButton(btnX, exitY, btnW, btnH, new Color(80, 40, 40), "SALIR", 2);
        buttons.add(new ButtonBounds(btnX, exitY, btnW, btnH, "exit"));
        
        pauseMenuButtons = buttons;
    }

    private void drawButton(int x, int y, int w, int h, Color bg, String text, int textScale) {
        renderer.fillRect(x, y, w, h, bg);
        renderer.drawLine2D(x, y, x + w - 1, y, Color.WHITE);
        renderer.drawLine2D(x, y + h - 1, x + w - 1, y + h - 1, Color.WHITE);
        renderer.drawLine2D(x, y, x, y + h - 1, Color.WHITE);
        renderer.drawLine2D(x + w - 1, y, x + w - 1, y + h - 1, Color.WHITE);
        
        int tw = PixelFont.measureTextWidth(text, textScale);
        int tx = x + (w - tw) / 2;
        int ty = y + (h - 7 * textScale) / 2;
        PixelFont.drawText(renderer, tx, ty, text, textScale, Color.WHITE);
    }

    private void drawCenteredText(String text, Color color, int y, int scale) {
        int w = PixelFont.measureTextWidth(text, scale);
        int x = (ancho - w) / 2;
        PixelFont.drawText(renderer, x, y, text, scale, color);
    }

    private void drawFilledCircle(int cx, int cy, int r, Color color) {
        int rr = r * r;
        for (int dy = -r; dy <= r; dy++) {
            int yy = cy + dy;
            for (int dx = -r; dx <= r; dx++) {
                int xx = cx + dx;
                if (dx * dx + dy * dy <= rr) {
                    renderer.drawPixel(xx, yy, color);
                }
            }
        }
    }

    private void drawCircleOutline(int cx, int cy, int r, Color color) {
        int rr = r * r;
        for (int dy = -r; dy <= r; dy++) {
            int yy = cy + dy;
            for (int dx = -r; dx <= r; dx++) {
                int xx = cx + dx;
                int d2 = dx * dx + dy * dy;
                if (d2 <= rr && d2 >= rr - 3) {
                    renderer.drawPixel(xx, yy, color);
                }
            }
        }
    }

    private void setTransientMessage(String text, Color color, long durationMs) {
        this.transientMessage = text;
        this.transientMessageColor = color;
        this.transientMessageUntil = System.currentTimeMillis() + durationMs;
    }

    private SpawnTarget computeSpawnTarget(Camera cam) {
        SpawnTarget result = new SpawnTarget();
        result.hasHit = false;
        result.valid = false;
        result.reason = "Apunta al terreno visible";

        if (cam == null || mundo == null) {
            return result;
        }

        Vector3 origin = cam.getPosicion();
        Vector3 dir = cam.getForward().normalize();

        double maxT = 500.0;
        double step = 5.0;
        double prevDiff = origin.y - mundo.getHeightAt(origin.x, origin.z);

        for (double t = step; t <= maxT; t += step) {
            Vector3 p = origin.add(dir.scale(t));
            double h = mundo.getHeightAt(p.x, p.z);
            if (h == Double.NEGATIVE_INFINITY) {
                prevDiff = Double.POSITIVE_INFINITY;
                continue;
            }
            double diff = p.y - h;
            if (diff <= 0.0) {
                double alpha = prevDiff / (prevDiff - diff);
                if (Double.isNaN(alpha) || Double.isInfinite(alpha)) {
                    alpha = 0.0;
                }
                double hitT = t - step + step * alpha;
                Vector3 hit = origin.add(dir.scale(hitT));
                // Añadir offset vertical para compensar patas de animales que tienen Y negativa
                // El offset de 5.0 es suficiente para la mayoría de los animales (voxelSize * 1.5 aprox)
                result.position = new Vector3(hit.x, h + 5.0, hit.z);
                result.hasHit = true;
                break;
            }
            prevDiff = diff;
        }

        if (!result.hasHit) {
            return result;
        }

        double dx = result.position.x - origin.x;
        double dy = result.position.y - origin.y;
        double dz = result.position.z - origin.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double minD = 20.0;
        double maxD = 420.0;
        if (dist < minD) {
            result.reason = "Demasiado cerca";
            return result;
        }
        if (dist > maxD) {
            result.reason = "Fuera de rango";
            return result;
        }

        double spawnRadius = 12.0;
        for (Collidable c : mundo.getCollidables()) {
            // Skip grass (Pasto) - animals can spawn on top of grass
            if (c instanceof entities.Pasto) {
                continue;
            }
            
            Vector3 min = c.getAABBMin();
            Vector3 max = c.getAABBMax();
            double cx = Math.max(min.x, Math.min(result.position.x, max.x));
            double cy = Math.max(min.y, Math.min(result.position.y, max.y));
            double cz = Math.max(min.z, Math.min(result.position.z, max.z));
            double ddx = result.position.x - cx;
            double ddy = result.position.y - cy;
            double ddz = result.position.z - cz;
            if (ddx * ddx + ddy * ddy + ddz * ddz < spawnRadius * spawnRadius) {
                result.reason = "Colision con estructura";
                return result;
            }
        }

        for (entities.BaseAnimal a : mundo.getAnimals()) {
            Vector3 ap = a.getPosicion();
            double d2 = (ap.x - result.position.x) * (ap.x - result.position.x)
                + (ap.z - result.position.z) * (ap.z - result.position.z);
            if (d2 < spawnRadius * spawnRadius) {
                result.reason = "Muy cerca de otro animal";
                return result;
            }
        }

        double[] proj = renderer.project(result.position, cam);
        if (proj == null) {
            result.reason = "Fuera de vista";
            return result;
        }
        result.screenX = proj[0];
        result.screenY = proj[1];

        Vector3 right = cam.getRight().normalize();
        double radiusWorld = 8.0;
        double[] proj2 = renderer.project(result.position.add(right.scale(radiusWorld)), cam);
        int pr = 12;
        if (proj2 != null) {
            pr = (int) Math.max(6, Math.min(40, Math.abs(proj2[0] - proj[0])));
        }
        result.pixelRadius = pr;

        result.valid = true;
        result.reason = "";
        return result;
    }

    public void handleKeyPressed(KeyEvent e, Controles controles) {
        if (controles == null) return;
        
        // If animal panel is open and ESC is pressed, close it
        if (animalPanelActive && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (selectedAnimal != null) {
                setAnimalSelected(selectedAnimal, false);
            }
            selectedAnimal = null;
            animalPanelActive = false;
            controles.setAnimalPanelOpen(false);
            e.consume();
        }
    }

    public void handleMousePressed(MouseEvent e, Controles controles) {
        if (controles == null) return;
        
        int mx = e.getX();
        int my = e.getY();
        
        // Detectar botones del mouse sin SwingUtilities
        boolean isLeftButton = (e.getButton() == MouseEvent.BUTTON1);
        boolean isRightButton = (e.getButton() == MouseEvent.BUTTON3);
        
        // Handle animal info panel clicks first
        if (selectedAnimal != null && selectedAnimal instanceof entities.BaseAnimal) {
            entities.BaseAnimal ba = (entities.BaseAnimal) selectedAnimal;
            
            if (isLeftButton) {
                // Check for delete button
                if (deleteAnimalButton != null && deleteAnimalButton.contains(mx, my)) {
                    if (mundo != null) {
                        mundo.removeEntity(selectedAnimal);
                    }
                    setAnimalSelected(selectedAnimal, false);
                    selectedAnimal = null;
                    animalPanelActive = false;
                    controles.setAnimalPanelOpen(false);
                    setTransientMessage("Animal eliminado", new Color(255, 150, 50), 2000);
                    return;
                }
                
                // Check for evolution buttons
                int phase = ba.getGrowthPhase();
                
                // Button dimensions for evolution buttons
                int panelWidth = 320;
                int panelHeight = 420;
                int panelX = ancho - (int)(panelWidth * animalPanelSlideProgress);
                int panelY = alto / 2 - panelHeight / 2;
                
                int lineY = panelY + 130;
                lineY += 18; // Velocidad
                lineY += 18; // Tiempo en mapa
                
                // Conditional height based on phase (same logic as drawAnimalInfoPanel)
                if (phase < 3) {
                    lineY += 18; // Progreso
                    lineY += 18; // Time to next
                    lineY += 20; // Progress bar
                } else {
                    lineY += 18; // "Evolucion completa" message
                }
                lineY += 10;
                
                int buttonHeight = 28;
                int buttonWidth = 85;
                int buttonSpacing = 6;
                
                int btn1X = panelX + 15;
                int btn1Y = lineY;
                int btn2X = btn1X + buttonWidth + buttonSpacing;
                int btn2Y = lineY;
                int btn3X = btn2X + buttonWidth + buttonSpacing;
                int btn3Y = lineY;
                
                // Check Anterior button (phase > 1)
                if (phase > 1 && mx >= btn1X && mx < btn1X + buttonWidth && my >= btn1Y && my < btn1Y + buttonHeight) {
                    ba.revertToPreviousPhase();
                    setTransientMessage("Retrocedido a etapa " + ba.getGrowthPhase(), new Color(150, 200, 255), 2000);
                    return;
                }
                
                // Check Siguiente button (phase < 3)
                if (phase < 3 && mx >= btn2X && mx < btn2X + buttonWidth && my >= btn2Y && my < btn2Y + buttonHeight) {
                    ba.advanceToNextPhase();
                    setTransientMessage("Avanzado a etapa " + ba.getGrowthPhase(), new Color(100, 255, 150), 2000);
                    return;
                }
                
                // Check Eliminar button
                if (mx >= btn3X && mx < btn3X + buttonWidth && my >= btn3Y && my < btn3Y + buttonHeight) {
                    // Start death animation
                    ba.markForDeath();
                    setAnimalSelected(selectedAnimal, false);
                    selectedAnimal = null;
                    animalPanelActive = false;
                    controles.setAnimalPanelOpen(false);
                    setTransientMessage("Animal eliminado", new Color(255, 150, 50), 2000);
                    return;
                }
            }
        } else if (selectedAnimal != null && selectedAnimal instanceof entities.Depredador) {
            // Handle depredador panel clicks
            if (isLeftButton) {
                entities.Depredador dep = (entities.Depredador) selectedAnimal;
                
                // Button dimensions for depredador - DEBE SER IDÉNTICO A drawDepredadorInfoPanel()
                int panelWidth = 320;
                int panelHeight = 280;
                int panelX = ancho - (int)(panelWidth * animalPanelSlideProgress);
                int panelY = alto / 2 - panelHeight / 2;
                
                // Calcular lineY exactamente como en drawDepredadorInfoPanel
                int lineY = panelY + 110;
                lineY += 18; // Velocidad
                lineY += 18; // Tiempo en mapa
                lineY += 18; // Característica 1
                lineY += 18; // Característica 2
                lineY += 30; // Spacing ANTES del botón
                
                // Ahora lineY apunta exactamente donde está el botón
                int buttonHeight = 28;
                int buttonWidth = 130;
                int btnX = panelX + (panelWidth - buttonWidth) / 2;
                int btnY = lineY;
                
                // El texto se dibuja en y + 7, así que el área visual está 7 píxeles más abajo
                // Por eso necesitamos expandir más para capturar donde el usuario ve las letras
                int clickPaddingTop = 15;    // Expansión hacia arriba (más generosa)
                int clickPaddingBottom = 5;  // Expansión hacia abajo
                
                // Check Eliminar button - ÁREA MUY EXPANDIDA
                boolean buttonHit = (mx >= btnX && mx < btnX + buttonWidth && 
                                    my >= btnY - clickPaddingTop && my < btnY + buttonHeight + clickPaddingBottom);
                
                if (buttonHit) {
                    // Marcar para muerte (permitir animación)
                    dep.markForDeath();
                    
                    // Desseleccionar y limpiar estado
                    setAnimalSelected(selectedAnimal, false);
                    selectedAnimal = null;
                    animalPanelActive = false;
                    animalPanelSlideProgress = 0.0;
                    controles.setAnimalPanelOpen(false);
                    
                    setTransientMessage("Depredador eliminado", new Color(255, 100, 50), 2000);
                    return;
                }
            }
        }
        
        // Handle pause menu clicks
        if (controles.isPaused()) {
            if (isLeftButton) {
                for (ButtonBounds btn : pauseMenuButtons) {
                    if (btn.contains(mx, my)) {
                        handlePauseMenuAction(btn.action, controles);
                        return;
                    }
                }
            }
            return;
        }
        
        AnimalSpawnerMenu menu = controles.getSpawnerMenu();
        if (menu == null) return;

        if (menu.isOpen()) {
            if (isLeftButton) {
                for (MenuItemBounds b : lastMenuBounds) {
                    if (b.contains(mx, my)) {
                        int current = menu.getSelectedIndex();
                        while (current < b.index) { menu.navigateDown(); current++; }
                        while (current > b.index) { menu.navigateUp(); current--; }
                        int type = menu.selectCurrentOption();
                        if (mundo != null) {
                            mundo.setSelectedAnimalType(type);
                            mundo.setWaitingForSpawn(true);
                        }
                        controles.lockMouse(true);
                        break;
                    }
                }
            }
            return;
        }

        if (menu.isWaitingForPosition()) {
            if (isRightButton) {
                attemptSpawn(controles);
            }
            return;
        }
        
        // Handle animal selection with right click
        if (isRightButton) {
            if (animalPanelActive) {
                // If panel is active, this closes it
                if (selectedAnimal != null) {
                    setAnimalSelected(selectedAnimal, false);
                }
                selectedAnimal = null;
                animalPanelActive = false;
                controles.setAnimalPanelOpen(false);
                return;
            }
            
            if (hoveredAnimal != null) {
                // Deselect previous
                if (selectedAnimal != null) {
                    setAnimalSelected(selectedAnimal, false);
                }
                // Select new
                selectedAnimal = hoveredAnimal;
                setAnimalSelected(selectedAnimal, true);
                animalPanelActive = true;
                controles.setAnimalPanelOpen(true);
                setTransientMessage("Animal #" + getAnimalId(selectedAnimal) + " seleccionado", new Color(100, 200, 255), 2000);
                return;
            } else {
                // Clicked empty space - deselect
                if (selectedAnimal != null) {
                    setAnimalSelected(selectedAnimal, false);
                    selectedAnimal = null;
                    animalPanelActive = false;
                    controles.setAnimalPanelOpen(false);
                }
            }
        }
        
        // Solo bloquear el cursor si no hay panel activo
        if (!animalPanelActive) {
            controles.lockMouse(true);
        }
    }
    
    // Helper methods for animal selection
    private void setAnimalSelected(Renderable animal, boolean selected) {
        if (animal instanceof entities.BaseAnimal) {
            ((entities.BaseAnimal) animal).setSelected(selected);
        } else if (animal instanceof entities.Depredador) {
            ((entities.Depredador) animal).setSelected(selected);
            ((entities.Depredador) animal).setPaused(selected);
        }
    }
    
    private void setAnimalHovered(Renderable animal, boolean hovered) {
        if (animal instanceof entities.BaseAnimal) {
            ((entities.BaseAnimal) animal).setHovered(hovered);
        } else if (animal instanceof entities.Depredador) {
            ((entities.Depredador) animal).setHovered(hovered);
        }
    }
    
    private int getAnimalId(Renderable animal) {
        if (animal instanceof entities.BaseAnimal) {
            return ((entities.BaseAnimal) animal).getAnimalId();
        } else if (animal instanceof entities.Depredador) {
            return ((entities.Depredador) animal).getDepredadorId();
        }
        return -1;
    }

    private void handlePauseMenuAction(String action, Controles controles) {
        switch (action) {
            case "continue":
                controles.setPaused(false);
                break;
            case "save":
                handleSaveGame(controles);
                break;
            case "load":
                handleLoadGame(controles);
                break;
            case "exit":
                System.exit(0);
                break;
        }
    }
    
    private void handleSaveGame(Controles controles) {
        if (mundo == null || camera == null) {
            setTransientMessage("Error: Falta informacion", new Color(255, 100, 50), 2000);
            return;
        }
        
        // Mostrar diálogo de guardado
        java.io.File saveFile = SaveGameDialog.showSaveDialog(null);
        if (saveFile == null) {
            return; // Usuario canceló
        }
        
        // Crear estado del juego
        simulation.Persistencia.GameState state = new simulation.Persistencia.GameState();
        state.animals = mundo.getAnimals();
        state.cameraPos = camera.getPosicion();
        state.cameraYaw = camera.getYaw();
        state.cameraPitch = camera.getPitch();
        state.simulatorSeed = simulador != null ? simulador.getSeed() : 5555L;
        state.environmentSeed = mundo.getEnvironmentSeed();
        state.environmentCreatedAt = mundo.getEnvironmentCreatedAt();
        
        // Guardar
        if (simulation.Persistencia.saveGameState(saveFile, state)) {
            setTransientMessage("Partida guardada: " + saveFile.getName(), new Color(100, 200, 150), 3000);
        } else {
            setTransientMessage("Error al guardar partida", new Color(255, 100, 50), 2000);
        }
    }
    
    private void handleLoadGame(Controles controles) {
        if (mundo == null || camera == null) {
            setTransientMessage("Error: Falta informacion", new Color(255, 100, 50), 2000);
            return;
        }
        
        // Mostrar diálogo de carga
        java.io.File loadFile = LoadGameDialog.showLoadDialog(null);
        if (loadFile == null) {
            return; // Usuario canceló
        }
        
        // Cargar estado
        simulation.Persistencia.GameState state = simulation.Persistencia.loadGameState(loadFile);
        
        // Limpiar mundo actual
        mundo.clearWorld();
        
        // Restaurar seed y tiempo de creación del entorno para generación determinística y progreso de crecimiento
        mundo.setEnvironmentSeed(state.environmentSeed);
        // Ajustar el tiempo de creación para mantener el mismo progreso: restar el tiempo transcurrido desde la última sesión
        long timeSinceCreation = state.environmentCreatedAt - System.currentTimeMillis();
        long adjustedCreationTime = System.currentTimeMillis() + timeSinceCreation;
        mundo.setEnvironmentCreatedAt(adjustedCreationTime);
        
        // Recrear terreno y entidades ambientales con tiempos ajustados
        mundo.initializeEnvironment();
        
        // Restaurar posición de cámara
        camera.setPosicion(state.cameraPos);
        camera.setOrientation(state.cameraYaw, state.cameraPitch);
        
        // Restaurar animales con ajuste de altura
        for (entities.BaseAnimal a : state.animals) {
            // Ajustar posición Y según el terreno actual
            Vector3 pos = a.getPosicion();
            double terrainH = mundo.getHeightAt(pos.x, pos.z);
            if (terrainH != Double.NEGATIVE_INFINITY) {
                // Colocar animal sobre el terreno
                a.setPosicion(new Vector3(pos.x, terrainH + 5.0, pos.z));
            }
            mundo.addAnimal(a);
        }
        
        // Reanudar simulación
        controles.setPaused(false);
        
        setTransientMessage("Partida cargada: " + loadFile.getName(), new Color(100, 200, 150), 3000);
    }

    private void attemptSpawn(Controles controles) {
        if (mundo == null) return;
        if (controles == null) return;
        AnimalSpawnerMenu menu = controles.getSpawnerMenu();
        
        // Prevenir spawns duplicados
        if (!mundo.isWaitingForSpawn()) return;

        if (latestSpawnTarget == null) {
            latestSpawnTarget = computeSpawnTarget(controles.getCamera());
        }

        if (latestSpawnTarget == null || !latestSpawnTarget.hasHit) {
            setTransientMessage("Apunta al terreno visible", new Color(255, 80, 80), 2000);
            return;
        }
        if (!latestSpawnTarget.valid) {
            setTransientMessage(latestSpawnTarget.reason, new Color(255, 80, 80), 2000);
            return;
        }

        int type = mundo.getSelectedAnimalType();
        if (type < 0) {
            type = rng.nextInt(10);
        }
        Vector3 pos = latestSpawnTarget.position;
        long seed = System.currentTimeMillis();
        Renderable r = EcosistemaApp.createAnimalOfType(type, pos, seed);
        if (r != null) {
            if (r instanceof entities.BaseAnimal) {
                mundo.addAnimal((entities.BaseAnimal) r);
            } else {
                mundo.addEntity(r);
            }
        }
        mundo.setWaitingForSpawn(false);
        if (menu != null) {
            menu.confirmSpawn();
        }
        setTransientMessage("Animal colocado", new Color(120, 255, 120), 2000);
    }
    
    private void updateHoveredAnimal(List<Renderable> entidades, Camera cam, Controles controles) {
        if (entidades == null || cam == null || controles == null) return;
        if (controles.isPaused()) return;
        
        // Clear previous hover state
        if (hoveredAnimal != null) {
            setAnimalHovered(hoveredAnimal, false);
        }
        hoveredAnimal = null;
        
        // Don't detect hover if spawn menu is open
        AnimalSpawnerMenu menu = controles.getSpawnerMenu();
        if (menu != null && (menu.isOpen() || menu.isWaitingForPosition())) {
            return;
        }
        
        // Ray from camera through center of screen
        Vector3 rayOrigin = cam.getPosicion();
        Vector3 rayDir = cam.getForward();
        
        double closestDist = Double.POSITIVE_INFINITY;
        Renderable closest = null;
        
        for (Renderable r : entidades) {
            if (r instanceof entities.BaseAnimal) {
                entities.BaseAnimal animal = (entities.BaseAnimal) r;
                
                // Simple AABB ray intersection
                Vector3 aabbMin = animal.getAABBMin();
                Vector3 aabbMax = animal.getAABBMax();
                
                double tMin = (aabbMin.x - rayOrigin.x) / (rayDir.x + 0.0001);
                double tMax = (aabbMax.x - rayOrigin.x) / (rayDir.x + 0.0001);
                if (tMin > tMax) { double t = tMin; tMin = tMax; tMax = t; }
                
                double tyMin = (aabbMin.y - rayOrigin.y) / (rayDir.y + 0.0001);
                double tyMax = (aabbMax.y - rayOrigin.y) / (rayDir.y + 0.0001);
                if (tyMin > tyMax) { double t = tyMin; tyMin = tyMax; tyMax = t; }
                
                if (tMin > tyMax || tyMin > tMax) continue;
                if (tyMin > tMin) tMin = tyMin;
                if (tyMax < tMax) tMax = tyMax;
                
                double tzMin = (aabbMin.z - rayOrigin.z) / (rayDir.z + 0.0001);
                double tzMax = (aabbMax.z - rayOrigin.z) / (rayDir.z + 0.0001);
                if (tzMin > tzMax) { double t = tzMin; tzMin = tzMax; tzMax = t; }
                
                if (tMin > tzMax || tzMin > tMax) continue;
                if (tzMin > tMin) tMin = tzMin;
                
                if (tMin > 0 && tMin < closestDist && tMin < 500) { // Max distance 500
                    closestDist = tMin;
                    closest = animal;
                }
            } else if (r instanceof entities.Depredador) {
                entities.Depredador dep = (entities.Depredador) r;
                
                // Simple sphere collision for depredador
                Vector3 depPos = dep.getPosition();
                Vector3 toTarget = depPos.subtract(rayOrigin);
                double projection = toTarget.dot(rayDir);
                
                if (projection > 0) {
                    Vector3 closestPoint = rayOrigin.add(rayDir.scale(projection));
                    double distance = closestPoint.subtract(depPos).length();
                    double radius = dep.getCollisionRadius();
                    
                    if (distance < radius && projection < closestDist && projection < 500) {
                        closestDist = projection;
                        closest = dep;
                    }
                }
            }
        }
        
        if (closest != null) {
            hoveredAnimal = closest;
            if (closest instanceof entities.BaseAnimal) {
                ((entities.BaseAnimal)closest).setHovered(true);
            } else if (closest instanceof entities.Depredador) {
                ((entities.Depredador)closest).setHovered(true);
            }
        }
    }
    
    private void drawAnimalInfoPanel() {
        if (selectedAnimal == null || !animalPanelActive) return;
        
        // Check if it's a Depredador
        if (selectedAnimal instanceof entities.Depredador) {
            drawDepredadorInfoPanel();
            return;
        }
        
        if (!(selectedAnimal instanceof entities.BaseAnimal)) return;
        
        entities.BaseAnimal ba = (entities.BaseAnimal) selectedAnimal;
        BufferedImage buffer = renderer.getBuffer();
        if (buffer == null) return;
        
        // Panel dimensions and position - made taller to accommodate more content
        int panelWidth = 320;
        int panelHeight = 420;
        int panelX = ancho - (int)(panelWidth * animalPanelSlideProgress);
        int panelY = alto / 2 - panelHeight / 2;
        
        // Draw gradient background with borders
        Color bgColor1 = new Color(20, 25, 40); // Darker top
        Color bgColor2 = new Color(35, 45, 65); // Lighter bottom
        
        for (int y = panelY; y < panelY + panelHeight; y++) {
            for (int x = panelX; x < panelX + panelWidth; x++) {
                if (x >= 0 && x < ancho && y >= 0 && y < alto) {
                    try {
                        // Gradient effect
                        float gradientFactor = (float)(y - panelY) / panelHeight;
                        int r = (int)(bgColor1.getRed() * (1 - gradientFactor) + bgColor2.getRed() * gradientFactor);
                        int g = (int)(bgColor1.getGreen() * (1 - gradientFactor) + bgColor2.getGreen() * gradientFactor);
                        int b = (int)(bgColor1.getBlue() * (1 - gradientFactor) + bgColor2.getBlue() * gradientFactor);
                        Color panelColor = new Color(r, g, b, 230);
                        buffer.setRGB(x, y, panelColor.getRGB());
                    } catch (Exception e) {}
                }
            }
        }
        
        // Draw outer border (bright cyan glow)
        Color outerBorder = new Color(100, 200, 255);
        for (int x = panelX; x < panelX + panelWidth; x++) {
            if (x >= 0 && x < ancho) {
                try {
                    for (int i = 0; i < 3; i++) {
                        if (panelY - i >= 0) buffer.setRGB(x, panelY - i, outerBorder.getRGB());
                        if (panelY + panelHeight + i < alto) buffer.setRGB(x, panelY + panelHeight + i, outerBorder.getRGB());
                    }
                } catch (Exception e) {}
            }
        }
        for (int y = panelY; y < panelY + panelHeight; y++) {
            if (y >= 0 && y < alto) {
                try {
                    for (int i = 0; i < 3; i++) {
                        if (panelX - i >= 0) buffer.setRGB(panelX - i, y, outerBorder.getRGB());
                        if (panelX + panelWidth + i < ancho) buffer.setRGB(panelX + panelWidth + i, y, outerBorder.getRGB());
                    }
                } catch (Exception e) {}
            }
        }
        
        // Draw inner border
        Color innerBorder = new Color(150, 220, 255);
        for (int x = panelX; x < panelX + panelWidth; x++) {
            if (x >= 0 && x < ancho) {
                try {
                    buffer.setRGB(x, panelY, innerBorder.getRGB());
                    buffer.setRGB(x, panelY + panelHeight - 1, innerBorder.getRGB());
                } catch (Exception e) {}
            }
        }
        for (int y = panelY; y < panelY + panelHeight; y++) {
            if (y >= 0 && y < alto) {
                try {
                    buffer.setRGB(panelX, y, innerBorder.getRGB());
                    buffer.setRGB(panelX + panelWidth - 1, y, innerBorder.getRGB());
                } catch (Exception e) {}
            }
        }
        
        // Draw title bar with gradient
        Color titleBg1 = new Color(50, 100, 150);
        Color titleBg2 = new Color(70, 130, 180);
        for (int y = panelY + 2; y < panelY + 35; y++) {
            for (int x = panelX + 2; x < panelX + panelWidth - 2; x++) {
                if (x >= 0 && x < ancho && y >= 0 && y < alto) {
                    try {
                        float grad = (float)(y - panelY - 2) / 33;
                        int r = (int)(titleBg1.getRed() * (1 - grad) + titleBg2.getRed() * grad);
                        int g = (int)(titleBg1.getGreen() * (1 - grad) + titleBg2.getGreen() * grad);
                        int b = (int)(titleBg1.getBlue() * (1 - grad) + titleBg2.getBlue() * grad);
                        buffer.setRGB(x, y, new Color(r, g, b).getRGB());
                    } catch (Exception e) {}
                }
            }
        }
        
        // Draw title
        PixelFont.drawText(renderer, panelX + 15, panelY + 10, "ANIMAL", 2, new Color(200, 230, 255));
        
        // Draw animal ID with larger font
        String animalInfo = "ID: #" + ba.getAnimalId();
        PixelFont.drawText(renderer, panelX + 15, panelY + 45, animalInfo, 3, new Color(255, 255, 150));
        
        // Draw species name
        String species = ba.getSpeciesName();
        PixelFont.drawText(renderer, panelX + 15, panelY + 75, species, 2, ba.getOriginalColor());
        
        // Draw growth phase
        int phase = ba.getGrowthPhase();
        String phaseName = "";
        Color phaseColor = Color.WHITE;
        switch(phase) {
            case 1: phaseName = "ETAPA 1: Cria"; phaseColor = new Color(150, 200, 255); break;
            case 2: phaseName = "ETAPA 2: Joven"; phaseColor = new Color(100, 255, 150); break;
            case 3: phaseName = "ETAPA 3: Adulto"; phaseColor = new Color(255, 215, 0); break;
        }
        PixelFont.drawText(renderer, panelX + 15, panelY + 100, phaseName, 2, phaseColor);
        
        // Draw additional stats
        int lineY = panelY + 130;
        
        // Velocidad (Speed)
        double speed = ba.getBaseSpeed();
        double speedMultiplier = phase == 1 ? 1.0 : (phase == 2 ? 1.25 : 1.5);
        double actualSpeed = speed * speedMultiplier;
        String speedText = String.format("Velocidad: %.2f", actualSpeed);
        PixelFont.drawText(renderer, panelX + 15, lineY, speedText, 1, new Color(180, 180, 255));
        lineY += 18;
        
        // Tiempo en mapa (Time in map)
        double timeInMap = ba.getTimeSinceSpawn();
        String timeText = String.format("T. en mapa: %.1fs", timeInMap);
        PixelFont.drawText(renderer, panelX + 15, lineY, timeText, 1, new Color(180, 200, 255));
        lineY += 18;
        
        // Tiempo para próxima etapa (Time to next stage)
        if (phase < 3) {
            double timeToNext = ba.getTimeToNextPhase();
            double progress = ba.getPhaseTimer();
            double maxTime = ba.getPhaseDurationPublic();
            int progressPercent = (int)((progress / maxTime) * 100);
            
            String progressText = String.format("Progreso: %d%%", progressPercent);
            PixelFont.drawText(renderer, panelX + 15, lineY, progressText, 1, new Color(180, 180, 255));
            lineY += 18;
            
            String timeNextText = String.format("Proxima etapa: %.1fs", timeToNext);
            PixelFont.drawText(renderer, panelX + 15, lineY, timeNextText, 1, new Color(150, 200, 255));
            lineY += 18;
            
            // Progress bar
            int barWidth = 280;
            int barHeight = 10;
            int barX = panelX + 15;
            int barY = lineY;
            
            // Background
            for (int y = barY; y < barY + barHeight; y++) {
                for (int x = barX; x < barX + barWidth; x++) {
                    if (x >= 0 && x < ancho && y >= 0 && y < alto) {
                        try {
                            buffer.setRGB(x, y, new Color(40, 40, 60).getRGB());
                        } catch (Exception e) {}
                    }
                }
            }
            
            // Progress fill
            int fillWidth = (int)(barWidth * (progress / maxTime));
            for (int y = barY; y < barY + barHeight; y++) {
                for (int x = barX; x < barX + fillWidth; x++) {
                    if (x >= 0 && x < ancho && y >= 0 && y < alto) {
                        try {
                            buffer.setRGB(x, y, new Color(100, 200, 255).getRGB());
                        } catch (Exception e) {}
                    }
                }
            }
            lineY += 20;
        } else {
            PixelFont.drawText(renderer, panelX + 15, lineY, "Evolucion completa", 1, new Color(255, 215, 0));
            lineY += 18;
        }
        
        // Draw evolution control buttons
        lineY += 10;
        int buttonHeight = 28;
        int buttonWidth = 85;
        int buttonSpacing = 6;
        
        // Button 1: Etapa Anterior (only if phase > 1)
        int btn1X = panelX + 15;
        int btn1Y = lineY;
        boolean btn1Enabled = phase > 1;
        drawEvolutionButton(buffer, btn1X, btn1Y, buttonWidth, buttonHeight, "ANTERIOR", btn1Enabled, panelX, panelY, panelWidth);
        if (btn1Enabled) {
            // Store button bounds for click detection
        }
        
        // Button 2: Siguiente Etapa (only if phase < 3)
        int btn2X = btn1X + buttonWidth + buttonSpacing;
        int btn2Y = lineY;
        boolean btn2Enabled = phase < 3;
        drawEvolutionButton(buffer, btn2X, btn2Y, buttonWidth, buttonHeight, "SIGUIENTE", btn2Enabled, panelX, panelY, panelWidth);
        
        // Button 3: Eliminar (always enabled)
        int btn3X = btn2X + buttonWidth + buttonSpacing;
        int btn3Y = lineY;
        drawEvolutionButton(buffer, btn3X, btn3Y, buttonWidth, buttonHeight, "ELIMINAR", true, panelX, panelY, panelWidth);
        
        // Store button bounds for click detection
        deleteAnimalButton = new ButtonBounds(btn3X, btn3Y, buttonWidth, buttonHeight, "delete_animal");
        
        // Draw hint
        PixelFont.drawText(renderer, panelX + 15, panelY + panelHeight - 15, "ESC para cerrar", 1, new Color(150, 150, 150));
    }
    
    private void drawEvolutionButton(BufferedImage buffer, int x, int y, int w, int h, String text, boolean enabled, int panelX, int panelY, int panelW) {
        Color btnBg1, btnBg2, btnBorder, txtColor;
        
        if (enabled) {
            btnBg1 = new Color(80, 120, 200);
            btnBg2 = new Color(100, 150, 220);
            btnBorder = new Color(150, 180, 255);
            txtColor = Color.WHITE;
        } else {
            btnBg1 = new Color(60, 60, 80);
            btnBg2 = new Color(80, 80, 100);
            btnBorder = new Color(100, 100, 120);
            txtColor = new Color(150, 150, 150);
        }
        
        // Button background with gradient
        for (int yy = y; yy < y + h; yy++) {
            for (int xx = x; xx < x + w; xx++) {
                if (xx >= 0 && xx < ancho && yy >= 0 && yy < alto) {
                    try {
                        float grad = (float)(yy - y) / h;
                        int r = (int)(btnBg1.getRed() * (1 - grad) + btnBg2.getRed() * grad);
                        int g = (int)(btnBg1.getGreen() * (1 - grad) + btnBg2.getGreen() * grad);
                        int b = (int)(btnBg1.getBlue() * (1 - grad) + btnBg2.getBlue() * grad);
                        buffer.setRGB(xx, yy, new Color(r, g, b).getRGB());
                    } catch (Exception e) {}
                }
            }
        }
        
        // Button border
        for (int xx = x; xx < x + w; xx++) {
            if (xx >= 0 && xx < ancho) {
                try {
                    if (y >= 0) buffer.setRGB(xx, y, btnBorder.getRGB());
                    if (y + h - 1 >= 0 && y + h - 1 < alto) buffer.setRGB(xx, y + h - 1, btnBorder.getRGB());
                } catch (Exception e) {}
            }
        }
        for (int yy = y; yy < y + h; yy++) {
            if (yy >= 0 && yy < alto) {
                try {
                    if (x >= 0) buffer.setRGB(x, yy, btnBorder.getRGB());
                    if (x + w - 1 >= 0 && x + w - 1 < ancho) buffer.setRGB(x + w - 1, yy, btnBorder.getRGB());
                } catch (Exception e) {}
            }
        }
        
        // Button text (small font)
        PixelFont.drawText(renderer, x + 5, y + 7, text, 1, txtColor);
    }
    
    private void drawDepredadorInfoPanel() {
        if (selectedAnimal == null || !animalPanelActive) return;
        if (!(selectedAnimal instanceof entities.Depredador)) return;
        
        entities.Depredador dep = (entities.Depredador) selectedAnimal;
        BufferedImage buffer = renderer.getBuffer();
        if (buffer == null) return;
        
        // Panel dimensions and position - smaller since no evolution info
        int panelWidth = 320;
        int panelHeight = 280;
        int panelX = ancho - (int)(panelWidth * animalPanelSlideProgress);
        int panelY = alto / 2 - panelHeight / 2;
        
        // Draw gradient background with borders (same as animal panel)
        Color bgColor1 = new Color(40, 15, 15); // Darker red tint
        Color bgColor2 = new Color(60, 25, 25); // Lighter red tint
        
        for (int y = panelY; y < panelY + panelHeight; y++) {
            for (int x = panelX; x < panelX + panelWidth; x++) {
                if (x >= 0 && x < ancho && y >= 0 && y < alto) {
                    try {
                        float gradientFactor = (float)(y - panelY) / panelHeight;
                        int r = (int)(bgColor1.getRed() * (1 - gradientFactor) + bgColor2.getRed() * gradientFactor);
                        int g = (int)(bgColor1.getGreen() * (1 - gradientFactor) + bgColor2.getGreen() * gradientFactor);
                        int b = (int)(bgColor1.getBlue() * (1 - gradientFactor) + bgColor2.getBlue() * gradientFactor);
                        Color panelColor = new Color(r, g, b, 230);
                        buffer.setRGB(x, y, panelColor.getRGB());
                    } catch (Exception e) {}
                }
            }
        }
        
        // Draw outer border (red glow)
        Color outerBorder = new Color(255, 100, 100);
        for (int x = panelX; x < panelX + panelWidth; x++) {
            if (x >= 0 && x < ancho) {
                try {
                    for (int i = 0; i < 3; i++) {
                        if (panelY - i >= 0) buffer.setRGB(x, panelY - i, outerBorder.getRGB());
                        if (panelY + panelHeight + i < alto) buffer.setRGB(x, panelY + panelHeight + i, outerBorder.getRGB());
                    }
                } catch (Exception e) {}
            }
        }
        for (int y = panelY; y < panelY + panelHeight; y++) {
            if (y >= 0 && y < alto) {
                try {
                    for (int i = 0; i < 3; i++) {
                        if (panelX - i >= 0) buffer.setRGB(panelX - i, y, outerBorder.getRGB());
                        if (panelX + panelWidth + i < ancho) buffer.setRGB(panelX + panelWidth + i, y, outerBorder.getRGB());
                    }
                } catch (Exception e) {}
            }
        }
        
        // Draw inner border
        Color innerBorder = new Color(255, 150, 150);
        for (int x = panelX; x < panelX + panelWidth; x++) {
            if (x >= 0 && x < ancho) {
                try {
                    buffer.setRGB(x, panelY, innerBorder.getRGB());
                    buffer.setRGB(x, panelY + panelHeight - 1, innerBorder.getRGB());
                } catch (Exception e) {}
            }
        }
        for (int y = panelY; y < panelY + panelHeight; y++) {
            if (y >= 0 && y < alto) {
                try {
                    buffer.setRGB(panelX, y, innerBorder.getRGB());
                    buffer.setRGB(panelX + panelWidth - 1, y, innerBorder.getRGB());
                } catch (Exception e) {}
            }
        }
        
        // Draw title bar with gradient (red theme)
        Color titleBg1 = new Color(150, 50, 50);
        Color titleBg2 = new Color(180, 70, 70);
        for (int y = panelY + 2; y < panelY + 35; y++) {
            for (int x = panelX + 2; x < panelX + panelWidth - 2; x++) {
                if (x >= 0 && x < ancho && y >= 0 && y < alto) {
                    try {
                        float grad = (float)(y - panelY - 2) / 33;
                        int r = (int)(titleBg1.getRed() * (1 - grad) + titleBg2.getRed() * grad);
                        int g = (int)(titleBg1.getGreen() * (1 - grad) + titleBg2.getGreen() * grad);
                        int b = (int)(titleBg1.getBlue() * (1 - grad) + titleBg2.getBlue() * grad);
                        buffer.setRGB(x, y, new Color(r, g, b).getRGB());
                    } catch (Exception e) {}
                }
            }
        }
        
        // Draw title
        PixelFont.drawText(renderer, panelX + 15, panelY + 10, "DEPREDADOR", 2, new Color(255, 200, 200));
        
        // Draw depredador ID with larger font
        String depInfo = "ID: #" + dep.getDepredadorId();
        PixelFont.drawText(renderer, panelX + 15, panelY + 45, depInfo, 3, new Color(255, 150, 100));
        
        // Draw type
        PixelFont.drawText(renderer, panelX + 15, panelY + 75, "Cazador", 2, new Color(200, 80, 80));
        
        // Draw stats
        int lineY = panelY + 110;
        
        // Velocidad (Speed)
        double speed = dep.getSpeed();
        String speedText = String.format("Velocidad: %.2f", speed);
        PixelFont.drawText(renderer, panelX + 15, lineY, speedText, 1, new Color(255, 180, 180));
        lineY += 18;
        
        // Tiempo en mapa (Time in map)
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - dep.getSpawnTime();
        double timeInMap = elapsed / 1000.0;
        String timeText = String.format("T. en mapa: %.1fs", timeInMap);
        PixelFont.drawText(renderer, panelX + 15, lineY, timeText, 1, new Color(255, 200, 180));
        lineY += 18;
        
        // Característica especial
        PixelFont.drawText(renderer, panelX + 15, lineY, "Puede cazar presas", 1, new Color(255, 100, 100));
        lineY += 18;
        
        PixelFont.drawText(renderer, panelX + 15, lineY, "al tocarlas", 1, new Color(255, 100, 100));
        lineY += 30;
        
        // Draw eliminate button (no evolution buttons for depredador)
        int buttonHeight = 28;
        int buttonWidth = 130;
        int btnX = panelX + (panelWidth - buttonWidth) / 2; // Centered
        int btnY = lineY;
        
        drawEvolutionButton(buffer, btnX, btnY, buttonWidth, buttonHeight, "ELIMINAR", true, panelX, panelY, panelWidth);
        
        // Store button bounds for click detection
        deleteAnimalButton = new ButtonBounds(btnX, btnY, buttonWidth, buttonHeight, "delete_depredador");
        
        // Draw hint
        PixelFont.drawText(renderer, panelX + 15, panelY + panelHeight - 15, "ESC para cerrar", 1, new Color(150, 150, 150));
    }
}
