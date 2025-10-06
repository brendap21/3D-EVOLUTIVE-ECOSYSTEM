package ui;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import math.Camera;
import math.Vector3;

public class Controles extends KeyAdapter {

    private Camera cam;
    private boolean[] teclas = new boolean[256];
    private double velocidad = 5.0;

    public Controles(Camera cam) {
        this.cam = cam;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() < 256)
            teclas[e.getKeyCode()] = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() < 256)
            teclas[e.getKeyCode()] = false;
    }

    public void actualizar() {
        Vector3 dir = new Vector3(0, 0, 0);

        if(teclas[KeyEvent.VK_W]) dir = dir.add(cam.getForward().scale(velocidad));
        if(teclas[KeyEvent.VK_S]) dir = dir.subtract(cam.getForward().scale(velocidad));
        if(teclas[KeyEvent.VK_D]) dir = dir.subtract(cam.getRight().scale(velocidad));
        if(teclas[KeyEvent.VK_A]) dir = dir.add(cam.getRight().scale(velocidad));
        if(teclas[KeyEvent.VK_DOWN]) dir = dir.add(new Vector3(0, velocidad, 0));
        if(teclas[KeyEvent.VK_UP]) dir = dir.subtract(new Vector3(0, velocidad, 0));

        cam.setPosicion(cam.getPosicion().add(dir));
    }
}
