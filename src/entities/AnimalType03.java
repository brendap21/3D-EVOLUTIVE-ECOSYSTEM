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
 * AnimalType03: "Criatura Voluminosa" (bulky creature)
 * Color: Púrpura-azul, voxel grande.
 * Forma: Bloque central masivo con pequeñas protuberancias.
 * Evolución: Aumenta número de voxels, cambia a colores más cálidos.
 */
public class AnimalType03 implements Renderable {
    private Vector3 posicion;
    private List<Vector3> voxels;
    private int voxelSize;
    private Color color;
    private double rotY = 0.0;
    private double bob = 0.0;
    private double speed = 0.8; // slower, heavier
    private long seed;

    public AnimalType03(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxels = new ArrayList<>();
        generateFromSeed(seed);
    }

    private void generateFromSeed(long seed) {
        Random r = new Random(seed);
        this.voxelSize = 8 + r.nextInt(4); // 3..5 pixels
        
        // Color: purple-blue
        int rcol = 80 + r.nextInt(100);
        int gcol = 50 + r.nextInt(80);
        int bcol = 140 + r.nextInt(100);
        this.color = new Color(rcol, gcol, bcol);
        
        this.speed = 0.6 + r.nextDouble() * 0.8;

        // Ultra-simplified bulky: 3 voxels
        voxels.add(new Vector3(0, 1, 0));  // Top
        voxels.add(new Vector3(0, 0, 0));  // Middle
        voxels.add(new Vector3(0, -1, 0)); // Bottom
    }

    public void mutate(long newSeed) {
        Random r = new Random(newSeed);
        // Shift toward warmer colors
        int r1 = Math.min(255, color.getRed() + r.nextInt(60) - 20);
        int g1 = Math.min(255, color.getGreen() + r.nextInt(60) - 20);
        int b1 = Math.max(0, color.getBlue() - r.nextInt(40));
        this.color = new Color(r1, g1, b1);
        
        // Can add bumps by growing voxels randomly
        if (r.nextDouble() < 0.3 && voxels.size() < 15) {
            int idx = r.nextInt(Math.min(9, voxels.size()));
            Vector3 base = voxels.get(idx);
            voxels.add(new Vector3(base.x + r.nextInt(3) - 1, base.y + 1, base.z + r.nextInt(3) - 1));
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
