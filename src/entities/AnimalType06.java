package entities;

import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * AnimalType06: Criatura radiante con antenas y brazos articulados.
 * Paleta fase 1 (3 colores): azul cielo, cian brillante, turquesa.
 * Fase 2-3 antenas más largas, brazos más móviles.
 */
public class AnimalType06 extends BaseAnimal {
    private double pulsePhase = 0.0;

    public AnimalType06(Vector3 posicion, long seed) {
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
            new Color(80, 180, 230),
            new Color(60, 220, 240),
            new Color(70, 200, 210)
        };
        this.color = palette[r.nextInt(palette.length)];
        this.originalColor = color;
        this.baseSpeed = 1.7 + r.nextDouble() * 0.6;

        // Cuerpo central
        voxels.add(new Vector3(0, 0, 0));
        voxels.add(new Vector3(0, 1, 0));
        voxels.add(new Vector3(0, 2, 0));

        // Patas bípedas
        voxels.add(new Vector3(0, -1, 0));
        voxels.add(new Vector3(0, -1, 1));
    }

    @Override
    protected void renderNormal(SoftwareRenderer renderer, Camera cam) {
        pulsePhase += 0.22 * getPhaseSpeedMultiplier();
        Color body = applyGlowToColor(color);

        // Efecto de pulso luminoso
        double pulse = Math.sin(pulsePhase * 2) * 0.2 + 0.8;
        Color glowBody = new Color(
            (int)(body.getRed() * pulse),
            (int)(body.getGreen() * pulse),
            (int)(body.getBlue() * pulse)
        );

        // Cuerpo base
        for (Vector3 v : voxels) {
            Vector3 wp = applyTransform(new Vector3(v.x * voxelSize, v.y * voxelSize, v.z * voxelSize));
            wp = applyScaleToPosition(wp);
            int s = applyScaleToSize(voxelSize);
            renderer.drawCubeShaded(renderer.getCubeVertices(wp, s, 0), cam, glowBody);
        }

        // Antenas (más largas en fases avanzadas)
        int antennaLen = 2 + growthPhase;
        int antennaSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.4)));
        for (int i = 0; i < antennaLen; i++) {
            double wobble = Math.sin(pulsePhase + i * 0.5) * voxelSize * 0.3;
            Vector3 antL = applyTransform(new Vector3(-voxelSize * 0.5 + wobble, voxelSize * (3.0 + i * 0.7), 0));
            antL = applyScaleToPosition(antL);
            Vector3 antR = applyTransform(new Vector3(voxelSize * 0.5 - wobble, voxelSize * (3.0 + i * 0.7), 0));
            antR = applyScaleToPosition(antR);
            renderer.drawCubeShaded(renderer.getCubeVertices(antL, antennaSize, 0), cam, glowBody.brighter());
            renderer.drawCubeShaded(renderer.getCubeVertices(antR, antennaSize, 0), cam, glowBody.brighter());
        }

        // Ojos grandes
        int eyeSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.4)));
        Color eyeW = new Color(200, 255, 255);
        Color pupil = new Color(20, 80, 100);
        Vector3 eyeL = applyTransform(new Vector3(-voxelSize * 0.5, voxelSize * 2.2, voxelSize * 0.5));
        eyeL = applyScaleToPosition(eyeL);
        Vector3 eyeR = applyTransform(new Vector3(voxelSize * 0.5, voxelSize * 2.2, voxelSize * 0.5));
        eyeR = applyScaleToPosition(eyeR);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeL, eyeSize, 0), cam, eyeW);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeR, eyeSize, 0), cam, eyeW);
        int pup = Math.max(1, eyeSize / 2);
        Vector3 pupilL = applyTransform(new Vector3(-voxelSize * 0.5, voxelSize * 2.2, voxelSize * 0.5 + pup));
        pupilL = applyScaleToPosition(pupilL);
        Vector3 pupilR = applyTransform(new Vector3(voxelSize * 0.5, voxelSize * 2.2, voxelSize * 0.5 + pup));
        pupilR = applyScaleToPosition(pupilR);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilL, pup, 0), cam, pupil);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilR, pup, 0), cam, pupil);

        // Boca pequeña
        int mouthSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.25)));
        Vector3 mouth = applyTransform(new Vector3(0, voxelSize * 1.7, voxelSize * 0.8));
        mouth = applyScaleToPosition(mouth);
        renderer.drawCubeShaded(renderer.getCubeVertices(mouth, mouthSize, 0), cam, new Color(50, 120, 140));

        // Brazos articulados (ondean)
        int armSegments = 2 + growthPhase / 2;
        int armSize = applyScaleToSize((int)(voxelSize * 0.6));
        for (int i = 0; i < armSegments; i++) {
            double armWave = Math.sin(pulsePhase + i * 0.7) * voxelSize * 0.5;
            Vector3 armL = applyTransform(new Vector3(-voxelSize * (1.2 + i * 0.5), voxelSize * (1.5 - i * 0.3) + armWave, 0));
            armL = applyScaleToPosition(armL);
            Vector3 armR = applyTransform(new Vector3(voxelSize * (1.2 + i * 0.5), voxelSize * (1.5 - i * 0.3) - armWave, 0));
            armR = applyScaleToPosition(armR);
            renderer.drawCubeShaded(renderer.getCubeVertices(armL, armSize, 0), cam, glowBody.darker());
            renderer.drawCubeShaded(renderer.getCubeVertices(armR, armSize, 0), cam, glowBody.darker());
        }

        // Patas bípedas con movimiento
        double leftLegPhase = Math.sin(pulsePhase) * voxelSize * 0.4;
        double rightLegPhase = Math.sin(pulsePhase + Math.PI) * voxelSize * 0.4;
        int legSize = applyScaleToSize((int)(voxelSize * 0.7));
        
        Vector3 legL = applyTransform(new Vector3(-voxelSize * 0.4, -voxelSize * 0.5 + Math.abs(leftLegPhase) * 0.2, leftLegPhase * 0.6));
        legL = applyScaleToPosition(legL);
        Vector3 legR = applyTransform(new Vector3(voxelSize * 0.4, -voxelSize * 0.5 + Math.abs(rightLegPhase) * 0.2, rightLegPhase * 0.6));
        legR = applyScaleToPosition(legR);
        renderer.drawCubeShaded(renderer.getCubeVertices(legL, legSize, 0), cam, glowBody.darker());
        renderer.drawCubeShaded(renderer.getCubeVertices(legR, legSize, 0), cam, glowBody.darker());
    }

    @Override
    protected double getPhaseDuration() {
        return 42.0; // 42 segundos por fase
    }
    
    @Override
    protected void applyPhaseVisuals() {
        // Fase 2: Cyan más brillante y vibrante
        if (growthPhase == 2) {
            int r = (int)(originalColor.getRed() * 0.9);
            int g = Math.min(255, (int)(originalColor.getGreen() * 1.25));
            int b = Math.min(255, (int)(originalColor.getBlue() * 1.3));
            this.color = new Color(r, g, b);
        }
        // Fase 3: Cyan profundo radiante
        else if (growthPhase == 3) {
            int r = (int)(originalColor.getRed() * 0.8);
            int g = Math.min(255, (int)(originalColor.getGreen() * 1.35));
            int b = Math.min(255, (int)(originalColor.getBlue() * 1.45));
            this.color = new Color(r, g, b);
        }
    }

    @Override
    public String getSpeciesName() {
        return "Radiante Cian";
    }
}
