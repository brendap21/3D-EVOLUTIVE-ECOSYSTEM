package entities;

import main.Renderable;
import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Pasto: Entidad ambiental pequeña, hecha de voxels alargados.
 * Animación: Ondulación tipo onda en el viento.
 */
public class Pasto implements Renderable {
    private Vector3 posicion;
    private List<Vector3> voxels;
    private int voxelSize;
    private Color color;
    private double wave = 0.0;

    public Pasto(Vector3 posicion) {
        this.posicion = posicion;
        this.voxelSize = 8;
        this.voxels = new ArrayList<>();
        this.color = new Color(34, 139, 34); // forest green
        generateGrass();
    }

    private void generateGrass() {
        // Tall thin grass: vertical voxel stack
        voxels.add(new Vector3(0, 0, 0));
        voxels.add(new Vector3(0, 1, 0));
        voxels.add(new Vector3(0, 2, 0));
        voxels.add(new Vector3(0, 3, 0));
    }

    @Override
    public void update() {
        wave += 0.03;
        if (wave > 2 * Math.PI) wave -= 2 * Math.PI;
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        for (int i = 0; i < voxels.size(); i++) {
            Vector3 voxel = voxels.get(i);
            double waveOffset = Math.sin(wave + i * 0.4) * 2;
            Vector3 worldPos = new Vector3(
                posicion.x + voxel.x * voxelSize + waveOffset,
                posicion.y + voxel.y * voxelSize,
                posicion.z + voxel.z * voxelSize
            );
            Vector3[] vertices = renderer.getCubeVertices(worldPos, voxelSize, 0);
            // Vary grass color for natural look
            Color grassColor = new Color(
                Math.max(0, color.getRed() - (int)(i * 5)),
                Math.max(80, color.getGreen() - (int)(i * 3)),
                Math.max(0, color.getBlue() - (int)(i * 5))
            );
            renderer.drawCubeShaded(vertices, cam, grassColor);
        }
    }
}
