package main;

import math.Camera;
import java.util.Collections;
import java.util.List;
import simulation.Mundo;
import ui.Controles;

public class RenderThread extends Thread {
    private RenderPanel panel;
    private Mundo mundo;
    private Camera cam;
    private Controles controles;

    public RenderThread(RenderPanel panel, Mundo mundo, Camera cam, Controles controles){
        this.panel = panel;
        this.mundo = mundo;
        this.cam = cam;
        this.controles = controles;
    }

    @Override
    public void run(){
        while(true){
            // Actualiza controles antes de renderizar, pero sólo si no estamos en pausa
            // y si el panel de animal no está abierto.
            // Cuando el juego está pausado o cuando un animal está seleccionado, 
            // queremos congelar la cámara y todas las actualizaciones dependientes del input.
            boolean shouldFreeze = controles != null && (controles.isPaused() || controles.isAnimalPanelOpen());
            if(controles != null && !shouldFreeze) controles.actualizar();

            List<Renderable> snapshot = mundo != null ? mundo.snapshotEntities() : Collections.emptyList();
            
            // Update all entities (for spawn animations, etc.)
            if (!shouldFreeze) {
                for (Renderable r : snapshot) {
                    r.update();
                }
            }
            
            panel.render(snapshot, cam, controles);

            try { Thread.sleep(7); } catch(Exception e){} // 7ms = ~143 FPS (mucho más fluido)
        }
    }
}
