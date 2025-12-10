package entities;

import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * AnimalType05: Criatura con púas/espinas que sobresalen, cuadrúpeda defensiva.
 * Paleta fase 1 (3 colores): rojo ladrillo, carmesí oscuro, burdeos.
 * Fase 2-3 más espinas, cuerpo más robusto.
 */
public class AnimalType05 extends BaseAnimal {
    private double walkPhase = 0.0;
    private AnimationController animController;

    public AnimalType05(Vector3 posicion, long seed) {
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
            new Color(150, 50, 40),
            new Color(180, 30, 50),
            new Color(130, 40, 50)
        };
        this.color = palette[r.nextInt(palette.length)];
        this.originalColor = color;
        this.baseSpeed = 1.1 + r.nextDouble() * 0.5;

        // Núcleo del cuerpo
        voxels.add(new Vector3(0, 0, 0));
        voxels.add(new Vector3(1, 0, 0));
        voxels.add(new Vector3(-1, 0, 0));
        voxels.add(new Vector3(0, 0, 1));
        voxels.add(new Vector3(0, 0, -1));

        // Cabeza
        voxels.add(new Vector3(0, 1, 1));
    }

    @Override
    protected void renderNormal(SoftwareRenderer renderer, Camera cam) {
        animController.update(0.016);
        animController.setBlinkFrequency(3.2);
        animController.setTailWagFrequency(1.8);
        walkPhase += 0.18 * getPhaseSpeedMultiplier();
        boolean evolved = growthPhase >= 2;
        boolean apex = growthPhase == 3;
        Color body = applyGlowToColor(color);

        // Transición de fase: estallido de espinas
        double tp = transitionPulse;
        if (tp > 0) {
            if (growthPhase == 2) {
                int burst = applyScaleToSize((int)(voxelSize * (1.0 + tp)));
                Vector3 spine = applyTransform(new Vector3(0, voxelSize * (0.8 + tp), 0));
                spine = applyScaleToPosition(spine);
                renderer.drawCubeShaded(renderer.getCubeVertices(spine, burst, 0), cam, body.brighter());
            } else if (growthPhase == 3) {
                int spark = applyScaleToSize((int)(voxelSize * (1.2 + tp)));
                for (int i = 0; i < 3; i++) {
                    Vector3 sp = applyTransform(new Vector3(0, voxelSize * (0.6 + i * 0.5), voxelSize * (0.2 - i * 0.4)));
                    sp = applyScaleToPosition(sp);
                    renderer.drawCubeShaded(renderer.getCubeVertices(sp, spark, 0), cam, body.brighter().brighter());
                }
            }
        }

        // Cuerpo base
        for (Vector3 v : voxels) {
            Vector3 wp = applyTransform(new Vector3(v.x * voxelSize, v.y * voxelSize, v.z * voxelSize));
            wp = applyScaleToPosition(wp);
            int s = applyScaleToSize(voxelSize);
            renderer.drawCubeShaded(renderer.getCubeVertices(wp, s, 0), cam, body);
        }

        // Espinas (más numerosas en fases avanzadas)
        int spikeCount = (apex ? 18 : (evolved ? 8 : 4)) + growthPhase * 3;
        int spikeSize = Math.max(1, applyScaleToSize((int)(voxelSize * (apex ? 0.95 : (evolved ? 0.7 : 0.5)))));
        Color spikeColor = body.brighter().brighter();
        Random r = new Random(seed);
        for (int i = 0; i < spikeCount; i++) {
            double angle = (i / (double)spikeCount) * Math.PI * 2;
            double radius = voxelSize * 1.2;
            Vector3 spike = applyTransform(new Vector3(
                Math.cos(angle) * radius,
                voxelSize * 0.5 + r.nextDouble() * voxelSize,
                Math.sin(angle) * radius
            ));
            spike = applyScaleToPosition(spike);
            renderer.drawCubeShaded(renderer.getCubeVertices(spike, spikeSize, 0), cam, spikeColor);
        }

        // Placas laterales solo fase 2/3
        if (evolved) {
            int plate = applyScaleToSize((int)(voxelSize * 0.8));
            Color plateC = body.darker();
            Vector3 lp = applyTransform(new Vector3(-voxelSize * 1.2, voxelSize * 0.6, 0));
            Vector3 rp = applyTransform(new Vector3(voxelSize * 1.2, voxelSize * 0.6, 0));
            lp = applyScaleToPosition(lp); rp = applyScaleToPosition(rp);
            renderer.drawCubeShaded(renderer.getCubeVertices(lp, plate, 0), cam, plateC);
            renderer.drawCubeShaded(renderer.getCubeVertices(rp, plate, 0), cam, plateC);
        }

        // Espinas dobles y corona dorsal en fase 3
        if (apex) {
            int crownSize = applyScaleToSize((int)(voxelSize * 1.0));
            Color crownC = body.brighter();
            for (int i = 0; i < 4; i++) {
                Vector3 cp = applyTransform(new Vector3(0, voxelSize * (0.8 + i * 0.5), voxelSize * (0.2 - i * 0.5)));
                cp = applyScaleToPosition(cp);
                renderer.drawCubeShaded(renderer.getCubeVertices(cp, crownSize, 0), cam, crownC);
            }
        }

        // Ojos
        int eyeSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.4)));
        Color eyeW = new Color(240, 240, 240);
        Color pupil = new Color(80, 20, 20);
        Vector3 eyeL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 1.3, voxelSize * 1.5));
        eyeL = applyScaleToPosition(eyeL);
        Vector3 eyeR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 1.3, voxelSize * 1.5));
        eyeR = applyScaleToPosition(eyeR);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeL, eyeSize, 0), cam, eyeW);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeR, eyeSize, 0), cam, eyeW);
        int pup = Math.max(1, (int)(eyeSize * 0.5 * (1.0 - animController.getBlinkAmount())));
        Vector3 pupilL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 1.3, voxelSize * 1.5 + pup));
        pupilL = applyScaleToPosition(pupilL);
        Vector3 pupilR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 1.3, voxelSize * 1.5 + pup));
        pupilR = applyScaleToPosition(pupilR);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilL, pup, 0), cam, pupil);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilR, pup, 0), cam, pupil);

        // Boca
        int mouthSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.35)));
        double jaw = animController.getJawOpen() * (apex ? 1.6 : 1.2);
        Vector3 mouth = applyTransform(new Vector3(0, voxelSize * 0.8 + jaw * 0.3, voxelSize * 1.8 + jaw));
        mouth = applyScaleToPosition(mouth);
        renderer.drawCubeShaded(renderer.getCubeVertices(mouth, mouthSize, 0), cam, new Color(100, 20, 20));

        if (apex) {
            int fang = Math.max(1, mouthSize / 2);
            Vector3 fangL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 0.6 + jaw * 0.2, voxelSize * 1.9 + jaw));
            Vector3 fangR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 0.6 + jaw * 0.2, voxelSize * 1.9 + jaw));
            fangL = applyScaleToPosition(fangL); fangR = applyScaleToPosition(fangR);
            renderer.drawCubeShaded(renderer.getCubeVertices(fangL, fang, 0), cam, spikeColor);
            renderer.drawCubeShaded(renderer.getCubeVertices(fangR, fang, 0), cam, spikeColor);
        }

        // Patas en dos segmentos con movimiento marcado
        double legAmp = evolved ? (apex ? 0.85 : 0.65) : 0.45;
        double frontLeftLeg = Math.sin(walkPhase) * voxelSize * legAmp;
        double frontRightLeg = Math.sin(walkPhase + Math.PI) * voxelSize * legAmp;
        double backLeftLeg = Math.sin(walkPhase + Math.PI) * voxelSize * legAmp;
        double backRightLeg = Math.sin(walkPhase) * voxelSize * legAmp;
        int upper = applyScaleToSize((int)(voxelSize * 0.9));
        int lower = applyScaleToSize((int)(voxelSize * 0.8));
        Color paw = body.darker();

        Vector3 flUp = applyTransform(new Vector3(-voxelSize, -voxelSize * 0.3 + Math.abs(frontLeftLeg) * 0.4, voxelSize + frontLeftLeg * 0.5));
        flUp = applyScaleToPosition(flUp);
        Vector3 flLo = applyTransform(new Vector3(-voxelSize, -voxelSize * 0.9 + Math.abs(frontLeftLeg) * 0.2, voxelSize * 0.6 + frontLeftLeg * 0.3));
        flLo = applyScaleToPosition(flLo);
        renderer.drawCubeShaded(renderer.getCubeVertices(flUp, upper, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(flLo, lower, 0), cam, paw);

        Vector3 frUp = applyTransform(new Vector3(voxelSize, -voxelSize * 0.3 + Math.abs(frontRightLeg) * 0.4, voxelSize + frontRightLeg * 0.5));
        frUp = applyScaleToPosition(frUp);
        Vector3 frLo = applyTransform(new Vector3(voxelSize, -voxelSize * 0.9 + Math.abs(frontRightLeg) * 0.2, voxelSize * 0.6 + frontRightLeg * 0.3));
        frLo = applyScaleToPosition(frLo);
        renderer.drawCubeShaded(renderer.getCubeVertices(frUp, upper, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(frLo, lower, 0), cam, paw);

        Vector3 blUp = applyTransform(new Vector3(-voxelSize, -voxelSize * 0.3 + Math.abs(backLeftLeg) * 0.4, -voxelSize + backLeftLeg * 0.5));
        blUp = applyScaleToPosition(blUp);
        Vector3 blLo = applyTransform(new Vector3(-voxelSize, -voxelSize * 0.9 + Math.abs(backLeftLeg) * 0.2, -voxelSize * 1.3 + backLeftLeg * 0.3));
        blLo = applyScaleToPosition(blLo);
        renderer.drawCubeShaded(renderer.getCubeVertices(blUp, upper, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(blLo, lower, 0), cam, paw);

        Vector3 brUp = applyTransform(new Vector3(voxelSize, -voxelSize * 0.3 + Math.abs(backRightLeg) * 0.4, -voxelSize + backRightLeg * 0.5));
        brUp = applyScaleToPosition(brUp);
        Vector3 brLo = applyTransform(new Vector3(voxelSize, -voxelSize * 0.9 + Math.abs(backRightLeg) * 0.2, -voxelSize * 1.3 + backRightLeg * 0.3));
        brLo = applyScaleToPosition(brLo);
        renderer.drawCubeShaded(renderer.getCubeVertices(brUp, upper, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(brLo, lower, 0), cam, paw);

        // Placas de tobillo brillantes solo fase 3
        if (apex) {
            int ank = Math.max(1, lower / 2);
            Color ankC = body.brighter();
            Vector3[] ankles = new Vector3[]{flLo, frLo, blLo, brLo};
            for (Vector3 a : ankles) {
                Vector3 ap = applyScaleToPosition(new Vector3(a.x, a.y, a.z));
                renderer.drawCubeShaded(renderer.getCubeVertices(ap, ank, 0), cam, ankC);
            }
        }
    }

    @Override
    protected double getPhaseDuration(int phase) {
        // Total: 180 segundos (3 minutos)
        // Fase 1: 70s, Fase 2: 60s, Fase 3: 50s
        switch (phase) {
            case 1: return 70.0;
            case 2: return 60.0;
            case 3: return 50.0;
            default: return 60.0;
        }
    }
    
    @Override
    public int getSpeciesType() {
        return 4;
    }
    
    @Override
    protected void applyPhaseVisuals() {
        // Fase 2: Tonos más rojizos
        if (growthPhase == 2) {
            int r = Math.min(255, (int)(originalColor.getRed() * 1.3));
            int g = (int)(originalColor.getGreen() * 0.95);
            int b = (int)(originalColor.getBlue() * 0.9);
            this.color = new Color(r, g, b);
        }
        // Fase 3: Marrón oscuro y rojizo
        else if (growthPhase == 3) {
            int r = Math.min(255, (int)(originalColor.getRed() * 1.4));
            int g = (int)(originalColor.getGreen() * 0.8);
            int b = (int)(originalColor.getBlue() * 0.7);
            this.color = new Color(r, g, b);
        }
    }

    @Override
    public String getSpeciesName() {
        return "Erizo Carmesí";
    }
}
