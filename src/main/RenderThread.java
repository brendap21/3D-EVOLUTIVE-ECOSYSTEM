package main;

import java.util.List;
import javax.swing.JFrame;
import math.Camera;

public class RenderThread extends Thread {
    private RenderPanel panel;
    private List<Renderable> entidades;
    private Camera cam;

    public RenderThread(RenderPanel panel, List<Renderable> entidades, Camera cam){
        this.panel = panel;
        this.entidades = entidades;
        this.cam = cam;
    }

    @Override
    public void run(){
        while(true){
            panel.render(entidades, cam);
            try { Thread.sleep(16); } catch(Exception e){}
        }
    }
}
