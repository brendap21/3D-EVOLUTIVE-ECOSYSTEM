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
 * AnimalType07: "Criatura Piramidal" (pyramidal creature)
 * Color: Verde oscuro, voxel mediano.
 * Forma: Base grande, se estrecha hacia arriba (pirámide).
 * Evolución: Se estira más, muta verdes más profundos.
 */
public class AnimalType07 implements Renderable {
    private Vector3 posicion;
    private List<Vector3> voxels;
    private int voxelSize;
    private Color color;
    private double rotY = 0.0;
    private double bob = 0.0;
    private double speed = 1.1;
    private long seed;

    public AnimalType07(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxels = new ArrayList<>();
        generateFromSeed(seed);
    }

    private void generateFromSeed(long seed) {
        Random r = new Random(seed);
        this.voxelSize = 8 + r.nextInt(4); // 3..5 pixels
        
        // Color: dark green
        int rcol = 40 + r.nextInt(70);
        int gcol = 80 + r.nextInt(100);
        int bcol = 30 + r.nextInt(60);
        this.color = new Color(rcol, gcol, bcol);
        
        this.speed = 0.9 + r.nextDouble() * 0.8;

        // Ultra-simplified pyramid: 3 voxels
        voxels.add(new Vector3(0, 0, 0));   // Base
        voxels.add(new Vector3(0, 1, 0));   // Middle
        voxels.add(new Vector3(0, 2, 0));   // Top
    }

    public void mutate(long newSeed) {
        Random r = new Random(newSeed);
        // Shift to deeper greens
        int r1 = Math.max(0, color.getRed() - r.nextInt(40));
        int g1 = Math.min(255, color.getGreen() + r.nextInt(40));
        int b1 = Math.max(0, color.getBlue() - r.nextInt(30));
        this.color = new Color(r1, g1, b1);
        
        // Possibly grow taller
        if (r.nextDouble() < 0.3 && voxels.size() < 16) {
            voxels.add(new Vector3(0, 3, 0));
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
        rotY += speed * 0.003;
        if (rotY > 2 * Math.PI) rotY -= 2 * Math.PI;
        bob += speed * 0.012;
        if (bob > 2 * Math.PI) bob -= 2 * Math.PI;
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        for (Vector3 voxel : voxels) {
            Vector3 worldPos = new Vector3(
                posicion.x + voxel.x * voxelSize,
                posicion.y + voxel.y * voxelSize + Math.sin(bob) * 2,
                posicion.z + voxel.z * voxelSize
            );
            Vector3[] vertices = renderer.getCubeVertices(worldPos, voxelSize, 0);
            renderer.drawCubeShaded(vertices, cam, color);
        }
    }
}
