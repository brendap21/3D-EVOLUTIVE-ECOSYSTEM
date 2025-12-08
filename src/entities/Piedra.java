package entities;

import main.Renderable;
import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Piedra: Entidad ambiental compuesta por voxels irregulares.
 * Animación: Rotación lenta y/o pulsación de escala.
 */
public class Piedra implements Renderable {
    private Vector3 posicion;
    private List<Vector3> voxels;
    private int voxelSize;
    private Color color;
    private double rotY = 0.0;
    private double pulse = 0.0;

    public Piedra(Vector3 posicion) {
        this.posicion = posicion;
        this.voxelSize = 20;
        this.voxels = new ArrayList<>();
        this.color = new Color(128, 128, 128); // gray
        generateRock();
    }

    private void generateRock() {
        // Minimal rock shape: just core voxels, no extras
        voxels.add(new Vector3(0, 0, 0));
        voxels.add(new Vector3(1, 0, 0));
        voxels.add(new Vector3(-1, 0, 0));
        voxels.add(new Vector3(0, 0, 1));
        voxels.add(new Vector3(0, 1, 0));
        voxels.add(new Vector3(0, 0, -1));
    }

    @Override
    public void update() {
        rotY += 0.002;
        if (rotY > 2 * Math.PI) rotY -= 2 * Math.PI;
        pulse += 0.015;
        if (pulse > 2 * Math.PI) pulse -= 2 * Math.PI;
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        double scale = 1.0 + Math.sin(pulse) * 0.1; // subtle pulse
        for (Vector3 voxel : voxels) {
            Vector3 worldPos = new Vector3(
                posicion.x + voxel.x * voxelSize * scale,
                posicion.y + voxel.y * voxelSize,
                posicion.z + voxel.z * voxelSize * scale
            );
            int scaledSize = (int)(voxelSize * scale);
            Vector3[] vertices = renderer.getCubeVertices(worldPos, scaledSize, rotY);
            // Vary rock colors slightly for rocky texture
            Color rockyColor = new Color(
                Math.max(0, Math.min(255, color.getRed() + (int)(Math.sin(voxel.x) * 30))),
                Math.max(0, Math.min(255, color.getGreen() + (int)(Math.sin(voxel.z) * 30))),
                Math.max(0, Math.min(255, color.getBlue() + (int)(Math.sin(voxel.y) * 30)))
            );
            renderer.drawCubeShaded(vertices, cam, rockyColor);
        }
    }
}
