package main;

import math.Camera;
import java.util.Collections;
import java.util.List;
import java.awt.image.BufferedImage;
import simulation.Mundo;
import ui.Controles;

public class RenderThread extends Thread {
    private RenderPanel panel;
    private Mundo mundo;
    private Camera cam;
    private Controles controles;
    private DisplayPanel displayPanel;

    public RenderThread(RenderPanel panel, Mundo mundo, Camera cam, Controles controles, DisplayPanel displayPanel){
        this.panel = panel;
        this.mundo = mundo;
        this.cam = cam;
        this.controles = controles;
        this.displayPanel = displayPanel;
    }

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
