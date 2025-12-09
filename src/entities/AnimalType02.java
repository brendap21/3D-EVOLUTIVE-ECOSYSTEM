package entities;

import main.Renderable;
import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * AnimalType02: "Bípedo Compacto" (bipedal compact)
 * Color: Naranja-rojo, voxel pequeño.
 * Forma: Cabeza grande, cuerpo central, 2 patas.
 * Evolución: Aumenta tamaño de voxel, muta hacia rojos más intensos.
 */
public class AnimalType02 implements Renderable {
    private Vector3 posicion;
    private List<Vector3> voxels;
    private int voxelSize;
    private Color color;
    private double rotY = 0.0;
    private double bob = 0.0;
    private double speed = 1.8;
    private long seed;

    public AnimalType02(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxels = new ArrayList<>();
        generateFromSeed(seed);
    }

    private void generateFromSeed(long seed) {
        Random r = new Random(seed);
        this.voxelSize = 8 + r.nextInt(4); // 3..5 pixels
        
        // Color: orange-red
        int rcol = 150 + r.nextInt(105);
        int gcol = 60 + r.nextInt(80);
        int bcol = 20 + r.nextInt(60);
        this.color = new Color(rcol, gcol, bcol);
        
        this.speed = 1.5 + r.nextDouble() * 1.2;

        // Ultra-simplified biped: 3 voxels
        voxels.add(new Vector3(0, 1, 0));  // Head
        voxels.add(new Vector3(0, 0, 0));  // Body
        voxels.add(new Vector3(0, -1, 0)); // Leg
    }

    public void mutate(long newSeed) {
        Random r = new Random(newSeed);
        // Intensify red
        int r1 = Math.min(255, color.getRed() + r.nextInt(50) - 10);
        int g1 = Math.max(0, color.getGreen() - r.nextInt(30));
        int b1 = Math.max(0, color.getBlue() - r.nextInt(30));
        this.color = new Color(r1, g1, b1);
        
        // Increase size
        voxelSize = Math.max(12, Math.min(32, voxelSize + r.nextInt(7) - 2));
        
        this.speed = Math.min(4.0, speed * (0.95 + r.nextDouble() * 0.25));
    }

    public Vector3 getAABBMin() {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        for (Vector3 v : voxels) {
            minX = Math.min(minX, v.x * voxelSize);
            minY = Math.min(minY, v.y * voxelSize);
            minZ = Math.min(minZ, v.z * voxelSize);
        }
        return new Vector3(posicion.x + minX, posicion.y + minY, posicion.z + minZ);
    }

    public Vector3 getAABBMax() {
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (Vector3 v : voxels) {
            maxX = Math.max(maxX, (v.x + 1) * voxelSize);
            maxY = Math.max(maxY, (v.y + 1) * voxelSize);
            maxZ = Math.max(maxZ, (v.z + 1) * voxelSize);
        }
        return new Vector3(posicion.x + maxX, posicion.y + maxY, posicion.z + maxZ);
    }

    @Override
    public void update() {
        // Animación deshabilitada
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        for (Vector3 voxel : voxels) {
            Vector3 worldPos = new Vector3(
                posicion.x + voxel.x * voxelSize,
                posicion.y + voxel.y * voxelSize,
                posicion.z + voxel.z * voxelSize
            );
            Vector3[] vertices = renderer.getCubeVertices(worldPos, voxelSize, 0);
            renderer.drawCubeShaded(vertices, cam, color);
        }
    }
}
