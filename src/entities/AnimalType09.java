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
 * AnimalType09: "Criatura Cristalina" (crystalline creature)
 * Color: Violeta-gris, voxel fino, forma geométrica.
 * Forma: Estructura rígida en forma de rombo/diamante.
 * Evolución: Cambia de tamaño de voxel, tonos más plateados.
 */
public class AnimalType09 extends BaseAnimal {
    private double rotY = 0.0;
    private double bob = 0.0;
    private double speed = 1.3;

    public AnimalType09(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxels = new ArrayList<>();
        generateFromSeed(seed);
        initializeSpawnAnimation();
    }

    private void generateFromSeed(long seed) {
        Random r = new Random(seed);
        this.voxelSize = 8 + r.nextInt(4); // 3..4 pixels
        
        // Color: purple-gray
        int rcol = 110 + r.nextInt(80);
        int gcol = 80 + r.nextInt(80);
        int bcol = 130 + r.nextInt(80);
        this.color = new Color(rcol, gcol, bcol);
        
        this.speed = 1.1 + r.nextDouble() * 0.9;

        // Ultra-simplified crystalline: 3 voxels
        voxels.add(new Vector3(0, 0, 0));   // Center
        voxels.add(new Vector3(0, 1, 0));   // Top
        voxels.add(new Vector3(0, -1, 0));  // Bottom
    }

    public void mutate(long newSeed) {
        Random r = new Random(newSeed);
        // Shift to silver-gray
        int r1 = Math.min(255, color.getRed() + r.nextInt(50) - 20);
        int g1 = Math.min(255, color.getGreen() + r.nextInt(50) - 20);
        int b1 = Math.max(0, Math.min(255, color.getBlue() + r.nextInt(40) - 30));
        this.color = new Color(r1, g1, b1);
        
        // Change voxel size
        voxelSize = Math.max(10, Math.min(28, voxelSize + r.nextInt(6) - 3));
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
