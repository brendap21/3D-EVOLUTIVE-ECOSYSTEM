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
public class Arbol implements Renderable, Collidable {
    private Vector3 posicion;
    private List<Vector3> trunkVoxels;
    private List<Vector3> canopyVoxels;
    private int voxelSize;
    private Color trunkColor;
    private Color canopyColor;
    private double sway = 0.0;
    private long seed;
    
    // Growth system
    private long creationTime; // When the tree was created
    private int maxTrunkRadius;
    private int maxTrunkHeight;
    private int maxCanopyRadius;
    private static final long GROWTH_DURATION = 180000L; // 3 minutes to full growth in milliseconds
    private double growthProgress = 0.0; // 0.0 to 1.0

    public Arbol(Vector3 posicion, int trunkRadius, int trunkHeight, int canopyRadius) {
        this(posicion, trunkRadius, trunkHeight, canopyRadius, System.currentTimeMillis());
    }

    public Arbol(Vector3 posicion, int trunkRadius, int trunkHeight, int canopyRadius, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxelSize = 6; // más pequeño para más detalle
        this.trunkVoxels = new ArrayList<>();
        this.canopyVoxels = new ArrayList<>();
        this.creationTime = System.currentTimeMillis();
        this.maxTrunkRadius = trunkRadius;
        this.maxTrunkHeight = trunkHeight;
        this.maxCanopyRadius = canopyRadius;
        
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
        
        // Generar con tamaño inicial de 50% (mitad del tamaño)
        generateStructure((int)(trunkRadius * 0.5), (int)(trunkHeight * 0.5), (int)(canopyRadius * 0.5));
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
        // Actualizar progreso de crecimiento (de 50% a 100%)
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - creationTime;
        double rawProgress = Math.min(1.0, (double) elapsedTime / GROWTH_DURATION);
        
        // El progreso va de 0.5 (50%) a 1.0 (100%)
        growthProgress = 0.5 + (rawProgress * 0.5);
        
        // Regenerar la estructura si el árbol aún está creciendo
        if (growthProgress < 1.0) {
            int currentTrunkRadius = (int)(maxTrunkRadius * growthProgress);
            int currentTrunkHeight = (int)(maxTrunkHeight * growthProgress);
            int currentCanopyRadius = (int)(maxCanopyRadius * growthProgress);
            
            // Asegurar que los valores sean al menos 1
            currentTrunkRadius = Math.max(1, currentTrunkRadius);
            currentTrunkHeight = Math.max(1, currentTrunkHeight);
            currentCanopyRadius = Math.max(1, currentCanopyRadius);
            
            trunkVoxels.clear();
            canopyVoxels.clear();
            generateStructure(currentTrunkRadius, currentTrunkHeight, currentCanopyRadius);
        }
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        // Tronco sin movimiento
        for (Vector3 voxel : trunkVoxels) {
            Vector3 worldPos = new Vector3(
                posicion.x + voxel.x * voxelSize,
                posicion.y + voxel.y * voxelSize,
                posicion.z + voxel.z * voxelSize
            );
            Vector3[] vertices = renderer.getCubeVertices(worldPos, voxelSize, 0);
            renderer.drawCubeShaded(vertices, cam, trunkColor);
        }

        // Copa sin movimiento
        for (Vector3 voxel : canopyVoxels) {
            Vector3 worldPos = new Vector3(
                posicion.x + voxel.x * voxelSize,
                posicion.y + voxel.y * voxelSize,
                posicion.z + voxel.z * voxelSize
            );
            Vector3[] vertices = renderer.getCubeVertices(worldPos, voxelSize, 0);
            renderer.drawCubeShaded(vertices, cam, canopyColor);
        }
    }

    @Override
    public Vector3 getAABBMin() {
        // Calculate min based on trunk and canopy voxels
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        
        for (Vector3 v : trunkVoxels) {
            minX = Math.min(minX, posicion.x + v.x * voxelSize - voxelSize/2.0);
            minY = Math.min(minY, posicion.y + v.y * voxelSize - voxelSize/2.0);
            minZ = Math.min(minZ, posicion.z + v.z * voxelSize - voxelSize/2.0);
        }
        for (Vector3 v : canopyVoxels) {
            minX = Math.min(minX, posicion.x + v.x * voxelSize - voxelSize/2.0);
            minY = Math.min(minY, posicion.y + v.y * voxelSize - voxelSize/2.0);
            minZ = Math.min(minZ, posicion.z + v.z * voxelSize - voxelSize/2.0);
        }
        
        return new Vector3(minX, minY, minZ);
    }

    @Override
    public Vector3 getAABBMax() {
        // Calculate max based on trunk and canopy voxels
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        
        for (Vector3 v : trunkVoxels) {
            maxX = Math.max(maxX, posicion.x + v.x * voxelSize + voxelSize/2.0);
            maxY = Math.max(maxY, posicion.y + v.y * voxelSize + voxelSize/2.0);
            maxZ = Math.max(maxZ, posicion.z + v.z * voxelSize + voxelSize/2.0);
        }
        for (Vector3 v : canopyVoxels) {
            maxX = Math.max(maxX, posicion.x + v.x * voxelSize + voxelSize/2.0);
            maxY = Math.max(maxY, posicion.y + v.y * voxelSize + voxelSize/2.0);
            maxZ = Math.max(maxZ, posicion.z + v.z * voxelSize + voxelSize/2.0);
        }
        
        return new Vector3(maxX, maxY, maxZ);
    }
}
