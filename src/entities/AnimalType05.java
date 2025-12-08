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
 * AnimalType05: "Criatura con Espinas" (spiky creature)
 * Color: Rojo oscuro, voxel mediano con protuberancias.
 * Forma: Núcleo central + múltiples púas.
 * Evolución: Aumenta espinas, muta color hacia rojo más intenso.
 */
public class AnimalType05 implements Renderable {
    private Vector3 posicion;
    private List<Vector3> voxels;
    private int voxelSize;
    private Color color;
    private double rotY = 0.0;
    private double bob = 0.0;
    private double speed = 1.5;
    private long seed;

    public AnimalType05(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxels = new ArrayList<>();
        generateFromSeed(seed);
    }

    private void generateFromSeed(long seed) {
        Random r = new Random(seed);
        this.voxelSize = 8 + r.nextInt(4); // 3..4 pixels
        
        // Color: dark red
        int rcol = 120 + r.nextInt(100);
        int gcol = 30 + r.nextInt(50);
        int bcol = 20 + r.nextInt(40);
        this.color = new Color(rcol, gcol, bcol);
        
        this.speed = 1.2 + r.nextDouble() * 1.0;

        // Ultra-simplified spiky: core + 2 spikes
        voxels.add(new Vector3(0, 0, 0));   // Core
        voxels.add(new Vector3(1, 0, 0));   // Spike right
        voxels.add(new Vector3(-1, 0, 0));  // Spike left
    }

    public void mutate(long newSeed) {
        Random r = new Random(newSeed);
        // Intensify red
        int r1 = Math.min(255, color.getRed() + r.nextInt(60) - 10);
        int g1 = Math.max(0, color.getGreen() - r.nextInt(30));
        int b1 = Math.max(0, color.getBlue() - r.nextInt(30));
        this.color = new Color(r1, g1, b1);
        
        // Add more spikes
        if (r.nextDouble() < 0.4 && voxels.size() < 14) {
            int dx = r.nextInt(5) - 2;
            int dy = r.nextInt(5) - 2;
            int dz = r.nextInt(5) - 2;
            if (dx != 0 || dy != 0 || dz != 0) {
                voxels.add(new Vector3(dx, dy, dz));
            }
        }
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
        rotY += speed * 0.005;
        if (rotY > 2 * Math.PI) rotY -= 2 * Math.PI;
        bob += speed * 0.015;
        if (bob > 2 * Math.PI) bob -= 2 * Math.PI;
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        for (Vector3 voxel : voxels) {
            Vector3 worldPos = new Vector3(
                posicion.x + voxel.x * voxelSize,
                posicion.y + voxel.y * voxelSize + Math.sin(bob) * 1.8,
                posicion.z + voxel.z * voxelSize
            );
            Vector3[] vertices = renderer.getCubeVertices(worldPos, voxelSize, 0);
            renderer.drawCubeShaded(vertices, cam, color);
        }
    }
}
