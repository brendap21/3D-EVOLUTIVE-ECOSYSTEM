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
    private AnimationController animController;

    public AnimalType06(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxels = new ArrayList<>();
        this.animController = new AnimationController();
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

        // Cuerpo central (sin patas, se dibujan animadas)
        voxels.add(new Vector3(0, 0, 0));
        voxels.add(new Vector3(0, 1, 0));
        voxels.add(new Vector3(0, 2, 0));
    }

    @Override
    protected void renderNormal(SoftwareRenderer renderer, Camera cam) {
        animController.update(0.016);
        animController.setBlinkFrequency(3.2);
        animController.setJawFrequency(2.8);
        animController.setBodySwayFrequency(1.8);
        pulsePhase += 0.24 * getPhaseSpeedMultiplier();
        boolean evolved = growthPhase >= 2;
        boolean apex = growthPhase == 3;
        Color body = applyGlowToColor(color);

        // Transición de fase: halo y flash central
        double tp = transitionPulse;
        if (tp > 0) {
            if (growthPhase == 2) {
                int ring = applyScaleToSize((int)(voxelSize * (1.0 + tp)));
                Vector3 halo = applyTransform(new Vector3(0, voxelSize * (2.6 + tp), 0));
                halo = applyScaleToPosition(halo);
                renderer.drawCubeShaded(renderer.getCubeVertices(halo, ring, 0), cam, body.brighter());
            } else if (growthPhase == 3) {
                int core = applyScaleToSize((int)(voxelSize * (1.2 + tp)));
                Vector3 corePos = applyTransform(new Vector3(0, voxelSize * 1.0, 0));
                corePos = applyScaleToPosition(corePos);
                renderer.drawCubeShaded(renderer.getCubeVertices(corePos, core, 0), cam, body.brighter().brighter());
            }
        }

        // Efecto de pulso luminoso
        double pulse = Math.sin(pulsePhase * 2) * (apex ? 0.5 : (evolved ? 0.35 : 0.2)) + 0.8;
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
        int antennaLen = apex ? 7 : (evolved ? 4 + growthPhase : 2 + growthPhase);
        int antennaSize = Math.max(1, applyScaleToSize((int)(voxelSize * (apex ? 0.65 : (evolved ? 0.55 : 0.4)))));
        for (int i = 0; i < antennaLen; i++) {
            double wobble = Math.sin(pulsePhase + i * 0.5) * voxelSize * (apex ? 0.5 : 0.3);
            Vector3 antL = applyTransform(new Vector3(-voxelSize * 0.5 + wobble, voxelSize * (3.0 + i * 0.7), 0));
            antL = applyScaleToPosition(antL);
            Vector3 antR = applyTransform(new Vector3(voxelSize * 0.5 - wobble, voxelSize * (3.0 + i * 0.7), 0));
            antR = applyScaleToPosition(antR);
            renderer.drawCubeShaded(renderer.getCubeVertices(antL, antennaSize, 0), cam, glowBody.brighter());
            renderer.drawCubeShaded(renderer.getCubeVertices(antR, antennaSize, 0), cam, glowBody.brighter());
        }

        // Ojos grandes
        int eyeSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.45)));
        Color eyeW = new Color(200, 255, 255);
        Color pupil = new Color(20, 80, 100);
        Vector3 eyeL = applyTransform(new Vector3(-voxelSize * 0.5, voxelSize * 2.2, voxelSize * 0.5));
        eyeL = applyScaleToPosition(eyeL);
        Vector3 eyeR = applyTransform(new Vector3(voxelSize * 0.5, voxelSize * 2.2, voxelSize * 0.5));
        eyeR = applyScaleToPosition(eyeR);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeL, eyeSize, 0), cam, eyeW);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeR, eyeSize, 0), cam, eyeW);
        int pup = Math.max(1, (int)(eyeSize * 0.55 * (1.0 - animController.getBlinkAmount())));
        Vector3 pupilL = applyTransform(new Vector3(-voxelSize * 0.5, voxelSize * 2.2, voxelSize * 0.5 + pup));
        pupilL = applyScaleToPosition(pupilL);
        Vector3 pupilR = applyTransform(new Vector3(voxelSize * 0.5, voxelSize * 2.2, voxelSize * 0.5 + pup));
        pupilR = applyScaleToPosition(pupilR);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilL, pup, 0), cam, pupil);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilR, pup, 0), cam, pupil);

        // Boca pequeña
        double jaw = animController.getJawOpen() * 1.6;
        int mouthSize = Math.max(1, applyScaleToSize((int)(voxelSize * (0.25 + jaw * 0.2))));
        Vector3 mouth = applyTransform(new Vector3(0, voxelSize * 1.7 + jaw * 0.25, voxelSize * 0.8 + jaw));
        mouth = applyScaleToPosition(mouth);
        renderer.drawCubeShaded(renderer.getCubeVertices(mouth, mouthSize, 0), cam, new Color(50, 120, 140));

        // Brazos articulados (ondean) más largos en fase 2
        int armSegments = apex ? 5 : (evolved ? 3 : 2) + growthPhase / 2;
        int armUpper = applyScaleToSize((int)(voxelSize * (apex ? 1.0 : (evolved ? 0.85 : 0.7))));
        int armLower = applyScaleToSize((int)(voxelSize * (apex ? 0.9 : (evolved ? 0.75 : 0.6))));
        for (int i = 0; i < armSegments; i++) {
            double armWave = Math.sin(pulsePhase + i * 0.7) * voxelSize * (apex ? 1.2 : (evolved ? 0.9 : 0.6));
            Vector3 armLUp = applyTransform(new Vector3(-voxelSize * (1.1 + i * 0.45), voxelSize * (1.6 - i * 0.25) + armWave, 0));
            armLUp = applyScaleToPosition(armLUp);
            Vector3 armLLo = applyTransform(new Vector3(-voxelSize * (1.3 + i * 0.5), voxelSize * (1.1 - i * 0.25) + armWave * 0.8, voxelSize * 0.1));
            armLLo = applyScaleToPosition(armLLo);
            Vector3 armRUp = applyTransform(new Vector3(voxelSize * (1.1 + i * 0.45), voxelSize * (1.6 - i * 0.25) - armWave, 0));
            armRUp = applyScaleToPosition(armRUp);
            Vector3 armRLo = applyTransform(new Vector3(voxelSize * (1.3 + i * 0.5), voxelSize * (1.1 - i * 0.25) - armWave * 0.8, voxelSize * 0.1));
            armRLo = applyScaleToPosition(armRLo);
            renderer.drawCubeShaded(renderer.getCubeVertices(armLUp, armUpper, 0), cam, glowBody.darker());
            renderer.drawCubeShaded(renderer.getCubeVertices(armLLo, armLower, 0), cam, glowBody.darker());
            renderer.drawCubeShaded(renderer.getCubeVertices(armRUp, armUpper, 0), cam, glowBody.darker());
            renderer.drawCubeShaded(renderer.getCubeVertices(armRLo, armLower, 0), cam, glowBody.darker());
        }

        // Orbes de energía en manos fase 3
        if (apex) {
            int orb = applyScaleToSize((int)(voxelSize * 0.7));
            Vector3 orbL = applyTransform(new Vector3(-voxelSize * (1.3 + armSegments * 0.35), voxelSize * 0.6 + Math.sin(pulsePhase) * voxelSize * 0.3, voxelSize * 0.2));
            Vector3 orbR = applyTransform(new Vector3(voxelSize * (1.3 + armSegments * 0.35), voxelSize * 0.6 - Math.sin(pulsePhase) * voxelSize * 0.3, voxelSize * 0.2));
            orbL = applyScaleToPosition(orbL); orbR = applyScaleToPosition(orbR);
            renderer.drawCubeShaded(renderer.getCubeVertices(orbL, orb, 0), cam, glowBody.brighter().brighter());
            renderer.drawCubeShaded(renderer.getCubeVertices(orbR, orb, 0), cam, glowBody.brighter().brighter());
        }

        // Patas bípedas en dos segmentos
        double leftLegPhase = Math.sin(pulsePhase) * voxelSize * (evolved ? 0.7 : 0.5);
        double rightLegPhase = Math.sin(pulsePhase + Math.PI) * voxelSize * (evolved ? 0.7 : 0.5);
        int upperLeg = applyScaleToSize((int)(voxelSize * 0.8));
        int lowerLeg = applyScaleToSize((int)(voxelSize * 0.7));
        
        Vector3 legLUp = applyTransform(new Vector3(-voxelSize * 0.5, -voxelSize * 0.3 + Math.abs(leftLegPhase) * 0.3, leftLegPhase * 0.5));
        legLUp = applyScaleToPosition(legLUp);
        Vector3 legLLo = applyTransform(new Vector3(-voxelSize * 0.55, -voxelSize * 0.9 + Math.abs(leftLegPhase) * 0.2, leftLegPhase * 0.3));
        legLLo = applyScaleToPosition(legLLo);
        Vector3 legRUp = applyTransform(new Vector3(voxelSize * 0.5, -voxelSize * 0.3 + Math.abs(rightLegPhase) * 0.3, rightLegPhase * 0.5));
        legRUp = applyScaleToPosition(legRUp);
        Vector3 legRLo = applyTransform(new Vector3(voxelSize * 0.55, -voxelSize * 0.9 + Math.abs(rightLegPhase) * 0.2, rightLegPhase * 0.3));
        legRLo = applyScaleToPosition(legRLo);
        renderer.drawCubeShaded(renderer.getCubeVertices(legLUp, upperLeg, 0), cam, glowBody.darker());
        renderer.drawCubeShaded(renderer.getCubeVertices(legLLo, lowerLeg, 0), cam, glowBody.darker());
        renderer.drawCubeShaded(renderer.getCubeVertices(legRUp, upperLeg, 0), cam, glowBody.darker());
        renderer.drawCubeShaded(renderer.getCubeVertices(legRLo, lowerLeg, 0), cam, glowBody.darker());

        // Aro luminoso flotante solo fase 2/3
        if (evolved) {
            int ring = applyScaleToSize((int)(voxelSize * 0.9));
            Vector3 halo = applyTransform(new Vector3(0, voxelSize * 2.8 + Math.sin(pulsePhase) * voxelSize * 0.3, -voxelSize * 0.2));
            halo = applyScaleToPosition(halo);
            renderer.drawCubeShaded(renderer.getCubeVertices(halo, ring, 0), cam, glowBody.brighter());

            if (apex) {
                int ring2 = applyScaleToSize((int)(voxelSize * 1.2));
                Vector3 halo2 = applyTransform(new Vector3(0, voxelSize * 3.4 + Math.cos(pulsePhase) * voxelSize * 0.4, 0));
                halo2 = applyScaleToPosition(halo2);
                renderer.drawCubeShaded(renderer.getCubeVertices(halo2, ring2, 0), cam, glowBody.brighter().brighter());
            }
        }

        // Núcleo luminoso marcado en fase 3
        if (apex) {
            int core = applyScaleToSize((int)(voxelSize * 1.0));
            Vector3 corePos = applyTransform(new Vector3(0, voxelSize * 1.0, 0));
            corePos = applyScaleToPosition(corePos);
            renderer.drawCubeShaded(renderer.getCubeVertices(corePos, core, 0), cam, glowBody.brighter());
        }
    }

    @Override
    protected double getPhaseDuration(int phase) {
        // Total: 180 segundos (3 minutos)
        // Fase 1: 80s, Fase 2: 60s, Fase 3: 40s
        switch (phase) {
            case 1: return 80.0;
            case 2: return 60.0;
            case 3: return 40.0;
            default: return 60.0;
        }
    }
    
    @Override
    public int getSpeciesType() {
        return 5;
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
