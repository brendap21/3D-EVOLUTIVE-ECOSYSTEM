package main;

import java.util.List;
import entities.Renderable;
import math.Camera;

public class RenderThread extends Thread {
    private RenderPanel panel;
    private List<Renderable> entidades;
    private Camera cam;
    private boolean running = true;

    public RenderThread(RenderPanel panel, List<Renderable> entidades, Camera cam){
        this.panel = panel;
        this.entidades = entidades;
        this.cam = cam;
    }

    @Override
    public void run() {
        while(running){
            panel.render(entidades, cam);
            try{
                Thread.sleep(16); // ~60 FPS
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    public void stopRunning(){
        running = false;
    }
}
