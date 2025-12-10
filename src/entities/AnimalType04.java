package entities;

import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * AnimalType04: Criatura serpentina alargada con aletas laterales.
 * Paleta fase 1 (3 colores): amarillo lima, verde limón, chartreuse.
 * Fase 2-3 se alarga más, movimiento ondulante pronunciado.
 */
public class AnimalType04 extends BaseAnimal {
    private double undulatePhase = 0.0;

    public AnimalType04(Vector3 posicion, long seed) {
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
            new Color(180, 200, 60),
            new Color(150, 220, 70),
            new Color(190, 210, 50)
        };
        this.color = palette[r.nextInt(palette.length)];
        this.originalColor = color;
        this.baseSpeed = 1.4 + r.nextDouble() * 0.5;

        // Cuerpo alargado (segmentos)
        for (int i = -2; i <= 2; i++) {
            voxels.add(new Vector3(0, 0, i));
        }
    }

    @Override
    protected void renderNormal(SoftwareRenderer renderer, Camera cam) {
        undulatePhase += 0.2 * getPhaseSpeedMultiplier();
        Color body = applyGlowToColor(color);

        // Segmentos del cuerpo con ondulación (más segmentos en fases avanzadas)
        int segments = 5 + growthPhase * 2;
        for (int i = 0; i < segments; i++) {
            double offset = Math.sin(undulatePhase + i * 0.5) * voxelSize * 0.6;
            Vector3 wp = applyTransform(new Vector3(
                offset,
                Math.sin(undulatePhase + i * 0.3) * voxelSize * 0.3,
                -voxelSize * 2 + i * voxelSize * 0.8
            ));
            wp = applyScaleToPosition(wp);
            int s = applyScaleToSize(voxelSize);
            renderer.drawCubeShaded(renderer.getCubeVertices(wp, s, 0), cam, body);
        }

        // Cabeza (primer segmento)
        Vector3 headPos = applyTransform(new Vector3(0, 0, voxelSize * 2));
        headPos = applyScaleToPosition(headPos);
        int headSize = applyScaleToSize((int)(voxelSize * 1.2));
        renderer.drawCubeShaded(renderer.getCubeVertices(headPos, headSize, 0), cam, body.brighter());

        // Ojos en la cabeza
        int eyeSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.35)));
        Color eyeW = new Color(255, 255, 200);
        Color pupil = new Color(50, 50, 20);
        Vector3 eyeL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 0.3, voxelSize * 2.3));
        eyeL = applyScaleToPosition(eyeL);
        Vector3 eyeR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 0.3, voxelSize * 2.3));
        eyeR = applyScaleToPosition(eyeR);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeL, eyeSize, 0), cam, eyeW);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeR, eyeSize, 0), cam, eyeW);
        int pup = Math.max(1, eyeSize / 2);
        Vector3 pupilL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 0.3, voxelSize * 2.3 + pup));
        pupilL = applyScaleToPosition(pupilL);
        Vector3 pupilR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 0.3, voxelSize * 2.3 + pup));
        pupilR = applyScaleToPosition(pupilR);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilL, pup, 0), cam, pupil);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilR, pup, 0), cam, pupil);

        // Boca
        int mouthSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.3)));
        Vector3 mouth = applyTransform(new Vector3(0, -voxelSize * 0.2, voxelSize * 2.5));
        mouth = applyScaleToPosition(mouth);
        renderer.drawCubeShaded(renderer.getCubeVertices(mouth, mouthSize, 0), cam, new Color(160, 80, 40));

        // Aletas laterales (ondulantes)
        int finCount = 3 + growthPhase;
        int finSize = applyScaleToSize((int)(voxelSize * 0.6));
        for (int i = 0; i < finCount; i++) {
            double finWave = Math.sin(undulatePhase + i * 0.8) * voxelSize * 0.4;
            Vector3 finL = applyTransform(new Vector3(-voxelSize * 1.2 + finWave, 0, -voxelSize + i * voxelSize * 0.9));
            finL = applyScaleToPosition(finL);
            Vector3 finR = applyTransform(new Vector3(voxelSize * 1.2 - finWave, 0, -voxelSize + i * voxelSize * 0.9));
            finR = applyScaleToPosition(finR);
            renderer.drawCubeShaded(renderer.getCubeVertices(finL, finSize, 0), cam, body.darker());
            renderer.drawCubeShaded(renderer.getCubeVertices(finR, finSize, 0), cam, body.darker());
        }
    }

    @Override
    protected double getPhaseDuration() {
        return 45.0; // 45 segundos por fase
    }
    
    @Override
    protected void applyPhaseVisuals() {
        // Fase 2: Verde más brillante
        if (growthPhase == 2) {
            int r = (int)(originalColor.getRed() * 0.9);
            int g = Math.min(255, (int)(originalColor.getGreen() * 1.3));
            int b = (int)(originalColor.getBlue() * 1.1);
            this.color = new Color(r, g, b);
        }
        // Fase 3: Verde intenso y vibrante
        else if (growthPhase == 3) {
            int r = (int)(originalColor.getRed() * 0.7);
            int g = Math.min(255, (int)(originalColor.getGreen() * 1.5));
            int b = Math.min(255, (int)(originalColor.getBlue() * 1.2));
            this.color = new Color(r, g, b);
        }
    }

    @Override
    public String getSpeciesName() {
        return "Rana Saltarina";
    }
}
