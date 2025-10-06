package main;

import javax.swing.JFrame;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import math.Camera;
import math.Vector3;
import entities.Cubo;
import entities.Cilindro;
import entities.Renderable;

public class EcosistemaApp {

    public static void main(String[] args){
        int ancho = 800;
        int alto = 600;

        Camera cam = new Camera(new Vector3(0, 0, -200));
        RenderPanel panel = new RenderPanel(ancho, alto, cam);

        JFrame frame = new JFrame("3D Evolutive Ecosystem - Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(ancho, alto);
        frame.add(panel);
        frame.setVisible(true);

        // Lista de Renderable
        List<Renderable> entidades = new ArrayList<>();

        // Cubos de prueba
        entidades.add(new Cubo(new Vector3(-50, -50, 100), 20, Color.RED));
        entidades.add(new Cubo(new Vector3(50, 50, 150), 30, Color.GREEN));
        entidades.add(new Cubo(new Vector3(0, 0, 200), 40, Color.BLUE));

        // Cilindro de prueba
        entidades.add(new Cilindro(new Vector3(0, -50, 120), 15, 50, Color.YELLOW));

        // Loop simple (~60 FPS)
        while(true){
            panel.render(entidades);
            try {
                Thread.sleep(16); // ~60 FPS
            } catch(Exception e){}
        }
    }
}
