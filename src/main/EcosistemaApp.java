package main;

import javax.swing.JFrame;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import math.Vector3;
import math.Camera;
import entities.Cubo;
import entities.Cilindro;
import entities.Renderable;

public class EcosistemaApp {
    public static void main(String[] args){
        int ancho = 800;
        int alto = 600;

        // Agregamos el FOV que faltaba
        Camera cam = new Camera(new Vector3(0,50,-200), 500);

        RenderPanel panel = new RenderPanel(ancho, alto);
        JFrame frame = new JFrame("Ecosistema 3D");
        frame.setSize(ancho, alto);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.setVisible(true);

        List<Renderable> entidades = new ArrayList<>();
        entidades.add(new Cubo(new Vector3(0,0,0), 50, Color.RED));
        entidades.add(new Cilindro(new Vector3(100,0,0), 20, 80, Color.BLUE));

        RenderThread hilo = new RenderThread(panel, entidades, cam);
        hilo.start();
    }
}
