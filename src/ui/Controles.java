package ui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import math.Camera;
import simulation.Mundo;
import java.io.File;
import math.Vector3;

public class Controles extends KeyAdapter implements MouseMotionListener {

    private Camera cam;
    private boolean[] teclas = new boolean[256];
    private double velocidad = 5.0;
    private double sensibilidad = 0.0035; // ajustar sensibilidad del mouse
    private int lastX = -1, lastY = -1;

    // Mouse lock
    private Robot robot;
    private Component comp; // componente donde se captura el mouse (RenderPanel)
    private boolean mouseLocked = false;
    private int centerXScreen = 0, centerYScreen = 0;
    private Cursor blankCursor;
    private boolean flyMode = true; // when true, W/S move along full forward vector (including Y)
    private boolean crosshairVisible = true;
    private boolean debugOverlay = false; // toggled with F3
    // Pause state
    private boolean paused = false;
    // For free-look absolute mapping
    private double baseYaw = 0.0;
    private double basePitch = 0.0;
    private boolean freeLookBaseSet = false;
    private Mundo mundo;
    private double eyeHeight = 20.0; // camera eye offset above ground (shared) (increased for safety)
    
    // Animal spawner menu
    private AnimalSpawnerMenu spawnerMenu;
    
    // Animal panel state
    private boolean animalPanelOpen = false;

    public Controles(Camera cam, Component comp) {
        this.cam = cam;
        this.comp = comp;
        this.spawnerMenu = new AnimalSpawnerMenu();
        try {
            robot = new Robot();
        } catch (Exception ex) {
            robot = null;
        }

        // Cursor invisible
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");
    }

    public void setMundo(Mundo m){ this.mundo = m; }

    // When assigning the world, immediately ensure the camera is placed above terrain
    public void setMundoAndCorrectPosition(Mundo m){
        this.mundo = m;
        if (this.mundo == null) return;

        math.Vector3 pos = cam.getPosicion();
        double startY = pos.y;
        double safeY = startY;

        double terrainH = this.mundo.getHeightAt(pos.x, pos.z);
        if (terrainH != Double.NEGATIVE_INFINITY) {
            safeY = Math.max(safeY, terrainH + eyeHeight + 1.0);
        }

        double nearbyMargin = 16.0;
        java.util.List<entities.Collidable> coll = this.mundo.getCollidables();
        for (entities.Collidable c : coll) {
            math.Vector3 min = c.getAABBMin();
            math.Vector3 max = c.getAABBMax();
            if (pos.x + nearbyMargin < min.x || pos.x - nearbyMargin > max.x) continue;
            if (pos.z + nearbyMargin < min.z || pos.z - nearbyMargin > max.z) continue;
            safeY = Math.max(safeY, max.y + eyeHeight + 1.0);
        }

        // Only adjust upward if we are below the safe height; never force the camera down.
        if (pos.y < safeY) {
            cam.setPosicion(new math.Vector3(pos.x, safeY, pos.z));
        }
    }

    // Lock or unlock the mouse. When locked, the cursor is hidden and recentred to the panel center.
    public void lockMouse(boolean lock) {
        if (lock == mouseLocked) return;
        mouseLocked = lock;
        if (mouseLocked) {
            // compute center in screen coordinates
            Point p = comp.getLocationOnScreen();
            centerXScreen = p.x + comp.getWidth()/2;
            centerYScreen = p.y + comp.getHeight()/2;
            if (robot != null) robot.mouseMove(centerXScreen, centerYScreen);
            comp.setCursor(blankCursor);
            comp.requestFocus();
            // initialize last positions to center
            lastX = comp.getWidth()/2;
            lastY = comp.getHeight()/2;
        } else {
            comp.setCursor(Cursor.getDefaultCursor());
            // reset last positions so free-look restarts smoothly
            lastX = -1;
            lastY = -1;
            // reset free-look baseline so we'll compute it next frame
            freeLookBaseSet = false;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() < 256)
            teclas[e.getKeyCode()] = true;
        
        // If the animal info panel is open, ESC should only close that flow and do nothing else.
        if (animalPanelOpen) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                setAnimalPanelOpen(false);
            }
            return; // Block pause toggle or other actions while panel is active
        }

        // If spawner menu is open, handle menu-specific keys
        if (spawnerMenu.isOpen()) {
            handleSpawnerMenuKeys(e);
            return;
        }
        
        // If waiting for spawn position and ESC pressed, cancel spawn and return to normal gameplay
        if (spawnerMenu.isWaitingForPosition() && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            spawnerMenu.cancel();
            if (mundo != null) mundo.setWaitingForSpawn(false);
            lockMouse(true);
            return;
        }
        
        // If waiting for spawn position but NOT ESC, consume the event (prevent other keys during spawn)
        if (spawnerMenu.isWaitingForPosition()) {
            return;
        }
        
        // NOTE: ESC is reserved for pause/menu; do NOT unlock the mouse with ESC.
        // Fly mode is always ON in this build (no toggle)
        // Toggle crosshair with 'C'
        if (e.getKeyCode() == KeyEvent.VK_C) {
            crosshairVisible = !crosshairVisible;
        }
        // Toggle debug overlay with F3
        if (e.getKeyCode() == KeyEvent.VK_F3) {
            debugOverlay = !debugOverlay;
        }
        // Open spawner menu with 'Y'
        if (e.getKeyCode() == KeyEvent.VK_Y) {
            spawnerMenu.open();
            if (mundo != null) {
                // unlock mouse to allow menu interaction
                lockMouse(false);
            }
        }
        // Toggle pause with ESC: when paused, show the menu and stop the game updates.
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            setPaused(!paused);
        }
    }
    
    /**
     * Maneja las teclas presionadas cuando el menú de spawn está abierto.
     * Permite navegación con flechas y selección con ENTER.
     */
    private void handleSpawnerMenuKeys(KeyEvent e) {
        int code = e.getKeyCode();
        
        if (code == KeyEvent.VK_UP) {
            spawnerMenu.navigateUp();
        } else if (code == KeyEvent.VK_DOWN) {
            spawnerMenu.navigateDown();
        } else if (code == KeyEvent.VK_ENTER) {
            // Select current option
            int animalType = spawnerMenu.selectCurrentOption();
            if (mundo != null) {
                mundo.setSelectedAnimalType(animalType);
                mundo.setWaitingForSpawn(true);
            }
            // close menu interaction and re-lock mouse for aiming
            lockMouse(true);
        } else if (code == KeyEvent.VK_ESCAPE) {
            // Cancel menu
            spawnerMenu.cancel();
            if (mundo != null) mundo.setWaitingForSpawn(false);
            lockMouse(true);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() < 256)
            teclas[e.getKeyCode()] = false;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Don't apply camera rotation if spawner menu is open
        if (spawnerMenu.isOpen()) {
            lastX = e.getX();
            lastY = e.getY();
            return;
        }
        
        if (mouseLocked) {
            if (robot == null) return;
            // Use screen coordinates so we can recenter with Robot
            int mouseX = e.getXOnScreen();
            int mouseY = e.getYOnScreen();

            int dx = mouseX - centerXScreen;
            int dy = mouseY - centerYScreen;

            // deadzone to ignore tiny movements and the synthetic event generated by Robot
            int deadzone = 1;
            if (Math.abs(dx) <= deadzone && Math.abs(dy) <= deadzone) {
                // ensure last positions match center to avoid accumulation
                lastX = comp.getWidth()/2;
                lastY = comp.getHeight()/2;
                return;
            }

            // apply rotation and recentre cursor
            cam.rotate(dx * sensibilidad, -dy * sensibilidad);
            if (robot != null) {
                robot.mouseMove(centerXScreen, centerYScreen);
                // update last known local positions to the center
                lastX = comp.getWidth()/2;
                lastY = comp.getHeight()/2;
            }
        } else {
            // In free-look we update orientation each frame in actualizar();
            // keep lastX/lastY for potential use but don't apply incremental rotates here.
            lastX = e.getX();
            lastY = e.getY();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    public void actualizar() {
        // Freeze camera while the spawn menu is open
        if (spawnerMenu.isOpen()) {
            return;
        }

        // Update camera orientation from mouse position when not locked (absolute mapping)
        if (!mouseLocked) {
            // compute baseline yaw/pitch once when entering free-look
            if (!freeLookBaseSet) {
                baseYaw = cam.getYaw();
                basePitch = cam.getPitch();
                freeLookBaseSet = true;
            }
            java.awt.Point mp = comp.getMousePosition();
            if (mp != null) {
                int centerXLocal = comp.getWidth() / 2;
                int centerYLocal = comp.getHeight() / 2;
                int dx = mp.x - centerXLocal;
                int dy = mp.y - centerYLocal;
                double newYaw = baseYaw + dx * sensibilidad;
                double newPitch = basePitch - dy * sensibilidad;
                cam.setOrientation(newYaw, newPitch);
            }
        }

        Vector3 dir = new Vector3(0, 0, 0);

        // Movimiento: si estamos en flyMode, mover según el forward completo (incluye Y),
        // permitiendo volar hacia donde apunte la cámara. Si no, movemos plano XZ según yaw.
        if (flyMode) {
            Vector3 f = cam.getForward().normalize();
            // Use forward for W/S (includes Y). For strafing, keep horizontal right to avoid unintended vertical strafing.
            Vector3 right = cam.getRight();
            Vector3 rightXZ = new Vector3(right.x, 0, right.z).normalize();

            if(teclas[KeyEvent.VK_W]) dir = dir.add(f.scale(velocidad));
            if(teclas[KeyEvent.VK_S]) dir = dir.subtract(f.scale(velocidad));
            if(teclas[KeyEvent.VK_D]) dir = dir.add(rightXZ.scale(velocidad));
            if(teclas[KeyEvent.VK_A]) dir = dir.subtract(rightXZ.scale(velocidad));
        } else {
            Vector3 f2 = cam.getForward();
            Vector3 forwardXZ = new Vector3(f2.x, 0, f2.z).normalize();
            Vector3 right = cam.getRight();
            Vector3 rightXZ = new Vector3(right.x, 0, right.z).normalize();

            if(teclas[KeyEvent.VK_W]) dir = dir.add(forwardXZ.scale(velocidad));
            if(teclas[KeyEvent.VK_S]) dir = dir.subtract(forwardXZ.scale(velocidad));
            if(teclas[KeyEvent.VK_D]) dir = dir.add(rightXZ.scale(velocidad));
            if(teclas[KeyEvent.VK_A]) dir = dir.subtract(rightXZ.scale(velocidad));
        }

        // Vertical movement: SPACE (or UP arrow) -> move up; CTRL (or DOWN arrow) -> move down
        if(teclas[KeyEvent.VK_SPACE] || teclas[KeyEvent.VK_UP]) dir = dir.add(new Vector3(0, velocidad, 0));
        if(teclas[KeyEvent.VK_CONTROL] || teclas[KeyEvent.VK_DOWN]) dir = dir.subtract(new Vector3(0, velocidad, 0));

        // Attempted new position
        Vector3 current = cam.getPosicion();
        Vector3 attempted = current.add(dir);

        // If there is no world, move freely
        if (mundo == null) {
            cam.setPosicion(attempted);
            return;
        }

        // To avoid tunneling through collidables when movement is large per-frame,
        // step the movement into smaller sub-steps and test collision at each step.
        double dx = attempted.x - current.x;
        double dy = attempted.y - current.y;
        double dz = attempted.z - current.z;
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
        int steps = Math.max(1, (int)Math.ceil(dist / 4.0)); // step size ~4 units
        double stepX = dx / steps;
        double stepY = dy / steps;
        double stepZ = dz / steps;

        Vector3 posNow = current;
        boolean moved = false;
        for(int s=0; s<steps; s++){
            Vector3 tryPos = new Vector3(posNow.x + stepX, posNow.y + stepY, posNow.z + stepZ);

            // Enforce terrain collision at each step
            double terrainH = mundo.getHeightAt(tryPos.x, tryPos.z);
            if(terrainH != Double.NEGATIVE_INFINITY){
                if(tryPos.y < terrainH + eyeHeight) tryPos = new Vector3(tryPos.x, terrainH + eyeHeight, tryPos.z);
            }

            // Sphere-AABB collision test
            double camRadius = 8.0;
            boolean colliding = false;
            java.util.List<entities.Collidable> coll = mundo.getCollidables();
            for(entities.Collidable c : coll){
                math.Vector3 min = c.getAABBMin();
                math.Vector3 max = c.getAABBMax();
                double cx = Math.max(min.x, Math.min(tryPos.x, max.x));
                double cy = Math.max(min.y, Math.min(tryPos.y, max.y));
                double cz = Math.max(min.z, Math.min(tryPos.z, max.z));
                double ddx = tryPos.x - cx;
                double ddy = tryPos.y - cy;
                double ddz = tryPos.z - cz;
                double dist2 = ddx*ddx + ddy*ddy + ddz*ddz;
                if(dist2 < camRadius*camRadius){ colliding = true; break; }
            }

            if(colliding){
                // stop movement on collision (simple response); don't apply this sub-step
                break;
            } else {
                posNow = tryPos;
                moved = true;
            }
        }

        if(moved){
            cam.setPosicion(posNow);
        }
    }

    public boolean isPaused(){
        return paused;
    }

    // Set pause state. When paused we unlock the mouse so the user can click the
    // pixel-drawn menu buttons; when unpausing we lock the mouse again for FPS-style control.
    public void setPaused(boolean p){
        if (this.paused == p) return;
        this.paused = p;
        if (paused) {
            // show cursor so user can interact with menu
            lockMouse(false);
        } else {
            // resume: re-lock mouse for gameplay
            lockMouse(true);
            // reset free-look baseline when returning to gameplay
            freeLookBaseSet = false;
        }
    }

    public boolean isCrosshairVisible() {
        return crosshairVisible;
    }

    public boolean isDebugOverlayEnabled(){ return debugOverlay; }
    
    public Camera getCamera(){ return cam; }
    
    public AnimalSpawnerMenu getSpawnerMenu() { return spawnerMenu; }
    
    public void setAnimalPanelOpen(boolean open) {
        animalPanelOpen = open;
        if (animalPanelOpen) {
            // Unlock mouse to click the panel
            lockMouse(false);
        } else {
            // Lock mouse again
            lockMouse(true);
        }
    }
    
    public boolean isAnimalPanelOpen() {
        return animalPanelOpen;
    }
}

