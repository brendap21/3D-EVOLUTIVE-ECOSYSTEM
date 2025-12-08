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
 * AnimalType04: "Criatura Alargada" (elongated creature)
 * Color: Amarillo-verdoso, voxel fino.
 * Forma: Cuerpo alargado tipo serpiente (múltiples voxels en línea).
 * Evolución: Se alarga más, muta tonos de amarillo.
 */
public class AnimalType04 implements Renderable {
    private Vector3 posicion;
    private List<Vector3> voxels;
    private int voxelSize;
    private Color color;
    private double rotY = 0.0;
    private double bob = 0.0;
    private double speed = 1.2;
    private long seed;

    public AnimalType04(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxels = new ArrayList<>();
        generateFromSeed(seed);
    }

    private void generateFromSeed(long seed) {
        Random r = new Random(seed);
        this.voxelSize = 8 + r.nextInt(4); // 3..4 pixels
        
        // Color: yellow-green
        int rcol = 100 + r.nextInt(100);
        int gcol = 150 + r.nextInt(80);
        int bcol = 30 + r.nextInt(60);
        this.color = new Color(rcol, gcol, bcol);
        
        this.speed = 1.0 + r.nextDouble() * 1.0;

        // Ultra-simplified elongated: 3 voxels in a line
        voxels.add(new Vector3(0, 0, -1));  // Head
        voxels.add(new Vector3(0, 0, 0));   // Middle
        voxels.add(new Vector3(0, 0, 1));   // Tail
    }

    public void mutate(long newSeed) {
        Random r = new Random(newSeed);
        // Shift yellow-green tones
        int r1 = Math.min(255, color.getRed() + r.nextInt(50) - 20);
        int g1 = Math.min(255, color.getGreen() + r.nextInt(50) - 20);
        int b1 = Math.max(0, color.getBlue() + r.nextInt(40) - 30);
        this.color = new Color(r1, g1, b1);
        
        // Possibly add a segment
        if (r.nextDouble() < 0.35 && voxels.size() < 12) {
            int idx = r.nextInt(voxels.size());
            Vector3 base = voxels.get(idx);
            voxels.add(new Vector3(base.x + r.nextInt(3) - 1, base.y, base.z + r.nextInt(3) - 1));
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
        rotY += speed * 0.004;
        if (rotY > 2 * Math.PI) rotY -= 2 * Math.PI;
        bob += speed * 0.02;
        if (bob > 2 * Math.PI) bob -= 2 * Math.PI;
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        for (int i = 0; i < voxels.size(); i++) {
            Vector3 voxel = voxels.get(i);
            double waveOffset = Math.sin(bob + i * 0.5) * 2;
            Vector3 worldPos = new Vector3(
                posicion.x + voxel.x * voxelSize + waveOffset,
                posicion.y + voxel.y * voxelSize,
                posicion.z + voxel.z * voxelSize
            );
            Vector3[] vertices = renderer.getCubeVertices(worldPos, voxelSize, 0);
            renderer.drawCubeShaded(vertices, cam, color);
        }
    }
}
