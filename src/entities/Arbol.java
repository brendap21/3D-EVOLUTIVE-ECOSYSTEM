package entities;

import main.Renderable;
import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Árbol: Entidad ambiental compuesta por tronco voxelizado + copa esférica.
 * Optimizado para performance: estructura compacta.
 * Animación: Pequeño balanceo lateral del follaje.
 */
public class Arbol implements Renderable {
    private Vector3 posicion;
    private List<Vector3> trunkVoxels;
    private List<Vector3> canopyVoxels;
    private int voxelSize;
    private Color trunkColor;
    private Color canopyColor;
    private double sway = 0.0;

    public Arbol(Vector3 posicion, int trunkRadius, int trunkHeight, int canopyRadius) {
        this.posicion = posicion;
        this.voxelSize = 12;
        this.trunkColor = new Color(85, 50, 25); // brown
        this.canopyColor = new Color(40, 120, 40); // forest green
        this.trunkVoxels = new ArrayList<>();
        this.canopyVoxels = new ArrayList<>();
        generateStructure(trunkRadius, trunkHeight, canopyRadius);
    }

    private void generateStructure(int trunkRadius, int trunkHeight, int canopyRadius) {
        // Trunk: simplified vertical cylinder of voxels
        int trunkVoxelRadius = Math.max(1, trunkRadius / voxelSize);
        int trunkVoxelHeight = Math.max(2, trunkHeight / voxelSize);
        
        for (int y = 0; y < trunkVoxelHeight; y++) {
            for (int x = -trunkVoxelRadius; x <= trunkVoxelRadius; x++) {
                for (int z = -trunkVoxelRadius; z <= trunkVoxelRadius; z++) {
                    if (x*x + z*z <= trunkVoxelRadius * trunkVoxelRadius) {
                        trunkVoxels.add(new Vector3(x, y, z));
                    }
                }
            }
        }
        
        // Canopy: simplified sphere of voxels on top of trunk
        int canopyVoxelRadius = Math.max(2, canopyRadius / voxelSize);
        int canopyStartY = trunkVoxelHeight;
        
        for (int x = -canopyVoxelRadius; x <= canopyVoxelRadius; x++) {
            for (int y = 0; y <= canopyVoxelRadius; y++) {
                for (int z = -canopyVoxelRadius; z <= canopyVoxelRadius; z++) {
                    double dist = Math.sqrt(x*x + y*y + z*z);
                    if (dist <= canopyVoxelRadius && dist > canopyVoxelRadius - 1.5) {
                        canopyVoxels.add(new Vector3(x, canopyStartY + y, z));
                    }
                }
            }
        }
    }

    @Override
    public void update() {
        sway += 0.008;
        if (sway > 2 * Math.PI) sway -= 2 * Math.PI;
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        // Draw trunk
        for (Vector3 voxel : trunkVoxels) {
            Vector3 worldPos = new Vector3(
                posicion.x + voxel.x * voxelSize,
                posicion.y + voxel.y * voxelSize,
                posicion.z + voxel.z * voxelSize
            );
            Vector3[] vertices = renderer.getCubeVertices(worldPos, voxelSize, 0);
            renderer.drawCubeShaded(vertices, cam, trunkColor);
        }

        // Draw canopy with sway
        double swayOffset = Math.sin(sway) * 2;
        for (Vector3 voxel : canopyVoxels) {
            Vector3 worldPos = new Vector3(
                posicion.x + voxel.x * voxelSize + swayOffset,
                posicion.y + voxel.y * voxelSize,
                posicion.z + voxel.z * voxelSize
            );
            Vector3[] vertices = renderer.getCubeVertices(worldPos, voxelSize, 0);
            renderer.drawCubeShaded(vertices, cam, canopyColor);
        }
    }
}
