package main;

import javax.swing.JFrame;
import java.util.ArrayList;
import java.util.List;
import math.Camera;
import math.Vector3;
import entities.Cubo;
import entities.Cilindro;
import java.awt.Color;
import ui.Controles;

public class EcosistemaApp {
    public static void main(String[] args){
        int ancho = 800, alto = 600;

        Camera cam = new Camera(new Vector3(0,50,-200), 500);

        RenderPanel panel = new RenderPanel(ancho, alto);

        JFrame frame = new JFrame("3D EVOLUTIVE ECOSYSTEM");
        frame.setSize(ancho, alto);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.setVisible(true);

        List<Renderable> entidades = new ArrayList<>();
        entidades.add(new Cubo(new Vector3(0,0,0), 50, Color.RED));
        entidades.add(new Cilindro(new Vector3(100,0,50), 20, 80, Color.BLUE));

        Controles controles = new Controles(cam, panel);
        // Add listeners to the render panel so it receives mouse and key events
        panel.addMouseMotionListener(controles);
        panel.addKeyListener(controles);
        panel.setFocusable(true);
        panel.requestFocus();

        // Click the panel to lock the mouse (enter mouse-look). When paused the
        // panel will draw a pixel menu and route clicks to the menu; route clicks
        // through the panel so it can handle pause-menu button hit-testing.
        panel.addMouseListener(new java.awt.event.MouseAdapter(){
            @Override
            public void mousePressed(java.awt.event.MouseEvent e){
                panel.handleMousePressed(e, controles);
            }
        });

        // Lock mouse by default so behavior matches Minecraft (cursor hidden & centered,
        // movement uses deltas to update yaw/pitch). If Robot is not available the method
        // handles it gracefully.
        controles.lockMouse(true);

        RenderThread hilo = new RenderThread(panel, entidades, cam, controles);
        hilo.start();
    }
}
