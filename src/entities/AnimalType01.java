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
 * AnimalType01: "Cuadrúpedo Ágil" (4-legged agile creature)
 * Color base: Verde azulado, voxel mediano.
 * Forma: Cabeza pequeña, cuerpo principal rectangular, 4 patas.
 * Evolución: Muta color hacia tonos más brillantes, aumenta velocidad.
 */
public class AnimalType01 extends BaseAnimal {
    private double rotY = 0.0;
    private double bob = 0.0;
    private double speed = 2.5; // faster type

    public AnimalType01(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxels = new ArrayList<>();
        generateFromSeed(seed);
        initializeSpawnAnimation();
    }

    private void generateFromSeed(long seed) {
        Random r = new Random(seed);
        this.voxelSize = 3 + r.nextInt(3); // 3..5 pixels (MUY pequeño)
        
        // Base color: teal to cyan
        int rcol = 50 + r.nextInt(100);
        int gcol = 120 + r.nextInt(80);
        int bcol = 130 + r.nextInt(70);
        this.color = new Color(rcol, gcol, bcol);
        
        this.speed = 2.0 + r.nextDouble() * 1.5;

        // Ultra-simplified: only 3 voxels (head + body + 1 leg)
        voxels.add(new Vector3(0, 1, 0));  // Head
        voxels.add(new Vector3(0, 0, 0));  // Body
        voxels.add(new Vector3(0, -1, 0)); // Leg
    }

    public void mutate(long newSeed) {
        Random r = new Random(newSeed);
        // Brighten color slightly
        int r1 = Math.min(255, color.getRed() + r.nextInt(40) - 20);
        int g1 = Math.min(255, color.getGreen() + r.nextInt(40) - 20);
        int b1 = Math.min(255, color.getBlue() + r.nextInt(40) - 20);
        this.color = new Color(Math.max(0, r1), Math.max(0, g1), Math.max(0, b1));
        
        // Increase speed
        this.speed = Math.min(5.0, speed * (0.9 + r.nextDouble() * 0.3));
        
        // Slight size change
        voxelSize = Math.max(10, Math.min(30, voxelSize + r.nextInt(5) - 2));
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
        
        // Draw eyes (small white cubes)
        Vector3 headPos = new Vector3(posicion.x, posicion.y + 3 * voxelSize, posicion.z);
        Vector3 eyeLeft = new Vector3(headPos.x - voxelSize * 0.3, headPos.y + voxelSize * 0.2, headPos.z - voxelSize * 0.3);
        Vector3 eyeRight = new Vector3(headPos.x + voxelSize * 0.3, headPos.y + voxelSize * 0.2, headPos.z - voxelSize * 0.3);
        
        eyeLeft = applyScaleToPosition(eyeLeft);
        eyeRight = applyScaleToPosition(eyeRight);
        
        int eyeSize = applyScaleToSize((int)(voxelSize * 0.25));
        Vector3[] eyeVerticesL = renderer.getCubeVertices(eyeLeft, eyeSize, 0);
        Vector3[] eyeVerticesR = renderer.getCubeVertices(eyeRight, eyeSize, 0);
        renderer.drawCubeShaded(eyeVerticesL, cam, new Color(255, 255, 255));
        renderer.drawCubeShaded(eyeVerticesR, cam, new Color(255, 255, 255));
        
        // Draw pupils (small black cubes)
        Vector3 pupilLeft = new Vector3(eyeLeft.x, eyeLeft.y, eyeLeft.z - eyeSize * 0.5);
        Vector3 pupilRight = new Vector3(eyeRight.x, eyeRight.y, eyeRight.z - eyeSize * 0.5);
        int pupilSize = (int)(eyeSize * 0.5);
        Vector3[] pupilVerticesL = renderer.getCubeVertices(pupilLeft, pupilSize, 0);
        Vector3[] pupilVerticesR = renderer.getCubeVertices(pupilRight, pupilSize, 0);
        renderer.drawCubeShaded(pupilVerticesL, cam, new Color(0, 0, 0));
        renderer.drawCubeShaded(pupilVerticesR, cam, new Color(0, 0, 0));
    }
}
