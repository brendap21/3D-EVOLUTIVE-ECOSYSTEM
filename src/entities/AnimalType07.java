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
    private AnimationController animController;

    public AnimalType07(Vector3 posicion, long seed) {
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

    }

    @Override
    protected void renderNormal(SoftwareRenderer renderer, Camera cam) {
        animController.update(0.016);
        animController.setBlinkFrequency(2.6);
        animController.setJawFrequency(3.2);
        animController.setEarTwitchFrequency(2.5);
        walkPhase += 0.18 * getPhaseSpeedMultiplier();
        boolean evolved = growthPhase >= 2;
        boolean apex = growthPhase == 3;
        Color body = applyGlowToColor(color);

        // Cuerpo base con leve bombeo en fase 2
        double abdomenPulse = evolved ? Math.sin(walkPhase * 0.7) * voxelSize * (apex ? 0.45 : 0.3) : 0.0;
        for (Vector3 v : voxels) {
            Vector3 wp = applyTransform(new Vector3(v.x * voxelSize, v.y * voxelSize + abdomenPulse, v.z * voxelSize));
            wp = applyScaleToPosition(wp);
            int s = applyScaleToSize(voxelSize);
            renderer.drawCubeShaded(renderer.getCubeVertices(wp, s, 0), cam, body);
        }

        // Transición de fase: pulso abdominal brillante
        double tp = transitionPulse;
        if (tp > 0) {
            if (growthPhase == 2) {
                int pulse = applyScaleToSize((int)(voxelSize * (1.0 + tp)));
                Vector3 p = applyTransform(new Vector3(0, abdomenPulse, 0));
                p = applyScaleToPosition(p);
                renderer.drawCubeShaded(renderer.getCubeVertices(p, pulse, 0), cam, body.brighter());
            } else if (growthPhase == 3) {
                int flare = applyScaleToSize((int)(voxelSize * (1.2 + tp)));
                Vector3 p = applyTransform(new Vector3(0, abdomenPulse, -voxelSize));
                p = applyScaleToPosition(p);
                renderer.drawCubeShaded(renderer.getCubeVertices(p, flare, 0), cam, body.brighter().brighter());
            }
        }

        // Segmento extra y placas dorsales en fase 2/3
        if (evolved) {
            int seg = applyScaleToSize((int)(voxelSize * 0.9));
            Vector3 extra = applyTransform(new Vector3(0, abdomenPulse, -voxelSize * 2));
            extra = applyScaleToPosition(extra);
            renderer.drawCubeShaded(renderer.getCubeVertices(extra, seg, 0), cam, body);
            Color plate = body.brighter();
            for (int i = 0; i < 3; i++) {
                Vector3 pp = applyTransform(new Vector3(0, voxelSize * 0.7 + abdomenPulse, -voxelSize * (0.6 + i * 0.7)));
                pp = applyScaleToPosition(pp);
                renderer.drawCubeShaded(renderer.getCubeVertices(pp, applyScaleToSize((int)(voxelSize * 0.6)), 0), cam, plate);
            }
        }

        if (apex) {
            int extraSeg = applyScaleToSize((int)(voxelSize * 1.0));
            Vector3 tailSeg = applyTransform(new Vector3(0, abdomenPulse, -voxelSize * 3));
            tailSeg = applyScaleToPosition(tailSeg);
            renderer.drawCubeShaded(renderer.getCubeVertices(tailSeg, extraSeg, 0), cam, body.brighter());
        }

        // Mandíbulas (más grandes en fases avanzadas)
        int mandibleSize = applyScaleToSize((int)(voxelSize * (0.6 + growthPhase * 0.25 + (apex ? 0.3 : 0))));
        double mandOpen = animController.getJawOpen() * voxelSize * (apex ? 2.0 : (evolved ? 1.5 : 1.0));
        Vector3 mandL = applyTransform(new Vector3(-voxelSize * 0.6 - mandOpen, -voxelSize * 0.2, voxelSize * 2.5));
        mandL = applyScaleToPosition(mandL);
        Vector3 mandR = applyTransform(new Vector3(voxelSize * 0.6 + mandOpen, -voxelSize * 0.2, voxelSize * 2.5));
        mandR = applyScaleToPosition(mandR);
        renderer.drawCubeShaded(renderer.getCubeVertices(mandL, mandibleSize, 0), cam, body.darker());
        renderer.drawCubeShaded(renderer.getCubeVertices(mandR, mandibleSize, 0), cam, body.darker());

        // Ojos compuestos
        int eyeSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.4)));
        Color eyeColor = new Color(230, 70, 70);
        Vector3 eyeL = applyTransform(new Vector3(-voxelSize * 0.6, voxelSize * 0.3, voxelSize * 2.2));
        eyeL = applyScaleToPosition(eyeL);
        Vector3 eyeR = applyTransform(new Vector3(voxelSize * 0.6, voxelSize * 0.3, voxelSize * 2.2));
        eyeR = applyScaleToPosition(eyeR);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeL, eyeSize, 0), cam, eyeColor);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeR, eyeSize, 0), cam, eyeColor);
        int pup = Math.max(1, (int)(eyeSize * 0.45 * (1.0 - animController.getBlinkAmount())));
        if (pup > 0) {
            Vector3 pL = applyTransform(new Vector3(-voxelSize * 0.6, voxelSize * 0.3, voxelSize * 2.2 + pup));
            pL = applyScaleToPosition(pL);
            Vector3 pR = applyTransform(new Vector3(voxelSize * 0.6, voxelSize * 0.3, voxelSize * 2.2 + pup));
            pR = applyScaleToPosition(pR);
            renderer.drawCubeShaded(renderer.getCubeVertices(pL, pup, 0), cam, new Color(80, 20, 20));
            renderer.drawCubeShaded(renderer.getCubeVertices(pR, pup, 0), cam, new Color(80, 20, 20));
        }

        // Antenas
        int antennaLen = apex ? 6 : (evolved ? 2 + growthPhase : 1 + growthPhase);
        int antennaSize = Math.max(1, applyScaleToSize((int)(voxelSize * (apex ? 0.5 : 0.35))));
        for (int i = 0; i < antennaLen; i++) {
            double bend = Math.sin(walkPhase + i * 0.7) * voxelSize * (apex ? 0.35 : 0.2);
            Vector3 antL = applyTransform(new Vector3(-voxelSize * 0.4 + bend, voxelSize * (0.8 + i * 0.6), voxelSize * 2.3));
            antL = applyScaleToPosition(antL);
            Vector3 antR = applyTransform(new Vector3(voxelSize * 0.4 - bend, voxelSize * (0.8 + i * 0.6), voxelSize * 2.3));
            antR = applyScaleToPosition(antR);
            renderer.drawCubeShaded(renderer.getCubeVertices(antL, antennaSize, 0), cam, body.brighter());
            renderer.drawCubeShaded(renderer.getCubeVertices(antR, antennaSize, 0), cam, body.brighter());
        }

        // 6 patas con movimiento alternado tipo insecto en dos segmentos
        int upper = applyScaleToSize((int)(voxelSize * (evolved ? 0.9 : 0.75)));
        int lower = applyScaleToSize((int)(voxelSize * (evolved ? 0.8 : 0.65)));
        Color paw = body.darker();
        double leg1 = Math.sin(walkPhase) * voxelSize * (apex ? 0.75 : (evolved ? 0.55 : 0.35));
        double leg2 = Math.sin(walkPhase + Math.PI) * voxelSize * (apex ? 0.75 : (evolved ? 0.55 : 0.35));

        Vector3 l1Up = applyTransform(new Vector3(-voxelSize * 1.1, -voxelSize * 0.3 + Math.abs(leg1) * 0.3, voxelSize + leg1 * 0.5));
        Vector3 l1Lo = applyTransform(new Vector3(-voxelSize * 1.15, -voxelSize * 0.9 + Math.abs(leg1) * 0.2, voxelSize * 0.7 + leg1 * 0.3));
        l1Up = applyScaleToPosition(l1Up); l1Lo = applyScaleToPosition(l1Lo);
        renderer.drawCubeShaded(renderer.getCubeVertices(l1Up, upper, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(l1Lo, lower, 0), cam, paw);

        Vector3 l2Up = applyTransform(new Vector3(voxelSize * 1.1, -voxelSize * 0.3 + Math.abs(leg2) * 0.3, voxelSize + leg2 * 0.5));
        Vector3 l2Lo = applyTransform(new Vector3(voxelSize * 1.15, -voxelSize * 0.9 + Math.abs(leg2) * 0.2, voxelSize * 0.7 + leg2 * 0.3));
        l2Up = applyScaleToPosition(l2Up); l2Lo = applyScaleToPosition(l2Lo);
        renderer.drawCubeShaded(renderer.getCubeVertices(l2Up, upper, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(l2Lo, lower, 0), cam, paw);

        Vector3 l3Up = applyTransform(new Vector3(-voxelSize * 1.1, -voxelSize * 0.3 + Math.abs(leg2) * 0.3, leg2 * 0.5));
        Vector3 l3Lo = applyTransform(new Vector3(-voxelSize * 1.15, -voxelSize * 0.9 + Math.abs(leg2) * 0.2, leg2 * 0.3));
        l3Up = applyScaleToPosition(l3Up); l3Lo = applyScaleToPosition(l3Lo);
        renderer.drawCubeShaded(renderer.getCubeVertices(l3Up, upper, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(l3Lo, lower, 0), cam, paw);

        Vector3 l4Up = applyTransform(new Vector3(voxelSize * 1.1, -voxelSize * 0.3 + Math.abs(leg1) * 0.3, leg1 * 0.5));
        Vector3 l4Lo = applyTransform(new Vector3(voxelSize * 1.15, -voxelSize * 0.9 + Math.abs(leg1) * 0.2, leg1 * 0.3));
        l4Up = applyScaleToPosition(l4Up); l4Lo = applyScaleToPosition(l4Lo);
        renderer.drawCubeShaded(renderer.getCubeVertices(l4Up, upper, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(l4Lo, lower, 0), cam, paw);

        Vector3 l5Up = applyTransform(new Vector3(-voxelSize * 1.1, -voxelSize * 0.3 + Math.abs(leg1) * 0.3, -voxelSize + leg1 * 0.5));
        Vector3 l5Lo = applyTransform(new Vector3(-voxelSize * 1.15, -voxelSize * 0.9 + Math.abs(leg1) * 0.2, -voxelSize * 1.3 + leg1 * 0.3));
        l5Up = applyScaleToPosition(l5Up); l5Lo = applyScaleToPosition(l5Lo);
        renderer.drawCubeShaded(renderer.getCubeVertices(l5Up, upper, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(l5Lo, lower, 0), cam, paw);

        Vector3 l6Up = applyTransform(new Vector3(voxelSize * 1.1, -voxelSize * 0.3 + Math.abs(leg2) * 0.3, -voxelSize + leg2 * 0.5));
        Vector3 l6Lo = applyTransform(new Vector3(voxelSize * 1.15, -voxelSize * 0.9 + Math.abs(leg2) * 0.2, -voxelSize * 1.3 + leg2 * 0.3));
        l6Up = applyScaleToPosition(l6Up); l6Lo = applyScaleToPosition(l6Lo);
        renderer.drawCubeShaded(renderer.getCubeVertices(l6Up, upper, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(l6Lo, lower, 0), cam, paw);

        // Abdomen bioluminiscente en fase 3
        if (apex) {
            int glow = applyScaleToSize((int)(voxelSize * 0.8));
            Vector3 glowPos = applyTransform(new Vector3(0, abdomenPulse, -voxelSize));
            glowPos = applyScaleToPosition(glowPos);
            renderer.drawCubeShaded(renderer.getCubeVertices(glowPos, glow, 0), cam, body.brighter().brighter());
        }
    }

    @Override
    protected double getPhaseDuration() {
        return 38.0; // 38 segundos por fase
    }
    
    @Override
    protected void applyPhaseVisuals() {
        // Fase 2: Verde esmeralda más brillante
        if (growthPhase == 2) {
            int r = (int)(originalColor.getRed() * 0.85);
            int g = Math.min(255, (int)(originalColor.getGreen() * 1.3));
            int b = (int)(originalColor.getBlue() * 1.1);
            this.color = new Color(r, g, b);
        }
        // Fase 3: Verde profundo metálico
        else if (growthPhase == 3) {
            int r = (int)(originalColor.getRed() * 0.7);
            int g = Math.min(255, (int)(originalColor.getGreen() * 1.45));
            int b = Math.min(255, (int)(originalColor.getBlue() * 1.25));
            this.color = new Color(r, g, b);
        }
    }

    @Override
    public String getSpeciesName() {
        return "Escarabajo Esmeralda";
    }
}
