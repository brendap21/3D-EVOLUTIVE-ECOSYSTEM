package entities;

import main.Renderable;
import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Flor: Pequeña entidad ornamental decorativa.
 * Animación: Rotación vertical suave (como una flor mecánica).
 */
public class Flor implements Renderable {
    private Vector3 posicion;
    private List<Vector3> petalos;
    private Vector3 centro;
    private int voxelSize;
    private Color colorPetalo;
    private Color colorCentro;
    private double rotY = 0.0;

    public Flor(Vector3 posicion, Color colorPetalo) {
        this.posicion = posicion;
        this.voxelSize = 6;
        this.colorPetalo = colorPetalo;
        this.colorCentro = new Color(255, 200, 0); // yellow center
        this.centro = new Vector3(0, 1, 0);
        this.petalos = new ArrayList<>();
        generateFlower();
    }

    private void generateFlower() {
        // 4-6 petals around center
        petalos.add(new Vector3(1, 1, 0));
        petalos.add(new Vector3(-1, 1, 0));
        petalos.add(new Vector3(0, 1, 1));
        petalos.add(new Vector3(0, 1, -1));
    }

    @Override
    public void update() {
        rotY += 0.01;
        if (rotY > 2 * Math.PI) rotY -= 2 * Math.PI;
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        // Draw center
        Vector3 centerPos = new Vector3(posicion.x, posicion.y + centro.y * voxelSize, posicion.z);
        Vector3[] centerVerts = renderer.getCubeVertices(centerPos, voxelSize + 2, rotY);
        renderer.drawCube(centerVerts, cam, colorCentro);

        // Draw petals
        for (Vector3 petalo : petalos) {
            Vector3 worldPos = new Vector3(
                posicion.x + petalo.x * voxelSize * 2,
                posicion.y + petalo.y * voxelSize,
                posicion.z + petalo.z * voxelSize * 2
            );
            Vector3[] vertices = renderer.getCubeVertices(worldPos, voxelSize, rotY);
            renderer.drawCube(vertices, cam, colorPetalo);
        }
    }
}
