package entities;

import main.Renderable;
import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Arbusto: Entidad ambiental pequeña y compacta.
 * Mucho más pequeño que un árbol, aspecto de arbusto bajo.
 */
public class Arbusto implements Renderable {
    private Vector3 posicion;
    private List<Vector3> foliageVoxels;
    private int voxelSize;
    private Color color;
    private double sway = 0.0;

    public Arbusto(Vector3 posicion) {
        this.posicion = posicion;
        this.voxelSize = 4; // voxel muy pequeño
        this.color = new Color(60, 95, 45); // verde oscuro realista
        this.foliageVoxels = new ArrayList<>();
        generateBush();
    }

    private void generateBush() {
        // Forma muy compacta: solo 5-7 voxels en patrón bajo
        // Arbusto esférico compacto (radio 1)
        foliageVoxels.add(new Vector3(0, 0, 0));   // Centro base
        foliageVoxels.add(new Vector3(1, 0, 0));   // Derecha
        foliageVoxels.add(new Vector3(-1, 0, 0));  // Izquierda
        foliageVoxels.add(new Vector3(0, 0, 1));   // Frente
        foliageVoxels.add(new Vector3(0, 0, -1));  // Atrás
        foliageVoxels.add(new Vector3(0, 1, 0));   // Arriba
    }

    @Override
    public void update() {
        sway += 0.02;
        if (sway > 2 * Math.PI) sway -= 2 * Math.PI;
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        double swayOffset = Math.sin(sway) * 0.8; // muy sutil
        
        for (Vector3 voxel : foliageVoxels) {
            Vector3 worldPos = new Vector3(
                posicion.x + voxel.x * voxelSize + swayOffset,
                posicion.y + voxel.y * voxelSize,
                posicion.z + voxel.z * voxelSize
            );
            Vector3[] vertices = renderer.getCubeVertices(worldPos, voxelSize, 0);
            renderer.drawCubeShaded(vertices, cam, color);
        }
    }
}
