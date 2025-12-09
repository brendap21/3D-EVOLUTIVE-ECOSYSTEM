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
public class Pasto implements Renderable {
    private Vector3 posicion;
    private List<Vector3> voxels;
    private int voxelSize;
    private Color[] colors;
    private double wave = 0.0;
    private long seed;

    public Pasto(Vector3 posicion) {
        this(posicion, System.currentTimeMillis());
    }

    public Pasto(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
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
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        Random r = new Random(seed);
        for (int i = 0; i < voxels.size(); i++) {
            Vector3 voxel = voxels.get(i);
            double waveOffset = Math.sin(wave + i * 0.6) * 1.2;
            Vector3 worldPos = new Vector3(
                posicion.x + voxel.x * voxelSize + waveOffset,
                posicion.y + voxel.y * voxelSize,
                posicion.z + voxel.z * voxelSize
            );
            Vector3[] vertices = renderer.getCubeVertices(worldPos, voxelSize, 0);
            Color grassColor = colors[i % colors.length];
            renderer.drawCubeShaded(vertices, cam, grassColor);
        }
    }
}
