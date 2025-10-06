package main;

import javax.swing.JFrame;
import math.Camera;
import math.Vector3;
import entities.Cubo;
import java.awt.Color;

public class EcosistemaApp {

    public static void main(String[] args){
        int ancho = 800;
        int alto = 600;

        Camera cam = new Camera(new Vector3(0,0,-200));
        RenderPanel panel = new RenderPanel(ancho, alto, cam);

        JFrame frame = new JFrame("3D Evolutive Ecosystem - Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(ancho, alto);
        frame.add(panel);
        frame.setVisible(true);

        // Cubos de prueba
        Cubo[] cubos = new Cubo[]{
            new Cubo(new Vector3(-50, -50, 100), 20, Color.RED),
            new Cubo(new Vector3(50, 50, 150), 30, Color.GREEN),
            new Cubo(new Vector3(0, 0, 200), 40, Color.BLUE)
        };

        // Loop simple
        while(true){
            panel.render(cubos);
            try{
                Thread.sleep(16); // ~60 FPS
            }catch(Exception e){}
        }
    }
}
