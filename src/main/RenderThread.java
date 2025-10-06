package main;

import java.util.List;
import entities.Renderable;
import math.Camera;

public class RenderThread extends Thread {
    private RenderPanel panel;
    private List<Renderable> objetos;
    private Camera cam;

    public RenderThread(RenderPanel panel, List<Renderable> objetos, Camera cam){
        this.panel = panel;
        this.objetos = objetos;
        this.cam = cam;
    }

    @Override
    public void run(){
        while(true){
            for(Renderable e : objetos){
                e.update(); // Actualiza rotación
            }
            panel.render(objetos, cam);
            try {
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException ex){}
        }
    }
}
