package main;

import javax.swing.JFrame;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import math.Camera;
import math.Vector3;
import entities.*;
import java.awt.Color;
import ui.Controles;

public class EcosistemaApp {
    public static void main(String[] args){
        int ancho = 1000, alto = 700;

        Camera cam = new Camera(new Vector3(0,80,-150), 500);

        RenderPanel panel = new RenderPanel(ancho, alto);

        JFrame frame = new JFrame("3D EVOLUTIVE ECOSYSTEM");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Use Mundo to manage entities and animals
        simulation.Mundo mundo = new simulation.Mundo();
        // Expand terrain: 160x160 grid, 8-unit scale = 1280 world units
        mundo.addEntity(new Terreno(160, 160, 8.0, 12345L, new Color(60, 140, 60)));

        // Spawn random animals (1-5 of 10 types)
        spawnRandomAnimals(mundo);

        // Spawn environmental entities (trees, rocks, grass, bushes, flowers)
        spawnEnvironmentalEntities(mundo);
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

    /**
     * Spawn 1-5 random animals from the 10 available animal types.
     * Each animal is placed with minimum distance separation to avoid overlap.
     */
    private static void spawnRandomAnimals(simulation.Mundo mundo) {
        Random r = new Random(System.currentTimeMillis());
        int numAnimals = 10 + r.nextInt(5); // 10-14 animals
        java.util.List<Vector3> usedPositions = new ArrayList<>();
        double minDistance = 50; // Reduced for more density

        int attempts = 0;
        int spawned = 0;
        while (spawned < numAnimals && attempts < 300) {
            int animalType = r.nextInt(10); // 0-9: 10 types
            long seed = System.currentTimeMillis() + spawned * 1000 + r.nextLong();

            // Spawn in visible area in front of camera (camera is at 0,50,-200)
            double x = -150 + r.nextDouble() * 300; // -150 to 150
            double z = -100 + r.nextDouble() * 200; // -100 to 100 (in front of camera)
            double y = 15; // Fixed height above ground
            Vector3 pos = new Vector3(x, y, z);

            // Check minimum distance from other animals
            boolean tooClose = false;
            for (Vector3 used : usedPositions) {
                double dist = Math.sqrt((pos.x - used.x) * (pos.x - used.x) + 
                                       (pos.z - used.z) * (pos.z - used.z));
                if (dist < minDistance) {
                    tooClose = true;
                    break;
                }
            }

            if (!tooClose) {
                usedPositions.add(pos);
                Renderable animal = createAnimalOfType(animalType, pos, seed);
                if (animal != null) {
                    if (animal instanceof Animal) {
                        mundo.addAnimal((Animal) animal);
                    } else {
                        mundo.addEntity(animal);
                    }
                    spawned++;
                }
            }
            attempts++;
        }
    }

    /**
     * Create an animal of the specified type (0-9).
     */
    public static Renderable createAnimalOfType(int type, Vector3 pos, long seed) {
        switch (type) {
            case 0: return new AnimalType01(pos, seed);
            case 1: return new AnimalType02(pos, seed);
            case 2: return new AnimalType03(pos, seed);
            case 3: return new AnimalType04(pos, seed);
            case 4: return new AnimalType05(pos, seed);
            case 5: return new AnimalType06(pos, seed);
            case 6: return new AnimalType07(pos, seed);
            case 7: return new AnimalType08(pos, seed);
            case 8: return new AnimalType09(pos, seed);
            case 9: return new AnimalType10(pos, seed);
            default: return null;
        }
    }

    /**
     * Spawn environmental entities (trees, rocks, grass, bushes, flowers) randomly across the terrain.
     * Uses minimum distance to prevent overlap and clustering.
     */
    private static void spawnEnvironmentalEntities(simulation.Mundo mundo) {
        Random r = new Random(System.currentTimeMillis() + 12345);
        java.util.List<Vector3> usedPositions = new ArrayList<>();

        // Spawn trees (3-5) - fewer for performance
        int numTrees = 3 + r.nextInt(3);
        spawnWithMinDistance(mundo, r, numTrees, 100, usedPositions, "tree");

        // Spawn rocks (4-8) - reduced
        int numRocks = 4 + r.nextInt(5);
        spawnWithMinDistance(mundo, r, numRocks, 50, usedPositions, "rock");

        // Spawn grass (8-12) - reduced
        int numGrass = 8 + r.nextInt(5);
        spawnWithMinDistance(mundo, r, numGrass, 30, usedPositions, "grass");

        // Spawn bushes (3-6) - reduced
        int numBushes = 3 + r.nextInt(4);
        spawnWithMinDistance(mundo, r, numBushes, 60, usedPositions, "bush");

        // Spawn flowers (2-4) - reduced
        int numFlowers = 2 + r.nextInt(3);
        spawnWithMinDistance(mundo, r, numFlowers, 40, usedPositions, "flower");
    }

    /**
     * Helper method to spawn entities with minimum distance separation.
     */
    private static void spawnWithMinDistance(simulation.Mundo mundo, Random r, int count, 
                                            double minDistance, java.util.List<Vector3> usedPositions, String type) {
        int spawned = 0;
        int attempts = 0;
        int maxAttempts = count * 10; // Reasonable attempt limit

        Color[] flowerColors = {
            new Color(255, 0, 127),   // pink
            new Color(255, 165, 0),   // orange
            new Color(255, 255, 0),   // yellow
            new Color(0, 255, 127),   // spring green
            new Color(0, 128, 255),   // dodger blue
            new Color(199, 21, 133)   // medium violet red
        };

        while (spawned < count && attempts < maxAttempts) {
            // Expanded range for large terrain (-300 to +300)
            double x = -300 + r.nextDouble() * 600;
            double z = -300 + r.nextDouble() * 600;
            Vector3 pos = new Vector3(x, 0, z);

            // Check distance from all existing positions
            boolean tooClose = false;
            for (Vector3 used : usedPositions) {
                double dist = Math.sqrt((pos.x - used.x) * (pos.x - used.x) + 
                                       (pos.z - used.z) * (pos.z - used.z));
                if (dist < minDistance) {
                    tooClose = true;
                    break;
                }
            }

            if (!tooClose) {
                usedPositions.add(pos);
                
                switch (type) {
                    case "tree":
                        Arbol arbol = new Arbol(pos, 10 + r.nextInt(8), 50 + r.nextInt(40), 25 + r.nextInt(20));
                        mundo.addEntity(arbol);
                        break;
                    case "rock":
                        Piedra piedra = new Piedra(pos);
                        mundo.addEntity(piedra);
                        break;
                    case "grass":
                        Pasto pasto = new Pasto(pos);
                        mundo.addEntity(pasto);
                        break;
                    case "bush":
                        Arbusto arbusto = new Arbusto(pos);
                        mundo.addEntity(arbusto);
                        break;
                    case "flower":
                        Color flowerColor = flowerColors[r.nextInt(flowerColors.length)];
                        Flor flor = new Flor(pos, flowerColor);
                        mundo.addEntity(flor);
                        break;
                }
                spawned++;
            }
            attempts++;
        }
    }
}
