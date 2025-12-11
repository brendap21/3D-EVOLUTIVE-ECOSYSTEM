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
 * Pasto: Entidad pequeña que simula hierba.
 * Múltiples tallos delgados para efecto de grupo de hierba.
 */
public class Pasto implements Renderable, Collidable {
    private Vector3 posicion;
    private List<Vector3> voxels;
    private int voxelSize;
    private Color[] colors;
    private long seed;
    private long creationTime; // Tiempo de creación para el crecimiento
    private double growthScale = 0.1; // Escala de crecimiento (0.1 a 1.0)
    private double wave = 0.0;

    public Pasto(Vector3 posicion) {
        this(posicion, System.currentTimeMillis());
    }

    public Pasto(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.creationTime = System.currentTimeMillis();
        this.voxelSize = 2; // muy delgado
        this.voxels = new ArrayList<>();
        
        Random r = new Random(seed);
        // Variación de colores verdes realistas
        int baseGreen = 80 + r.nextInt(60);
        colors = new Color[]{
            new Color(30 + r.nextInt(30), baseGreen, 20 + r.nextInt(20)),
            new Color(35 + r.nextInt(25), baseGreen + 10, 15 + r.nextInt(25)),
            new Color(25 + r.nextInt(35), baseGreen - 10, 25 + r.nextInt(20))
        };
        
        generateGrass();
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    private void generateGrass() {
        Random r = new Random(seed);
        // Crear 3-5 tallos delgados
        int numStems = 3 + r.nextInt(3);
        for (int s = 0; s < numStems; s++) {
            double offsetX = (r.nextDouble() - 0.5) * 3;
            double offsetZ = (r.nextDouble() - 0.5) * 3;
            int height = 3 + r.nextInt(3); // altura variable
            
            for (int h = 0; h < height; h++) {
                voxels.add(new Vector3(offsetX, h, offsetZ));
            }
        }
    }

    @Override
    public void update() {
        wave += 0.04;
        if (wave > 2 * Math.PI) wave -= 2 * Math.PI;
        
        // Actualizar crecimiento: después de 20 segundos alcanza tamaño completo (pasto más rápido)
        long elapsedTime = System.currentTimeMillis() - creationTime;
        double growthTime = 20.0; // 20 segundos para crecer completamente
        double elapsedSeconds = elapsedTime / 1000.0;
        growthScale = Math.min(1.0, 0.1 + (elapsedSeconds / growthTime) * 0.9);
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        Random r = new Random(seed);
        for (int i = 0; i < voxels.size(); i++) {
            Vector3 voxel = voxels.get(i);
            Vector3 scaledVoxel = new Vector3(
                voxel.x * growthScale,
                voxel.y * growthScale,
                voxel.z * growthScale
            );
            double waveOffset = Math.sin(wave + i * 0.6) * 1.2 * growthScale;
            Vector3 worldPos = new Vector3(
                posicion.x + scaledVoxel.x * voxelSize + waveOffset,
                posicion.y + scaledVoxel.y * voxelSize,
                posicion.z + scaledVoxel.z * voxelSize
            );
            Vector3[] vertices = renderer.getCubeVertices(worldPos, (int)(voxelSize * growthScale), 0);
            Color grassColor = colors[i % colors.length];
            renderer.drawCubeShaded(vertices, cam, grassColor);
        }
    }

    @Override
    public Vector3 getAABBMin() {
        // Small AABB for grass
        return new Vector3(posicion.x - 3, posicion.y, posicion.z - 3);
    }

    @Override
    public Vector3 getAABBMax() {
        // Max height based on typical grass height
        return new Vector3(posicion.x + 3, posicion.y + 12, posicion.z + 3);
    }
}
