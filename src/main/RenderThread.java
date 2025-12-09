package main;

import java.util.List;
import math.Camera;
import ui.Controles;

public class RenderThread extends Thread {
    private RenderPanel panel;
    private List<Renderable> entidades;
    private Camera cam;
    private Controles controles;

    public RenderThread(RenderPanel panel, List<Renderable> entidades, Camera cam, Controles controles){
        this.panel = panel;
        this.entidades = entidades;
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

            panel.render(entidades, cam, controles);

            try { Thread.sleep(7); } catch(Exception e){} // 7ms = ~143 FPS (mucho más fluido)
        }
    }
}
