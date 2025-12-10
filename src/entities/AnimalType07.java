package entities;

import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * AnimalType07: Criatura insectoíde con mandíbulas y múltiples patas.
 * Paleta fase 1 (3 colores): verde bosque, esmeralda, verde musgo.
 * Fase 2-3 más segmentos, mandíbulas más grandes.
 */
public class AnimalType07 extends BaseAnimal {
    private double walkPhase = 0.0;

    public AnimalType07(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxels = new ArrayList<>();
        generateFromSeed(seed);
        initializeSpawnAnimation();
    }

    private void generateFromSeed(long seed) {
        Random r = new Random(seed);
        this.voxelSize = 3 + r.nextInt(2);
        this.baseVoxelSize = voxelSize;
        Color[] palette = new Color[]{
            new Color(40, 100, 50),
            new Color(50, 180, 80),
            new Color(60, 120, 60)
        };
        this.color = palette[r.nextInt(palette.length)];
        this.originalColor = color;
        this.baseSpeed = 1.3 + r.nextDouble() * 0.5;

        // Cuerpo segmentado
        voxels.add(new Vector3(0, 0, 0));
        voxels.add(new Vector3(0, 0, 1));
        voxels.add(new Vector3(0, 0, -1));

        // Cabeza
        voxels.add(new Vector3(0, 0, 2));

        // Patas (6 patas insectoídeas)
        voxels.add(new Vector3(-1, -1, 1));
        voxels.add(new Vector3(1, -1, 1));
        voxels.add(new Vector3(-1, -1, 0));
        voxels.add(new Vector3(1, -1, 0));
        voxels.add(new Vector3(-1, -1, -1));
        voxels.add(new Vector3(1, -1, -1));
    }

    @Override
    protected void renderNormal(SoftwareRenderer renderer, Camera cam) {
        walkPhase += 0.18 * getPhaseSpeedMultiplier();
        Color body = applyGlowToColor(originalColor);

        // Cuerpo base
        for (Vector3 v : voxels) {
            Vector3 wp = applyTransform(new Vector3(v.x * voxelSize, v.y * voxelSize, v.z * voxelSize));
            wp = applyScaleToPosition(wp);
            int s = applyScaleToSize(voxelSize);
            renderer.drawCubeShaded(renderer.getCubeVertices(wp, s, 0), cam, body);
        }

        // Mandíbulas (más grandes en fases avanzadas)
        int mandibleSize = applyScaleToSize((int)(voxelSize * (0.6 + growthPhase * 0.2)));
        double mandOpen = Math.sin(walkPhase * 2) * voxelSize * 0.3;
        Vector3 mandL = applyTransform(new Vector3(-voxelSize * 0.6 - mandOpen, -voxelSize * 0.2, voxelSize * 2.5));
        mandL = applyScaleToPosition(mandL);
        Vector3 mandR = applyTransform(new Vector3(voxelSize * 0.6 + mandOpen, -voxelSize * 0.2, voxelSize * 2.5));
        mandR = applyScaleToPosition(mandR);
        renderer.drawCubeShaded(renderer.getCubeVertices(mandL, mandibleSize, 0), cam, body.darker());
        renderer.drawCubeShaded(renderer.getCubeVertices(mandR, mandibleSize, 0), cam, body.darker());

        // Ojos compuestos
        int eyeSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.35)));
        Color eyeColor = new Color(200, 50, 50);
        Vector3 eyeL = applyTransform(new Vector3(-voxelSize * 0.6, voxelSize * 0.3, voxelSize * 2.2));
        eyeL = applyScaleToPosition(eyeL);
        Vector3 eyeR = applyTransform(new Vector3(voxelSize * 0.6, voxelSize * 0.3, voxelSize * 2.2));
        eyeR = applyScaleToPosition(eyeR);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeL, eyeSize, 0), cam, eyeColor);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeR, eyeSize, 0), cam, eyeColor);

        // Antenas
        int antennaLen = 1 + growthPhase;
        int antennaSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.3)));
        for (int i = 0; i < antennaLen; i++) {
            double bend = Math.sin(walkPhase + i * 0.7) * voxelSize * 0.2;
            Vector3 antL = applyTransform(new Vector3(-voxelSize * 0.4 + bend, voxelSize * (0.8 + i * 0.6), voxelSize * 2.3));
            antL = applyScaleToPosition(antL);
            Vector3 antR = applyTransform(new Vector3(voxelSize * 0.4 - bend, voxelSize * (0.8 + i * 0.6), voxelSize * 2.3));
            antR = applyScaleToPosition(antR);
            renderer.drawCubeShaded(renderer.getCubeVertices(antL, antennaSize, 0), cam, body.brighter());
            renderer.drawCubeShaded(renderer.getCubeVertices(antR, antennaSize, 0), cam, body.brighter());
        }

        // 6 patas con movimiento alternado tipo insecto
        int legSize = applyScaleToSize((int)(voxelSize * 0.7));
        Color paw = body.darker();
        
        // Tripod gait - 3 patas en contacto siempre
        double leg1 = Math.sin(walkPhase) * voxelSize * 0.3;
        double leg2 = Math.sin(walkPhase + Math.PI) * voxelSize * 0.3;
        
        Vector3 l1 = applyTransform(new Vector3(-voxelSize * 1.2, -voxelSize * 0.5 + Math.abs(leg1) * 0.2, voxelSize + leg1 * 0.5));
        l1 = applyScaleToPosition(l1);
        renderer.drawCubeShaded(renderer.getCubeVertices(l1, legSize, 0), cam, paw);
        Vector3 l2 = applyTransform(new Vector3(voxelSize * 1.2, -voxelSize * 0.5 + Math.abs(leg2) * 0.2, voxelSize + leg2 * 0.5));
        l2 = applyScaleToPosition(l2);
        renderer.drawCubeShaded(renderer.getCubeVertices(l2, legSize, 0), cam, paw);
        Vector3 l3 = applyTransform(new Vector3(-voxelSize * 1.2, -voxelSize * 0.5 + Math.abs(leg2) * 0.2, leg2 * 0.5));
        l3 = applyScaleToPosition(l3);
        renderer.drawCubeShaded(renderer.getCubeVertices(l3, legSize, 0), cam, paw);
        Vector3 l4 = applyTransform(new Vector3(voxelSize * 1.2, -voxelSize * 0.5 + Math.abs(leg1) * 0.2, leg1 * 0.5));
        l4 = applyScaleToPosition(l4);
        renderer.drawCubeShaded(renderer.getCubeVertices(l4, legSize, 0), cam, paw);
        Vector3 l5 = applyTransform(new Vector3(-voxelSize * 1.2, -voxelSize * 0.5 + Math.abs(leg1) * 0.2, -voxelSize + leg1 * 0.5));
        l5 = applyScaleToPosition(l5);
        renderer.drawCubeShaded(renderer.getCubeVertices(l5, legSize, 0), cam, paw);
        Vector3 l6 = applyTransform(new Vector3(voxelSize * 1.2, -voxelSize * 0.5 + Math.abs(leg2) * 0.2, -voxelSize + leg2 * 0.5));
        l6 = applyScaleToPosition(l6);
        renderer.drawCubeShaded(renderer.getCubeVertices(l6, legSize, 0), cam, paw);
    }

    @Override
    public String getSpeciesName() {
        return "Escarabajo Esmeralda";
    }
}
