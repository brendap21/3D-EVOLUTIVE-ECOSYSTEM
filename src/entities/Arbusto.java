package entities;

import main.Renderable;
import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Arbusto: Entidad ambiental, versión compacta y redondeada del árbol.
 * Animación: Oscilación lateral leve del follaje.
 */
public class Arbusto implements Renderable {
    private Vector3 posicion;
    private List<Vector3> foliageVoxels;
    private int voxelSize;
    private Color color;
    private double sway = 0.0;

    public Arbusto(Vector3 posicion) {
        this.posicion = posicion;
        this.voxelSize = 12;
        this.color = new Color(85, 107, 47); // dark yellow-green (olive)
        this.foliageVoxels = new ArrayList<>();
        generateBush();
    }

    private void generateBush() {
        // Compact spherical shape: fewer voxels than tree
        int r = 2;
        for (int x = -r; x <= r; x++) {
            for (int y = 0; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    double dist = Math.sqrt(x*x + y*y + z*z);
                    if (dist <= r && dist > r - 1.2) {
                        foliageVoxels.add(new Vector3(x, y, z));
                    }
                }
            }
        }
    }

    @Override
    public void update() {
        sway += 0.02;
        if (sway > 2 * Math.PI) sway -= 2 * Math.PI;
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        Vector3 center = new Vector3(
            posicion.x + Math.sin(sway) * 2,
            posicion.y,
            posicion.z + Math.cos(sway) * 2
        );
        
        for (Vector3 voxel : foliageVoxels) {
            Vector3 worldPos = new Vector3(
                center.x + voxel.x * voxelSize,
                center.y + voxel.y * voxelSize,
                center.z + voxel.z * voxelSize
            );
            Vector3[] vertices = renderer.getCubeVertices(worldPos, voxelSize, 0);
            renderer.drawCube(vertices, cam, color);
        }
    }
}
