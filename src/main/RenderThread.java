// src/main/RenderThread.java
package main;

import java.util.List;
import entities.Renderable;

public class RenderThread extends Thread {
    private RenderPanel panel;
    private List<Renderable> entidades;
    private boolean running = true;

    public RenderThread(RenderPanel panel, List<Renderable> entidades){
        this.panel = panel;
        this.entidades = entidades;
    }

    public void terminate() {
        running = false;
    }

    @Override
    public void run(){
        while(running){
            panel.render(entidades);
            try{
                Thread.sleep(16); // ~60 FPS
            } catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    }
}
