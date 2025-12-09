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
public class Arbusto implements Renderable, Collidable {
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
        // Animación deshabilitada
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        for (Vector3 voxel : foliageVoxels) {
            Vector3 worldPos = new Vector3(
                posicion.x + voxel.x * voxelSize,
                posicion.y + voxel.y * voxelSize,
                posicion.z + voxel.z * voxelSize
            );
            Vector3[] vertices = renderer.getCubeVertices(worldPos, voxelSize, 0);
            renderer.drawCubeShaded(vertices, cam, color);
        }
    }

    @Override
    public Vector3 getAABBMin() {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        
        for (Vector3 v : foliageVoxels) {
            minX = Math.min(minX, posicion.x + v.x * voxelSize - voxelSize/2.0);
            minY = Math.min(minY, posicion.y + v.y * voxelSize - voxelSize/2.0);
            minZ = Math.min(minZ, posicion.z + v.z * voxelSize - voxelSize/2.0);
        }
        
        return new Vector3(minX, minY, minZ);
    }

    @Override
    public Vector3 getAABBMax() {
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        
        for (Vector3 v : foliageVoxels) {
            maxX = Math.max(maxX, posicion.x + v.x * voxelSize + voxelSize/2.0);
            maxY = Math.max(maxY, posicion.y + v.y * voxelSize + voxelSize/2.0);
            maxZ = Math.max(maxZ, posicion.z + v.z * voxelSize + voxelSize/2.0);
        }
        
        return new Vector3(maxX, maxY, maxZ);
    }
}
