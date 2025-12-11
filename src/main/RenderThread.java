package main;

import math.Camera;
import java.util.Collections;
import java.util.List;
import java.awt.image.BufferedImage;
import simulation.Mundo;
import ui.Controles;

/**
 * ============================================================================================
 * RenderThread - Hilo de renderizado (bucle principal de dibujo)
 * ============================================================================================
 * 
 * PROPÓSITO:
 * Ejecuta el bucle de renderizado en un hilo separado del AWT Event Dispatch Thread.
 * Esto permite que la UI responda mientras se dibuja la escena 3D.
 * 
 * RESPONSABILIDADES:
 * 1. Actualizar controles (movimiento de cámara basado en input)
 * 2. Obtener snapshot thread-safe de todas las entidades del mundo
 * 3. Actualizar animaciones de entidades (spawn effects, movimiento)
 * 4. Llamar al RenderPanel para dibujar la escena
 * 5. Controlar framerate (~143 FPS con sleep de 7ms)
 * 
 * CONCEPTOS IMPLEMENTADOS:
 * 1. MULTI-THREADING:
 *    - Separación de lógica de render del Event Dispatch Thread
 *    - Evita congelar la UI durante operaciones costosas
 *    - Thread.sleep() para limitar framerate y reducir uso de CPU
 * 
 * 2. PAUSA ABSOLUTA:
 *    - Cuando isPaused() o isAnimalPanelOpen() = true:
 *      * NO actualizar controles (cámara congelada)
 *      * NO actualizar entidades (animaciones congeladas)
 *      * SÍ seguir dibujando (para mostrar menú de pausa)
 *    - Implementa congelamiento completo del juego
 * 
 * 3. SNAPSHOT PATTERN:
 *    - mundo.snapshotEntities() crea copia defensiva de la lista
 *    - Evita ConcurrentModificationException si otra thread modifica el mundo
 *    - Thread-safety esencial en arquitecturas multi-hilo
 * 
 * 4. FRAME TIMING:
 *    - 7ms sleep = ~143 FPS máximo
 *    - Balance entre fluidez visual y uso de CPU
 *    - 60 FPS = 16.67ms, 143 FPS = 7ms (muy fluido para movimiento de cámara)
 * 
 * ============================================================================================
 */
public class RenderThread extends Thread {
    // Referencias a los componentes principales del sistema
    private RenderPanel panel;        // Panel donde se dibuja (contiene el backBuffer)
    private Mundo mundo;              // Contenedor de todas las entidades
    private Camera cam;               // Cámara 3D (posición + orientación)
    private Controles controles;      // Sistema de input (teclado + mouse)
    private DisplayPanel displayPanel; // Panel para mostrar imagen

    /**
     * Constructor: Inicializa el hilo de renderizado con referencias a componentes.
     * 
     * @param panel Panel de renderizado (donde se dibuja el backBuffer)
     * @param mundo Mundo con todas las entidades a dibujar
     * @param cam Cámara 3D (define punto de vista)
     * @param controles Sistema de controles (para actualizar posición/orientación cámara)
     * @param displayPanel Panel para mostrar la imagen renderizada
     */
    public RenderThread(RenderPanel panel, Mundo mundo, Camera cam, Controles controles, DisplayPanel displayPanel){
        this.panel = panel;
        this.mundo = mundo;
        this.cam = cam;
        this.controles = controles;
        this.displayPanel = displayPanel;
    }

    /**
     * ========================================================================================
     * run - Bucle principal de renderizado (ejecuta mientras el programa esté activo)
     * ========================================================================================
     * 
     * FLUJO DEL BUCLE:
     * 1. Verificar estado de pausa (isPaused o isAnimalPanelOpen)
     * 2. Si NO está pausado:
     *    a. Actualizar controles (mover/rotar cámara según input)
     *    b. Actualizar entidades (animaciones, movimiento)
     * 3. Obtener snapshot thread-safe de entidades
     * 4. Llamar a panel.render() para dibujar la escena
     * 5. Dormir 7ms para limitar a ~143 FPS
     * 6. Repetir infinitamente
     * 
     * PAUSA ABSOLUTA:
     * Cuando shouldFreeze = true, NO se actualizan controles ni entidades.
     * Solo se dibuja la escena (para mostrar menú de pausa y últim frame congelado).
     */
    @Override
    public void run(){
        while(true){
            // VERIFICAR ESTADO DE PAUSA
            // shouldFreeze = true cuando:
            // - Usuario presionó ESC (menú de pausa)
            // - Usuario abrió panel de info de un animal
            boolean shouldFreeze = controles != null && (controles.isPaused() || controles.isAnimalPanelOpen());
            
            // ACTUALIZAR CONTROLES (solo si NO está pausado)
            // controles.actualizar() procesa input de teclado/mouse y mueve la cámara
            // Implementa movimiento tipo FPS: WASD, espacio, ctrl, mouse look
            if(controles != null && !shouldFreeze) controles.actualizar();

            // OBTENER SNAPSHOT DE ENTIDADES
            // snapshotEntities() crea una copia de la lista (thread-safe)
            // Evita ConcurrentModificationException si el Simulador agrega/quita animales
            List<Renderable> snapshot = mundo != null ? mundo.snapshotEntities() : Collections.emptyList();
            
            // ACTUALIZAR ENTIDADES (solo si NO está pausado)
            // Cada entidad.update() puede:
            // - Animar spawn (fade in desde transparente)
            // - Mover el animal (caminar, saltar)
            // - Actualizar fase de crecimiento
            // - Consumir energía/hambre
            if (!shouldFreeze) {
                for (Renderable r : snapshot) {
                    r.update();
                }
            }
            
            // RENDERIZAR ESCENA
            // panel.render() ejecuta todo el pipeline 3D:
            // 1. Limpiar buffers (color + z-buffer)
            // 2. Para cada entidad:
            //    a. Aplicar transformaciones (model matrix)
            //    b. Aplicar view matrix (cámara)
            //    c. Aplicar projection matrix (perspectiva)
            //    d. Rasterizar triángulos con z-test
            // 3. Dibujar HUD (estadísticas, crosshair, menús)
            // 4. Swap buffers (mostrar backBuffer en pantalla)
            panel.render(snapshot, cam, controles);
            
            // MOSTRAR IMAGEN EN PANTALLA
            // Actualizar el DisplayPanel con la imagen renderizada
            BufferedImage img = panel.getRenderedImage();
            if (img != null && displayPanel != null) {
                displayPanel.setImage(img);
                displayPanel.repaint();
            }

            // LIMITAR FRAMERATE
            // Sleep de 7ms = ~143 FPS máximo
            // Reduce uso de CPU y batería sin sacrificar fluidez
            try { Thread.sleep(7); } catch(Exception e){}
        }
    }
}
