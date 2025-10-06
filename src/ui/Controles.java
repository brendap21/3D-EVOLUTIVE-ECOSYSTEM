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
        // Obtenemos la posición actual de la cámara
        Vector3 pos = cam.getPosicion();

        if (teclas[KeyEvent.VK_W]) pos.z += velocidad;
        if (teclas[KeyEvent.VK_S]) pos.z -= velocidad;
        if (teclas[KeyEvent.VK_A]) pos.x -= velocidad;
        if (teclas[KeyEvent.VK_D]) pos.x += velocidad;
        if (teclas[KeyEvent.VK_Q]) pos.y += velocidad;
        if (teclas[KeyEvent.VK_E]) pos.y -= velocidad;

        // Actualizamos la posición en la cámara
        cam.setPosicion(pos);
    }
}
