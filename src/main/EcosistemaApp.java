package main;

import javax.swing.JFrame;
import java.util.ArrayList;
import java.util.List;
import math.Camera;
import math.Vector3;
import entities.Cubo;
import entities.Cilindro;
import entities.Terreno;
import entities.Animal;
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

    // Use Mundo to manage entities and animals
    simulation.Mundo mundo = new simulation.Mundo();
    mundo.addEntity(new Terreno(80, 80, 8.0, 12345L, new Color(60,140,60)));
    mundo.addAnimal(new Animal(new Vector3(0, 10, 40), 1001L));
    mundo.addAnimal(new Animal(new Vector3(-80, 10, 60), 2002L));
    mundo.addAnimal(new Animal(new Vector3(80, 10, 20), 3003L));
    // Decorative objects
    mundo.addEntity(new Cubo(new Vector3(0,0,0), 50, Color.RED));
    mundo.addEntity(new Cilindro(new Vector3(100,0,50), 20, 80, Color.BLUE));

        Controles controles = new Controles(cam, panel);
    // Give controls and panel a reference to the world so they can save/load animals
    controles.setMundoAndCorrectPosition(mundo);
    panel.setMundo(mundo);
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

    // Start the renderer thread using the world's entity list
    RenderThread hilo = new RenderThread(panel, mundo.getEntities(), cam, controles);
    hilo.start();

    // Start the simulation thread that mutates animals deterministically
    simulation.Simulador sim = new simulation.Simulador(mundo, 5555L);
    sim.start();
    }
}
