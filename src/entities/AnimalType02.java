package entities;

import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * AnimalType02: Bípedo compacto con ojos grandes, pico y cresta.
 * Paleta fase 1 (3 colores): naranja fuego, rojo coral, amarillo quemado.
 * Fase 2-3 crece en altura, mueve brazos/alas.
 */
public class AnimalType02 extends BaseAnimal {
    private double animPhase = 0.0;

    public AnimalType02(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxels = new ArrayList<>();
        generateFromSeed(seed);
        initializeSpawnAnimation();
    }

    private void generateFromSeed(long seed) {
        Random r = new Random(seed);
        this.voxelSize = 3 + r.nextInt(2); // MÁS pequeño (3-4 en lugar de 7-9)
        this.baseVoxelSize = voxelSize;
        Color[] palette = new Color[]{
            new Color(230, 120, 40),
            new Color(210, 80, 70),
            new Color(200, 140, 40)
        };
        this.color = palette[r.nextInt(palette.length)];
        this.originalColor = color;
        this.baseSpeed = 1.3 + r.nextDouble() * 0.5;

        // Cuerpo
        voxels.add(new Vector3(0, 0, 0));
        voxels.add(new Vector3(0, 1, 0));
        // Cabeza grande
        voxels.add(new Vector3(0, 2, 0));
        voxels.add(new Vector3(0, 3, 0));
        // Patas
        voxels.add(new Vector3(0, -1, 0));
        voxels.add(new Vector3(0, -1, 1));
        // Brazos/alas
        voxels.add(new Vector3(1, 1, 0));
        voxels.add(new Vector3(-1, 1, 0));
    }

    @Override
    protected void renderNormal(SoftwareRenderer renderer, Camera cam) {
        animPhase += 0.15 * getPhaseSpeedMultiplier();
        Color body = applyGlowToColor(color);

        // Cuerpo base
        for (Vector3 v : voxels) {
            Vector3 wp = applyTransform(new Vector3(v.x * voxelSize, v.y * voxelSize, v.z * voxelSize));
            wp = applyScaleToPosition(wp);
            int s = applyScaleToSize(voxelSize);
            renderer.drawCubeShaded(renderer.getCubeVertices(wp, s, 0), cam, body);
        }

        // Cresta
        int crestSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.6)));
        for (int i = 0; i < 2; i++) {
            Vector3 cpos = applyTransform(new Vector3(0, voxelSize * (3.5 + i), voxelSize * 0.2));
            cpos = applyScaleToPosition(cpos);
            renderer.drawCubeShaded(renderer.getCubeVertices(cpos, crestSize, 0), cam, body.darker());
        }

        // Ojos
        int eye = Math.max(1, applyScaleToSize((int)(voxelSize * 0.35)));
        Color eyeW = new Color(245, 245, 245);
        Color pupil = new Color(30, 30, 40);
        Vector3 eyeL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 2.6, voxelSize * 0.9));
        eyeL = applyScaleToPosition(eyeL);
        Vector3 eyeR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 2.6, voxelSize * 0.9));
        eyeR = applyScaleToPosition(eyeR);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeL, eye, 0), cam, eyeW);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeR, eye, 0), cam, eyeW);
        int pup = Math.max(1, eye / 2);
        Vector3 pupilL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 2.6, voxelSize * 0.9 + pup));
        pupilL = applyScaleToPosition(pupilL);
        Vector3 pupilR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 2.6, voxelSize * 0.9 + pup));
        pupilR = applyScaleToPosition(pupilR);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilL, pup, 0), cam, pupil);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilR, pup, 0), cam, pupil);

        // Pico/boca
        int beak = Math.max(1, applyScaleToSize((int)(voxelSize * 0.5)));
        Vector3 beakPos = applyTransform(new Vector3(0, voxelSize * 2.1, voxelSize * 1.2));
        beakPos = applyScaleToPosition(beakPos);
        renderer.drawCubeShaded(renderer.getCubeVertices(beakPos, beak, 0), cam, new Color(240, 180, 60));

        // Patas bípedas (se alternan al caminar)
        double leftLegPhase = Math.sin(animPhase) * voxelSize * 0.4;
        double rightLegPhase = Math.sin(animPhase + Math.PI) * voxelSize * 0.4;
        int legSize = applyScaleToSize((int)(voxelSize * 0.75));
        
        // Pata izquierda
        Vector3 leftLeg = applyTransform(new Vector3(
            -voxelSize * 0.4,
            -voxelSize * 0.5 + Math.abs(leftLegPhase) * 0.2,
            leftLegPhase * 0.6
        ));
        leftLeg = applyScaleToPosition(leftLeg);
        renderer.drawCubeShaded(renderer.getCubeVertices(leftLeg, legSize, 0), cam, body.darker());
        
        // Pata derecha
        Vector3 rightLeg = applyTransform(new Vector3(
            voxelSize * 0.4,
            -voxelSize * 0.5 + Math.abs(rightLegPhase) * 0.2,
            rightLegPhase * 0.6
        ));
        rightLeg = applyScaleToPosition(rightLeg);
        renderer.drawCubeShaded(renderer.getCubeVertices(rightLeg, legSize, 0), cam, body.darker());
        
        // Brazos/alas (oscilan arriba/abajo opuestos a las patas para balance)
        double wingOff = Math.sin(animPhase) * voxelSize * 0.3;
        int wingSize = applyScaleToSize((int)(voxelSize * 0.85));
        Vector3 wingL = applyTransform(new Vector3(-voxelSize * 1.2, voxelSize * 1.2 + wingOff, 0));
        wingL = applyScaleToPosition(wingL);
        Vector3 wingR = applyTransform(new Vector3(voxelSize * 1.2, voxelSize * 1.2 - wingOff, 0));
        wingR = applyScaleToPosition(wingR);
        renderer.drawCubeShaded(renderer.getCubeVertices(wingL, wingSize, 0), cam, body.darker());
        renderer.drawCubeShaded(renderer.getCubeVertices(wingR, wingSize, 0), cam, body.darker());
    }
    
    @Override
    protected double getPhaseDuration() {
        return 35.0; // 35 segundos por fase
    }
    
    @Override
    protected void applyPhaseVisuals() {
        // Fase 2: Colores más cálidos e intensos
        if (growthPhase == 2) {
            int r = Math.min(255, (int)(originalColor.getRed() * 1.2));
            int g = (int)(originalColor.getGreen() * 1.1);
            int b = (int)(originalColor.getBlue() * 0.9);
            this.color = new Color(r, g, b);
        }
        // Fase 3: Colores brillantes tipo fuego
        else if (growthPhase == 3) {
            int r = Math.min(255, originalColor.getRed() + 25);
            int g = Math.min(255, (int)(originalColor.getGreen() * 1.3));
            int b = Math.max(20, (int)(originalColor.getBlue() * 0.7));
            this.color = new Color(r, g, b);
        }
    }
}
