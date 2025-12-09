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
            // Actualiza controles antes de renderizar, pero sólo si no estamos en pausa.
            // Cuando el juego está pausado queremos congelar la cámara y todas las
            // actualizaciones dependientes del input.
            if(controles != null && !controles.isPaused()) controles.actualizar();

            List<Renderable> snapshot = mundo != null ? mundo.snapshotEntities() : Collections.emptyList();
            panel.render(snapshot, cam, controles);

            try { Thread.sleep(7); } catch(Exception e){} // 7ms = ~143 FPS (mucho más fluido)
        }
    }
}
