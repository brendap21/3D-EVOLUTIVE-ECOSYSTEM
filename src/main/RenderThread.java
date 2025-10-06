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
            // Actualiza controles antes de renderizar
            if(controles != null) controles.actualizar();

            panel.render(entidades, cam);

            try { Thread.sleep(16); } catch(Exception e){}
        }
    }
}
