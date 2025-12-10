package entities;

import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * AnimalType10: Criatura nocturna con alas membranosas.
 * Paleta fase 1 (3 colores): gris oscuro, plata, carbón.
 * Fase 2-3 alas más grandes, movimiento más ágil.
 */
public class AnimalType10 extends BaseAnimal {
    private double flapPhase = 0.0;

    public AnimalType10(Vector3 posicion, long seed) {
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
            new Color(70, 70, 80),
            new Color(140, 140, 150),
            new Color(50, 50, 55)
        };
        this.color = palette[r.nextInt(palette.length)];
        this.originalColor = color;
        this.baseSpeed = 1.5 + r.nextDouble() * 0.7;

        // Cuerpo pequeño
        voxels.add(new Vector3(0, 0, 0));
        voxels.add(new Vector3(0, 1, 0));

        // Cabeza
        voxels.add(new Vector3(0, 1, 1));

        // Patas
        voxels.add(new Vector3(-1, -1, 0));
        voxels.add(new Vector3(1, -1, 0));
    }

    @Override
    protected void renderNormal(SoftwareRenderer renderer, Camera cam) {
        flapPhase += 0.22 * getPhaseSpeedMultiplier();
        Color body = applyGlowToColor(color);

        // Cuerpo base
        for (Vector3 v : voxels) {
            Vector3 wp = applyTransform(new Vector3(v.x * voxelSize, v.y * voxelSize, v.z * voxelSize));
            wp = applyScaleToPosition(wp);
            int s = applyScaleToSize(voxelSize);
            renderer.drawCubeShaded(renderer.getCubeVertices(wp, s, 0), cam, body);
        }

        // Alas membranosas (más grandes en fases avanzadas)
        int wingSpan = 2 + growthPhase;
        int wingSize = applyScaleToSize((int)(voxelSize * 0.5));
        double wingFlap = Math.sin(flapPhase * 2) * voxelSize * 0.8;
        Color wingColor = new Color(
            Math.max(0, body.getRed() - 30),
            Math.max(0, body.getGreen() - 30),
            Math.max(0, body.getBlue() - 20)
        );
        
        for (int i = 0; i < wingSpan; i++) {
            double wingY = voxelSize * (1.0 - i * 0.3) + wingFlap;
            Vector3 wingL = applyTransform(new Vector3(-voxelSize * (1.2 + i * 0.7), wingY, 0));
            wingL = applyScaleToPosition(wingL);
            Vector3 wingR = applyTransform(new Vector3(voxelSize * (1.2 + i * 0.7), wingY, 0));
            wingR = applyScaleToPosition(wingR);
            renderer.drawCubeShaded(renderer.getCubeVertices(wingL, wingSize, 0), cam, wingColor);
            renderer.drawCubeShaded(renderer.getCubeVertices(wingR, wingSize, 0), cam, wingColor);
        }

        // Orejas puntiagudas
        int earSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.4)));
        Vector3 earL = applyTransform(new Vector3(-voxelSize * 0.5, voxelSize * 2.0, voxelSize * 0.8));
        earL = applyScaleToPosition(earL);
        Vector3 earR = applyTransform(new Vector3(voxelSize * 0.5, voxelSize * 2.0, voxelSize * 0.8));
        earR = applyScaleToPosition(earR);
        renderer.drawCubeShaded(renderer.getCubeVertices(earL, earSize, 0), cam, body.darker());
        renderer.drawCubeShaded(renderer.getCubeVertices(earR, earSize, 0), cam, body.darker());

        // Ojos brillantes
        int eyeSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.35)));
        Color eyeGlow = new Color(255, 200, 100);
        Color pupil = new Color(20, 20, 30);
        Vector3 eyeL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 1.5, voxelSize * 1.3));
        eyeL = applyScaleToPosition(eyeL);
        Vector3 eyeR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 1.5, voxelSize * 1.3));
        eyeR = applyScaleToPosition(eyeR);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeL, eyeSize, 0), cam, eyeGlow);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeR, eyeSize, 0), cam, eyeGlow);
        int pup = Math.max(1, eyeSize / 2);
        Vector3 pupilL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 1.5, voxelSize * 1.3 + pup));
        pupilL = applyScaleToPosition(pupilL);
        Vector3 pupilR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 1.5, voxelSize * 1.3 + pup));
        pupilR = applyScaleToPosition(pupilR);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilL, pup, 0), cam, pupil);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilR, pup, 0), cam, pupil);

        // Boca pequeña
        int mouthSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.2)));
        Vector3 mouth = applyTransform(new Vector3(0, voxelSize * 1.2, voxelSize * 1.5));
        mouth = applyScaleToPosition(mouth);
        renderer.drawCubeShaded(renderer.getCubeVertices(mouth, mouthSize, 0), cam, new Color(40, 40, 50));

        // Patas con garras
        int legSize = applyScaleToSize((int)(voxelSize * 0.6));
        Color claw = body.darker();
        double legPhase = Math.sin(flapPhase) * voxelSize * 0.2;
        
        Vector3 leg1 = applyTransform(new Vector3(-voxelSize * 0.5, -voxelSize * 0.5 + Math.abs(legPhase) * 0.2, legPhase * 0.3));
        leg1 = applyScaleToPosition(leg1);
        Vector3 leg2 = applyTransform(new Vector3(voxelSize * 0.5, -voxelSize * 0.5 - Math.abs(legPhase) * 0.2, -legPhase * 0.3));
        leg2 = applyScaleToPosition(leg2);
        renderer.drawCubeShaded(renderer.getCubeVertices(leg1, legSize, 0), cam, claw);
        renderer.drawCubeShaded(renderer.getCubeVertices(leg2, legSize, 0), cam, claw);
    }

    @Override
    protected double getPhaseDuration() {
        return 60.0; // 60 segundos por fase (evolución más lenta)
    }
    
    @Override
    protected void applyPhaseVisuals() {
        // Fase 2: Tonos más oscuros y profundos
        if (growthPhase == 2) {
            int r = (int)(originalColor.getRed() * 0.85);
            int g = (int)(originalColor.getGreen() * 0.9);
            int b = Math.min(255, (int)(originalColor.getBlue() * 1.2));
            this.color = new Color(r, g, b);
        }
        // Fase 3: Negro azulado nocturno
        else if (growthPhase == 3) {
            int r = (int)(originalColor.getRed() * 0.7);
            int g = (int)(originalColor.getGreen() * 0.75);
            int b = Math.min(255, (int)(originalColor.getBlue() * 1.3));
            this.color = new Color(r, g, b);
        }
    }

    @Override
    public String getSpeciesName() {
        return "Murciélago Nocturno";
    }
}
