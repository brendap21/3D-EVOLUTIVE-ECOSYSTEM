package entities;

import main.Renderable;
import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Piedra: Roca ambiental con variación de tamaño y color.
 */
public class Piedra implements Renderable, Collidable {
    private Vector3 posicion;
    private List<Vector3> voxels;
    private int voxelSize;
    private Color color;
    private double rotY = 0.0;
    private double pulse = 0.0;
    private long seed;

    public Piedra(Vector3 posicion) {
        this(posicion, System.currentTimeMillis());
    }

    public Piedra(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxels = new ArrayList<>();
        
        Random r = new Random(seed);
        // Tamaño variable: 8-16 píxeles
        this.voxelSize = 8 + r.nextInt(9);
        
        // Colores realistas de piedra/roca
        int[] stoneColors = {
            0x8B8680, // tan
            0x696969, // dim gray
            0x808080, // gray
            0x7F7F7F, // web gray
            0x928E85, // warm gray
            0x9C9C9C, // light gray
            0x6B6B47, // olive gray
        };
        int colorIdx = r.nextInt(stoneColors.length);
        int rgb = stoneColors[colorIdx];
        int rc = (rgb >> 16) & 0xFF;
        int gc = (rgb >> 8) & 0xFF;
        int bc = rgb & 0xFF;
        // Añadir variación
        rc = Math.max(0, Math.min(255, rc + r.nextInt(40) - 20));
        gc = Math.max(0, Math.min(255, gc + r.nextInt(40) - 20));
        bc = Math.max(0, Math.min(255, bc + r.nextInt(40) - 20));
        this.color = new Color(rc, gc, bc);
        
        generateRock();
    }

    private void generateRock() {
        // Forma irregular de piedra: 5-8 voxels
        Random r = new Random(seed);
        voxels.add(new Vector3(0, 0, 0));
        
        int numExtraVoxels = 3 + r.nextInt(4);
        for (int i = 0; i < numExtraVoxels; i++) {
            int dx = r.nextInt(3) - 1;
            int dy = r.nextInt(2);
            int dz = r.nextInt(3) - 1;
            if (dx != 0 || dy != 0 || dz != 0) {
                voxels.add(new Vector3(dx, dy, dz));
            }
        }
    }

    @Override
    public void update() {
        // Animación deshabilitada
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        for (Vector3 voxel : voxels) {
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
        
        for (Vector3 v : voxels) {
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
        
        for (Vector3 v : voxels) {
            maxX = Math.max(maxX, posicion.x + v.x * voxelSize + voxelSize/2.0);
            maxY = Math.max(maxY, posicion.y + v.y * voxelSize + voxelSize/2.0);
            maxZ = Math.max(maxZ, posicion.z + v.z * voxelSize + voxelSize/2.0);
        }
        
        return new Vector3(maxX, maxY, maxZ);
    }
}
