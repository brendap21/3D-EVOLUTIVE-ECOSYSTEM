package main;

import javax.swing.JFrame;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import entities.Cubo;
import entities.Cilindro;
import entities.Renderable;
import math.Vector3;
import math.Camera;

public class EcosistemaApp {
    public static void main(String[] args) {
        int ancho = 800, alto = 600;
        Camera cam = new Camera(new Vector3(0,50,-200), 500);

        RenderPanel panel = new RenderPanel(ancho, alto);

        JFrame frame = new JFrame("3D Evolutive Ecosystem");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(ancho, alto);
        frame.add(panel);
        frame.setVisible(true);

        List<Renderable> entidades = new ArrayList<>();
        entidades.add(new Cubo(new Vector3(0,0,0), 50, Color.RED));
        entidades.add(new Cilindro(new Vector3(100,0,0), 25, 100, 20, Color.BLUE));

        RenderThread rt = new RenderThread(panel, entidades, cam);
        rt.start();
    }
}
