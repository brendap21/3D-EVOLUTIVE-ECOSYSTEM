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
 * Flor: Entidad decorativa pequeña con variación de tamaño y color.
 */
public class Flor implements Renderable, Collidable {
    private Vector3 posicion;
    private List<Vector3> petalos;
    private Vector3 centro;
    private int voxelSize;
    private Color colorPetalo;
    private Color colorCentro;
    private double rotY = 0.0;
    private long seed;

    public Flor(Vector3 posicion, Color colorPetalo) {
        this(posicion, colorPetalo, System.currentTimeMillis());
    }

    public Flor(Vector3 posicion, Color colorPetalo, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.colorPetalo = colorPetalo;
        this.colorCentro = new Color(255, 220, 80); // amarillo cálido
        this.centro = new Vector3(0, 1, 0);
        this.petalos = new ArrayList<>();
        
        Random r = new Random(seed);
        // Tamaño variable: 1-3 píxeles (flores MÁS PEQUEÑAS - reducido a la mitad)
        this.voxelSize = 1 + r.nextInt(3);
        
        generateFlower();
    }

    private void generateFlower() {
        Random r = new Random(seed);
        // 4 pétalos en cruz
        petalos.add(new Vector3(1.5, 0.5, 0));
        petalos.add(new Vector3(-1.5, 0.5, 0));
        petalos.add(new Vector3(0, 0.5, 1.5));
        petalos.add(new Vector3(0, 0.5, -1.5));
        
        // Añadir 1-2 pétalos diagonales
        if (r.nextDouble() < 0.6) {
            petalos.add(new Vector3(1, 0.5, 1));
            petalos.add(new Vector3(-1, 0.5, -1));
        }
    }

    @Override
    public void update() {
        // Animación deshabilitada
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        // Centro sin rotación
        Vector3 centerPos = new Vector3(posicion.x, posicion.y + centro.y * voxelSize, posicion.z);
        Vector3[] centerVerts = renderer.getCubeVertices(centerPos, voxelSize + 1, 0);
        renderer.drawCubeShaded(centerVerts, cam, colorCentro);

        // Pétalos sin rotación
        for (Vector3 petalo : petalos) {
            Vector3 worldPos = new Vector3(
                posicion.x + petalo.x * voxelSize,
                posicion.y + petalo.y * voxelSize,
                posicion.z + petalo.z * voxelSize
            );
            Vector3[] vertices = renderer.getCubeVertices(worldPos, voxelSize, 0);
            renderer.drawCubeShaded(vertices, cam, colorPetalo);
        }
    }

    @Override
    public Vector3 getAABBMin() {
        double minX = posicion.x - 2 * voxelSize;
        double minY = posicion.y;
        double minZ = posicion.z - 2 * voxelSize;
        return new Vector3(minX, minY, minZ);
    }

    @Override
    public Vector3 getAABBMax() {
        double maxX = posicion.x + 2 * voxelSize;
        double maxY = posicion.y + 2 * voxelSize;
        double maxZ = posicion.z + 2 * voxelSize;
        return new Vector3(maxX, maxY, maxZ);
    }
}
