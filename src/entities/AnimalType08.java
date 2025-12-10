package entities;

import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * AnimalType08: Criatura acorazada con placas protectoras.
 * Paleta fase 1 (3 colores): marrón tierra, óxido, café oscuro.
 * Fase 2-3 más placas, cuerpo más robusto.
 */
public class AnimalType08 extends BaseAnimal {
    private double walkPhase = 0.0;
    private AnimationController animController;

    public AnimalType08(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxels = new ArrayList<>();
        this.animController = new AnimationController();
        generateFromSeed(seed);
        initializeSpawnAnimation();
    }

    private void generateFromSeed(long seed) {
        Random r = new Random(seed);
        this.voxelSize = 4 + r.nextInt(2);
        this.baseVoxelSize = voxelSize;
        Color[] palette = new Color[]{
            new Color(150, 110, 70),
            new Color(180, 95, 55),
            new Color(110, 80, 55)
        };
        this.color = palette[r.nextInt(palette.length)];
        this.originalColor = color;
        this.baseSpeed = 0.8 + r.nextDouble() * 0.4;

        // Cuerpo blindado (sin patas; se dibujan animadas)
        voxels.add(new Vector3(0, 0, 0));
        voxels.add(new Vector3(1, 0, 0));
        voxels.add(new Vector3(-1, 0, 0));
        voxels.add(new Vector3(0, 0, 1));
        voxels.add(new Vector3(0, 0, -1));
        voxels.add(new Vector3(0, 1, 0));

        // Cabeza
        voxels.add(new Vector3(0, 0, 2));
    }

    @Override
    protected void renderNormal(SoftwareRenderer renderer, Camera cam) {
        animController.update(0.016);
        animController.setBlinkFrequency(2.4);
        animController.setJawFrequency(1.8);
        animController.setTailWagFrequency(1.5);
        walkPhase += 0.16 * getPhaseSpeedMultiplier();
        boolean evolved = growthPhase >= 2;
        boolean apex = growthPhase == 3;
        Color body = applyGlowToColor(color);

        // Transición de fase: destello en caparazón
        double tp = transitionPulse;
        if (tp > 0) {
            if (growthPhase == 2) {
                int flash = applyScaleToSize((int)(voxelSize * (1.0 + tp)));
                Vector3 shell = applyTransform(new Vector3(0, voxelSize * (0.8 + tp), 0));
                shell = applyScaleToPosition(shell);
                renderer.drawCubeShaded(renderer.getCubeVertices(shell, flash, 0), cam, body.brighter());
            } else if (growthPhase == 3) {
                int vent = applyScaleToSize((int)(voxelSize * (1.2 + tp)));
                Vector3 shell = applyTransform(new Vector3(0, voxelSize * (1.0 + tp), -voxelSize * 0.4));
                shell = applyScaleToPosition(shell);
                renderer.drawCubeShaded(renderer.getCubeVertices(shell, vent, 0), cam, body.brighter().brighter());
            }
        }

        // Cuerpo base
        double bodyBob = Math.sin(walkPhase * 0.8) * voxelSize * 0.15;
        for (Vector3 v : voxels) {
            Vector3 wp = applyTransform(new Vector3(v.x * voxelSize, v.y * voxelSize + bodyBob, v.z * voxelSize));
            wp = applyScaleToPosition(wp);
            int s = applyScaleToSize(voxelSize);
            renderer.drawCubeShaded(renderer.getCubeVertices(wp, s, 0), cam, body);
        }

        // Placas dorsales (más numerosas en fases avanzadas)
        int plateCount = (apex ? 10 : (evolved ? 5 : 2)) + growthPhase * 2;
        int plateSize = applyScaleToSize((int)(voxelSize * (apex ? 1.0 : (evolved ? 0.9 : 0.7))));
        Color plateColor = body.brighter().brighter();
        for (int i = 0; i < plateCount; i++) {
            Vector3 plate = applyTransform(new Vector3(
                0,
                voxelSize * (1.5 + i * 0.2),
                voxelSize * (1.0 - i * 0.6)
            ));
            plate = applyScaleToPosition(plate);
            renderer.drawCubeShaded(renderer.getCubeVertices(plate, plateSize, 0), cam, plateColor);
        }

        if (apex) {
            int shoulder = applyScaleToSize((int)(voxelSize * 1.2));
            Vector3 shL = applyTransform(new Vector3(-voxelSize * 1.4, voxelSize * 0.8, 0));
            Vector3 shR = applyTransform(new Vector3(voxelSize * 1.4, voxelSize * 0.8, 0));
            shL = applyScaleToPosition(shL); shR = applyScaleToPosition(shR);
            renderer.drawCubeShaded(renderer.getCubeVertices(shL, shoulder, 0), cam, plateColor);
            renderer.drawCubeShaded(renderer.getCubeVertices(shR, shoulder, 0), cam, plateColor);
        }

        // Ojos pequeños
        int eyeSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.35)));
        Color eyeW = new Color(220, 220, 180);
        Color pupil = new Color(60, 40, 20);
        Vector3 eyeL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 0.3, voxelSize * 2.3));
        eyeL = applyScaleToPosition(eyeL);
        Vector3 eyeR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 0.3, voxelSize * 2.3));
        eyeR = applyScaleToPosition(eyeR);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeL, eyeSize, 0), cam, eyeW);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeR, eyeSize, 0), cam, eyeW);
        int pup = Math.max(1, (int)(eyeSize * 0.5 * (1.0 - animController.getBlinkAmount())));
        Vector3 pupilL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 0.3, voxelSize * 2.3 + pup));
        pupilL = applyScaleToPosition(pupilL);
        Vector3 pupilR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 0.3, voxelSize * 2.3 + pup));
        pupilR = applyScaleToPosition(pupilR);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilL, pup, 0), cam, pupil);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilR, pup, 0), cam, pupil);

        // Boca
        double jaw = animController.getJawOpen() * 1.1;
        int mouthSize = Math.max(1, applyScaleToSize((int)(voxelSize * (0.3 + jaw * 0.2))));
        Vector3 mouth = applyTransform(new Vector3(0, -voxelSize * 0.2 + jaw * 0.2, voxelSize * 2.5 + jaw));
        mouth = applyScaleToPosition(mouth);
        renderer.drawCubeShaded(renderer.getCubeVertices(mouth, mouthSize, 0), cam, new Color(80, 50, 30));

        // Cola acorazada
        int tailLen = apex ? 5 : (evolved ? 3 : 1) + growthPhase;
        int tailSize = applyScaleToSize((int)(voxelSize * (apex ? 1.05 : (evolved ? 0.95 : 0.8))));
        for (int i = 0; i < tailLen; i++) {
            double wag = animController.getTailWagOffset(i) * (apex ? 0.8 : (evolved ? 0.6 : 0.4));
            double wagV = animController.getTailWagVertical(i) * (apex ? 0.6 : (evolved ? 0.45 : 0.3));
            Vector3 tpos = applyTransform(new Vector3(wag, voxelSize * 0.3 + wagV, -voxelSize * (1.5 + i * 0.8)));
            tpos = applyScaleToPosition(tpos);
            Color tailC = apex && i == tailLen - 1 ? plateColor : body.darker();
            renderer.drawCubeShaded(renderer.getCubeVertices(tpos, tailSize, 0), cam, tailC);
        }

        // Patas robustas en dos segmentos con movimiento lento
        double legAmp = evolved ? (apex ? 0.55 : 0.35) : 0.22;
        double frontLeftLeg = Math.sin(walkPhase) * voxelSize * legAmp;
        double frontRightLeg = Math.sin(walkPhase + Math.PI) * voxelSize * legAmp;
        double backLeftLeg = Math.sin(walkPhase + Math.PI) * voxelSize * legAmp;
        double backRightLeg = Math.sin(walkPhase) * voxelSize * legAmp;
        int upper = applyScaleToSize((int)(voxelSize * 1.15));
        int lower = applyScaleToSize((int)(voxelSize * 1.0));
        Color paw = body.darker().darker();

        Vector3 flUp = applyTransform(new Vector3(-voxelSize * 1.1, -voxelSize * 0.2 + Math.abs(frontLeftLeg) * 0.3, voxelSize + frontLeftLeg * 0.35));
        Vector3 flLo = applyTransform(new Vector3(-voxelSize * 1.2, -voxelSize * 0.9 + Math.abs(frontLeftLeg) * 0.18, voxelSize * 0.7 + frontLeftLeg * 0.25));
        flUp = applyScaleToPosition(flUp); flLo = applyScaleToPosition(flLo);
        renderer.drawCubeShaded(renderer.getCubeVertices(flUp, upper, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(flLo, lower, 0), cam, paw);

        Vector3 frUp = applyTransform(new Vector3(voxelSize * 1.1, -voxelSize * 0.2 + Math.abs(frontRightLeg) * 0.3, voxelSize + frontRightLeg * 0.35));
        Vector3 frLo = applyTransform(new Vector3(voxelSize * 1.2, -voxelSize * 0.9 + Math.abs(frontRightLeg) * 0.18, voxelSize * 0.7 + frontRightLeg * 0.25));
        frUp = applyScaleToPosition(frUp); frLo = applyScaleToPosition(frLo);
        renderer.drawCubeShaded(renderer.getCubeVertices(frUp, upper, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(frLo, lower, 0), cam, paw);

        Vector3 blUp = applyTransform(new Vector3(-voxelSize * 1.1, -voxelSize * 0.2 + Math.abs(backLeftLeg) * 0.3, -voxelSize + backLeftLeg * 0.35));
        Vector3 blLo = applyTransform(new Vector3(-voxelSize * 1.2, -voxelSize * 0.9 + Math.abs(backLeftLeg) * 0.18, -voxelSize * 1.3 + backLeftLeg * 0.25));
        blUp = applyScaleToPosition(blUp); blLo = applyScaleToPosition(blLo);
        renderer.drawCubeShaded(renderer.getCubeVertices(blUp, upper, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(blLo, lower, 0), cam, paw);

        Vector3 brUp = applyTransform(new Vector3(voxelSize * 1.1, -voxelSize * 0.2 + Math.abs(backRightLeg) * 0.3, -voxelSize + backRightLeg * 0.35));
        Vector3 brLo = applyTransform(new Vector3(voxelSize * 1.2, -voxelSize * 0.9 + Math.abs(backRightLeg) * 0.18, -voxelSize * 1.3 + backRightLeg * 0.25));
        brUp = applyScaleToPosition(brUp); brLo = applyScaleToPosition(brLo);
        renderer.drawCubeShaded(renderer.getCubeVertices(brUp, upper, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(brLo, lower, 0), cam, paw);

        // Ventanas brillantes en caparazón fase 3
        if (apex) {
            int vent = applyScaleToSize((int)(voxelSize * 0.8));
            Vector3 ventPos = applyTransform(new Vector3(0, voxelSize * 0.5 + bodyBob, 0));
            ventPos = applyScaleToPosition(ventPos);
            renderer.drawCubeShaded(renderer.getCubeVertices(ventPos, vent, 0), cam, plateColor);
        }
    }

    @Override
    protected double getPhaseDuration(int phase) {
        // Total: 180 segundos (3 minutos)
        // Fase 1: 55s, Fase 2: 65s, Fase 3: 60s
        switch (phase) {
            case 1: return 55.0;
            case 2: return 65.0;
            case 3: return 60.0;
            default: return 60.0;
        }
    }
    
    @Override
    public int getSpeciesType() {
        return 7;
    }
    
    @Override
    protected void applyPhaseVisuals() {
        // Fase 2: Tonos terrosos más cálidos
        if (growthPhase == 2) {
            int r = Math.min(255, (int)(originalColor.getRed() * 1.15));
            int g = Math.min(255, (int)(originalColor.getGreen() * 1.1));
            int b = (int)(originalColor.getBlue() * 0.95);
            this.color = new Color(r, g, b);
        }
        // Fase 3: Marrón dorado
        else if (growthPhase == 3) {
            int r = Math.min(255, (int)(originalColor.getRed() * 1.25));
            int g = Math.min(255, (int)(originalColor.getGreen() * 1.2));
            int b = (int)(originalColor.getBlue() * 0.85);
            this.color = new Color(r, g, b);
        }
    }

    @Override
    public String getSpeciesName() {
        return "Armadillo Tierra";
    }
}
