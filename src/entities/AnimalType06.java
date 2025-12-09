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
 * AnimalType06: "Criatura Radiante" (radiant creature)
 * Color: Azul brillante, voxel pequeño.
 * Forma: Centro con 6 puntas en estrella (cruz 3D).
 * Evolución: Se hace más brillante, cambia a tonos cianitas.
 */
public class AnimalType06 extends BaseAnimal {
    private double rotY = 0.0;
    private double bob = 0.0;
    private double speed = 1.9;

    public AnimalType06(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxels = new ArrayList<>();
        generateFromSeed(seed);
        initializeSpawnAnimation();
    }

    private void generateFromSeed(long seed) {
        Random r = new Random(seed);
        this.voxelSize = 8 + r.nextInt(4); // 3..5 pixels
        
        // Color: bright blue
        int rcol = 30 + r.nextInt(80);
        int gcol = 100 + r.nextInt(100);
        int bcol = 200 + r.nextInt(55);
        this.color = new Color(rcol, gcol, bcol);
        
        this.speed = 1.7 + r.nextDouble() * 1.2;

        // Ultra-simplified radiant: center + 4 arms (2D cross)
        voxels.add(new Vector3(0, 0, 0));   // Center
        voxels.add(new Vector3(1, 0, 0));   // Right
        voxels.add(new Vector3(-1, 0, 0));  // Left
        voxels.add(new Vector3(0, 1, 0));   // Top
    }

    public void mutate(long newSeed) {
        Random r = new Random(newSeed);
        // Brighten and shift to cyan
        int r1 = Math.max(0, color.getRed() - r.nextInt(50));
        int g1 = Math.min(255, color.getGreen() + r.nextInt(50));
        int b1 = Math.min(255, color.getBlue() + r.nextInt(30));
        this.color = new Color(r1, g1, b1);
        
        this.speed = Math.min(4.0, speed * (0.95 + r.nextDouble() * 0.3));
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
    protected void renderNormal(SoftwareRenderer renderer, Camera cam) {
        Color glowColor = applyGlowToColor(color);
        for (Vector3 voxel : voxels) {
            Vector3 worldPos = new Vector3(
                posicion.x + voxel.x * voxelSize,
                posicion.y + voxel.y * voxelSize,
                posicion.z + voxel.z * voxelSize
            );
            worldPos = applyScaleToPosition(worldPos);
            int scaledSize = applyScaleToSize(voxelSize);
            Vector3[] vertices = renderer.getCubeVertices(worldPos, scaledSize, 0);
            renderer.drawCubeShaded(vertices, cam, glowColor);
        }
    }
}
