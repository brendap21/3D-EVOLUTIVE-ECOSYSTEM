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
 * Árbol: Entidad ambiental con tronco y copa.
 * Variación de tamaño realista.
 */
public class Arbol implements Renderable {
    private Vector3 posicion;
    private List<Vector3> trunkVoxels;
    private List<Vector3> canopyVoxels;
    private int voxelSize;
    private Color trunkColor;
    private Color canopyColor;
    private double sway = 0.0;
    private long seed;

    public Arbol(Vector3 posicion, int trunkRadius, int trunkHeight, int canopyRadius) {
        this(posicion, trunkRadius, trunkHeight, canopyRadius, System.currentTimeMillis());
    }

    public Arbol(Vector3 posicion, int trunkRadius, int trunkHeight, int canopyRadius, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxelSize = 6; // más pequeño para más detalle
        this.trunkVoxels = new ArrayList<>();
        this.canopyVoxels = new ArrayList<>();
        
        Random r = new Random(seed);
        // Colores realistas
        int trunkRed = 100 + r.nextInt(50);
        int trunkGreen = 60 + r.nextInt(40);
        int trunkBlue = 20 + r.nextInt(30);
        this.trunkColor = new Color(trunkRed, trunkGreen, trunkBlue);
        
        int canopyGreen = 80 + r.nextInt(60);
        int canopyRed = 40 + r.nextInt(30);
        int canopyBlue = 30 + r.nextInt(30);
        this.canopyColor = new Color(canopyRed, canopyGreen, canopyBlue);
        
        generateStructure(trunkRadius, trunkHeight, canopyRadius);
    }

    private void generateStructure(int trunkRadius, int trunkHeight, int canopyRadius) {
        Random r = new Random(seed);
        int trunkVoxelRadius = Math.max(1, trunkRadius / voxelSize);
        int trunkVoxelHeight = Math.max(2, trunkHeight / voxelSize);
        
        // Tronco cilíndrico
        for (int y = 0; y < trunkVoxelHeight; y++) {
            for (int x = -trunkVoxelRadius; x <= trunkVoxelRadius; x++) {
                for (int z = -trunkVoxelRadius; z <= trunkVoxelRadius; z++) {
                    if (x*x + z*z <= trunkVoxelRadius * trunkVoxelRadius) {
                        trunkVoxels.add(new Vector3(x, y, z));
                    }
                }
            }
        }
        
        // Copa esférica
        int canopyVoxelRadius = Math.max(2, canopyRadius / voxelSize);
        int canopyStartY = trunkVoxelHeight;
        
        for (int x = -canopyVoxelRadius; x <= canopyVoxelRadius; x++) {
            for (int y = 0; y <= canopyVoxelRadius; y++) {
                for (int z = -canopyVoxelRadius; z <= canopyVoxelRadius; z++) {
                    double dist = Math.sqrt(x*x + y*y + z*z);
                    if (dist <= canopyVoxelRadius && dist > canopyVoxelRadius - 1.8) {
                        canopyVoxels.add(new Vector3(x, canopyStartY + y, z));
                    }
                }
            }
        }
    }

    @Override
    public void update() {
        sway += 0.006;
        if (sway > 2 * Math.PI) sway -= 2 * Math.PI;
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        // Tronco
        for (Vector3 voxel : trunkVoxels) {
            Vector3 worldPos = new Vector3(
                posicion.x + voxel.x * voxelSize,
                posicion.y + voxel.y * voxelSize,
                posicion.z + voxel.z * voxelSize
            );
            Vector3[] vertices = renderer.getCubeVertices(worldPos, voxelSize, 0);
            renderer.drawCubeShaded(vertices, cam, trunkColor);
        }

        // Copa con movimiento
        double swayOffset = Math.sin(sway) * 2.5;
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
