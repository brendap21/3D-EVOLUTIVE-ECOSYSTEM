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
    private AnimationController animController;

    public AnimalType10(Vector3 posicion, long seed) {
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

        // Cabeza (patas se dibujan animadas)
        voxels.add(new Vector3(0, 1, 1));
    }

    @Override
    protected void renderNormal(SoftwareRenderer renderer, Camera cam) {
        animController.update(0.016);
        animController.setBlinkFrequency(2.2);
        animController.setJawFrequency(1.4);
        flapPhase += 0.3 * getPhaseSpeedMultiplier();
        boolean evolved = growthPhase >= 2;
        boolean apex = growthPhase == 3;
        Color body = applyGlowToColor(color);

        // Transición de fase: destello de alas/halo
        double tp = transitionPulse;
        if (tp > 0) {
            if (growthPhase == 2) {
                int wingFlash = applyScaleToSize((int)(voxelSize * (1.0 + tp)));
                Vector3 wpos = applyTransform(new Vector3(0, voxelSize * (1.4 + tp), 0));
                wpos = applyScaleToPosition(wpos);
                renderer.drawCubeShaded(renderer.getCubeVertices(wpos, wingFlash, 0), cam, body.brighter());
            } else if (growthPhase == 3) {
                int halo = applyScaleToSize((int)(voxelSize * (1.2 + tp)));
                Vector3 hpos = applyTransform(new Vector3(0, voxelSize * (2.4 + tp), 0));
                hpos = applyScaleToPosition(hpos);
                renderer.drawCubeShaded(renderer.getCubeVertices(hpos, halo, 0), cam, body.brighter().brighter());
            }
        }

        // Cuerpo base
        double bob = Math.sin(flapPhase * 1.2) * voxelSize * 0.35;
        for (Vector3 v : voxels) {
            Vector3 wp = applyTransform(new Vector3(v.x * voxelSize, v.y * voxelSize + bob, v.z * voxelSize));
            wp = applyScaleToPosition(wp);
            int s = applyScaleToSize(voxelSize);
            renderer.drawCubeShaded(renderer.getCubeVertices(wp, s, 0), cam, body);
        }

        // Alas membranosas (más grandes en fases avanzadas)
        int wingSpan = (apex ? 5 : (evolved ? 3 : 2) + growthPhase);
        int wingSize = applyScaleToSize((int)(voxelSize * (apex ? 0.85 : (evolved ? 0.65 : 0.5))));
        double wingFlap = Math.sin(flapPhase * 2.2) * voxelSize * (apex ? 1.6 : (evolved ? 1.3 : 1.05));
        Color wingColor = new Color(
            Math.max(0, body.getRed() - 30),
            Math.max(0, body.getGreen() - 30),
            Math.max(0, body.getBlue() - 20)
        );
        
        for (int i = 0; i < wingSpan; i++) {
            double wingY = voxelSize * (1.0 - i * 0.3) + wingFlap;
            double sweep = Math.sin(flapPhase * 1.6 + i * 0.4) * voxelSize * 0.4;
            Vector3 wingL = applyTransform(new Vector3(-voxelSize * (1.2 + i * 0.7), wingY, sweep));
            wingL = applyScaleToPosition(wingL);
            Vector3 wingR = applyTransform(new Vector3(voxelSize * (1.2 + i * 0.7), wingY, -sweep));
            wingR = applyScaleToPosition(wingR);
            renderer.drawCubeShaded(renderer.getCubeVertices(wingL, wingSize, 0), cam, wingColor);
            renderer.drawCubeShaded(renderer.getCubeVertices(wingR, wingSize, 0), cam, wingColor);
            if (evolved) {
                int edge = Math.max(1, wingSize / 2);
                renderer.drawCubeShaded(renderer.getCubeVertices(wingL, edge, 0), cam, body.brighter());
                renderer.drawCubeShaded(renderer.getCubeVertices(wingR, edge, 0), cam, body.brighter());
            }
            if (apex) {
                int tip = Math.max(1, wingSize / 3);
                Color glow = body.brighter().brighter();
                Vector3 tipL = applyTransform(new Vector3(-voxelSize * (1.2 + i * 0.7) * 1.3, wingY + voxelSize * 0.2, sweep * 1.2));
                Vector3 tipR = applyTransform(new Vector3(voxelSize * (1.2 + i * 0.7) * 1.3, wingY + voxelSize * 0.2, -sweep * 1.2));
                tipL = applyScaleToPosition(tipL); tipR = applyScaleToPosition(tipR);
                renderer.drawCubeShaded(renderer.getCubeVertices(tipL, tip, 0), cam, glow);
                renderer.drawCubeShaded(renderer.getCubeVertices(tipR, tip, 0), cam, glow);
            }
        }

        // Orejas puntiagudas
        int earSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.45)));
        double earTilt = Math.sin(flapPhase * 1.8) * voxelSize * 0.2;
        Vector3 earL = applyTransform(new Vector3(-voxelSize * 0.5 - earTilt, voxelSize * 2.0, voxelSize * 0.8));
        earL = applyScaleToPosition(earL);
        Vector3 earR = applyTransform(new Vector3(voxelSize * 0.5 + earTilt, voxelSize * 2.0, voxelSize * 0.8));
        earR = applyScaleToPosition(earR);
        renderer.drawCubeShaded(renderer.getCubeVertices(earL, earSize, 0), cam, body.darker());
        renderer.drawCubeShaded(renderer.getCubeVertices(earR, earSize, 0), cam, body.darker());

        // Ojos brillantes
        int eyeSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.38)));
        Color eyeGlow = new Color(255, 200, 100);
        Color pupil = new Color(20, 20, 30);
        Vector3 eyeL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 1.5, voxelSize * 1.3));
        eyeL = applyScaleToPosition(eyeL);
        Vector3 eyeR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 1.5, voxelSize * 1.3));
        eyeR = applyScaleToPosition(eyeR);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeL, eyeSize, 0), cam, eyeGlow);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeR, eyeSize, 0), cam, eyeGlow);
        int pup = Math.max(1, (int)(eyeSize * 0.55 * (1.0 - animController.getBlinkAmount())));
        Vector3 pupilL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 1.5, voxelSize * 1.3 + pup));
        pupilL = applyScaleToPosition(pupilL);
        Vector3 pupilR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 1.5, voxelSize * 1.3 + pup));
        pupilR = applyScaleToPosition(pupilR);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilL, pup, 0), cam, pupil);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilR, pup, 0), cam, pupil);

        // Boca animada
        double jaw = animController.getJawOpen();
        int mouthSize = Math.max(1, applyScaleToSize((int)(voxelSize * (0.2 + jaw * 0.25))));
        Vector3 mouth = applyTransform(new Vector3(0, voxelSize * 1.2 - jaw * 0.1, voxelSize * 1.5 + jaw * 0.2));
        mouth = applyScaleToPosition(mouth);
        renderer.drawCubeShaded(renderer.getCubeVertices(mouth, mouthSize, 0), cam, new Color(40, 40, 50));

        // Patas con garras (dos segmentos)
        int upper = applyScaleToSize((int)(voxelSize * (apex ? 1.05 : (evolved ? 0.9 : 0.75))));
        int lower = applyScaleToSize((int)(voxelSize * (apex ? 0.9 : (evolved ? 0.75 : 0.65))));
        Color claw = body.darker();
        double legPhase = Math.sin(flapPhase) * voxelSize * (apex ? 0.6 : (evolved ? 0.4 : 0.25));

        Vector3 leg1Up = applyTransform(new Vector3(-voxelSize * 0.5, -voxelSize * 0.2 + Math.abs(legPhase) * 0.25, legPhase * 0.25));
        Vector3 leg1Lo = applyTransform(new Vector3(-voxelSize * 0.55, -voxelSize * 0.7 + Math.abs(legPhase) * 0.15, legPhase * 0.35));
        leg1Up = applyScaleToPosition(leg1Up); leg1Lo = applyScaleToPosition(leg1Lo);
        renderer.drawCubeShaded(renderer.getCubeVertices(leg1Up, upper, 0), cam, claw);
        renderer.drawCubeShaded(renderer.getCubeVertices(leg1Lo, lower, 0), cam, claw);

        Vector3 leg2Up = applyTransform(new Vector3(voxelSize * 0.5, -voxelSize * 0.2 + Math.abs(legPhase) * 0.25, -legPhase * 0.25));
        Vector3 leg2Lo = applyTransform(new Vector3(voxelSize * 0.55, -voxelSize * 0.7 + Math.abs(legPhase) * 0.15, -legPhase * 0.35));
        leg2Up = applyScaleToPosition(leg2Up); leg2Lo = applyScaleToPosition(leg2Lo);
        renderer.drawCubeShaded(renderer.getCubeVertices(leg2Up, upper, 0), cam, claw);
        renderer.drawCubeShaded(renderer.getCubeVertices(leg2Lo, lower, 0), cam, claw);

        // Halo nocturno y cola corta en fase 3
        if (apex) {
            int halo = applyScaleToSize((int)(voxelSize * 1.0));
            Vector3 hpos = applyTransform(new Vector3(0, voxelSize * 2.5 + Math.sin(flapPhase) * voxelSize * 0.2, 0));
            hpos = applyScaleToPosition(hpos);
            renderer.drawCubeShaded(renderer.getCubeVertices(hpos, halo, 0), cam, body.brighter());

            int tail = applyScaleToSize((int)(voxelSize * 0.8));
            Vector3 tpos = applyTransform(new Vector3(0, voxelSize * 0.4, -voxelSize * 1.5));
            tpos = applyScaleToPosition(tpos);
            renderer.drawCubeShaded(renderer.getCubeVertices(tpos, tail, 0), cam, wingColor);
        }
    }

    @Override
    protected double getPhaseDuration(int phase) {
        // Total: 180 segundos (3 minutos)
        // Fase 1: 65s, Fase 2: 55s, Fase 3: 60s
        switch (phase) {
            case 1: return 65.0;
            case 2: return 55.0;
            case 3: return 60.0;
            default: return 60.0;
        }
    }
    
    @Override
    public int getSpeciesType() {
        return 9;
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
