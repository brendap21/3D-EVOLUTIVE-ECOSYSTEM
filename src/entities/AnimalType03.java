package entities;

import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * AnimalType03: Criatura voluminosa cuadrúpeda con cuernos y cola gruesa.
 * Paleta fase 1 (3 colores): púrpura oscuro, violeta, azul profundo.
 * Fase 2-3 crece en tamaño, cuernos más largos, movimiento pesado.
 */
public class AnimalType03 extends BaseAnimal {
    private double walkPhase = 0.0;
    private AnimationController animController;

    public AnimalType03(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxels = new ArrayList<>();
        this.animController = new AnimationController();
        generateFromSeed(seed);
        initializeSpawnAnimation();
    }

    private void generateFromSeed(long seed) {
        Random r = new Random(seed);
        this.voxelSize = 4 + r.nextInt(2); // Más grande inicialmente
        this.baseVoxelSize = voxelSize;
        Color[] palette = new Color[]{
            new Color(90, 50, 130),
            new Color(110, 70, 160),
            new Color(60, 70, 140)
        };
        this.color = palette[r.nextInt(palette.length)];
        this.originalColor = color;
        this.baseSpeed = 0.9 + r.nextDouble() * 0.4; // Más lento, pesado

        // Cuerpo robusto
        voxels.add(new Vector3(0, 0, 0));
        voxels.add(new Vector3(1, 0, 0));
        voxels.add(new Vector3(-1, 0, 0));
        voxels.add(new Vector3(0, 0, 1));
        voxels.add(new Vector3(0, 0, -1));
        voxels.add(new Vector3(0, 1, 0));

        // Cabeza
        voxels.add(new Vector3(0, 1, 2));
    }

    @Override
    protected void renderNormal(SoftwareRenderer renderer, Camera cam) {
        animController.update(0.016);
        animController.setBlinkFrequency(2.8);
        animController.setTailWagFrequency(1.2);
        walkPhase += 0.14 * getPhaseSpeedMultiplier();
        boolean evolved = growthPhase >= 2;
        boolean apex = growthPhase == 3;
        Color body = applyGlowToColor(color);

        // Transición de fase: ondas de choque en cascos
        double tp = transitionPulse;
        if (tp > 0) {
            if (growthPhase == 2) {
                int shock = applyScaleToSize((int)(voxelSize * (1.0 + tp)));
                Vector3 stompL = applyTransform(new Vector3(-voxelSize, -voxelSize * 1.0, voxelSize * 0.6));
                Vector3 stompR = applyTransform(new Vector3(voxelSize, -voxelSize * 1.0, voxelSize * 0.6));
                stompL = applyScaleToPosition(stompL); stompR = applyScaleToPosition(stompR);
                renderer.drawCubeShaded(renderer.getCubeVertices(stompL, shock, 0), cam, body.brighter());
                renderer.drawCubeShaded(renderer.getCubeVertices(stompR, shock, 0), cam, body.brighter());
            } else if (growthPhase == 3) {
                int quake = applyScaleToSize((int)(voxelSize * (1.3 + tp)));
                Vector3 center = applyTransform(new Vector3(0, -voxelSize * 0.8, 0));
                center = applyScaleToPosition(center);
                renderer.drawCubeShaded(renderer.getCubeVertices(center, quake, 0), cam, body.brighter().brighter());
            }
        }

        // Cuerpo base
        double stomp = evolved ? Math.sin(walkPhase * 0.8) * voxelSize * (apex ? 0.55 : 0.4) : 0.0;
        for (Vector3 v : voxels) {
            Vector3 wp = applyTransform(new Vector3(v.x * voxelSize, v.y * voxelSize + stomp, v.z * voxelSize));
            wp = applyScaleToPosition(wp);
            int s = applyScaleToSize(voxelSize);
            renderer.drawCubeShaded(renderer.getCubeVertices(wp, s, 0), cam, body);
        }

        // Placas dorsales solo fase 2/3
        if (evolved) {
            int plate = applyScaleToSize((int)(voxelSize * 0.9));
            Color plateC = body.brighter();
            for (int i = -1; i <= 1; i++) {
                Vector3 pp = applyTransform(new Vector3(0, voxelSize * 1.3 + stomp, voxelSize * i));
                pp = applyScaleToPosition(pp);
                renderer.drawCubeShaded(renderer.getCubeVertices(pp, plate, 0), cam, plateC);
            }
        }

        // Placas masivas y hombreras en fase 3
        if (apex) {
            int megaPlate = applyScaleToSize((int)(voxelSize * 1.2));
            Color plateC = body.brighter().brighter();
            Vector3 shoulderL = applyTransform(new Vector3(-voxelSize * 1.4, voxelSize * 1.0 + stomp, voxelSize * 0.3));
            Vector3 shoulderR = applyTransform(new Vector3(voxelSize * 1.4, voxelSize * 1.0 + stomp, voxelSize * 0.3));
            shoulderL = applyScaleToPosition(shoulderL); shoulderR = applyScaleToPosition(shoulderR);
            renderer.drawCubeShaded(renderer.getCubeVertices(shoulderL, megaPlate, 0), cam, plateC);
            renderer.drawCubeShaded(renderer.getCubeVertices(shoulderR, megaPlate, 0), cam, plateC);

            int backSpine = applyScaleToSize((int)(voxelSize * 1.0));
            for (int i = 0; i < 3; i++) {
                Vector3 spine = applyTransform(new Vector3(0, voxelSize * (1.6 + i * 0.6) + stomp, -voxelSize * (0.2 + i * 0.8)));
                spine = applyScaleToPosition(spine);
                renderer.drawCubeShaded(renderer.getCubeVertices(spine, backSpine, 0), cam, plateC);
            }
        }

        // Cuernos (crecen con la fase)
        int hornLen = evolved ? (apex ? growthPhase + 2 : growthPhase + 1) : growthPhase;
        int hornSize = Math.max(1, applyScaleToSize((int)(voxelSize * (apex ? 0.9 : (evolved ? 0.7 : 0.5)))));
        for (int i = 0; i < hornLen; i++) {
            Vector3 hornL = applyTransform(new Vector3(-voxelSize * 0.7, voxelSize * (2.0 + i * 0.8), voxelSize * 2.0));
            hornL = applyScaleToPosition(hornL);
            Vector3 hornR = applyTransform(new Vector3(voxelSize * 0.7, voxelSize * (2.0 + i * 0.8), voxelSize * 2.0));
            hornR = applyScaleToPosition(hornR);
            renderer.drawCubeShaded(renderer.getCubeVertices(hornL, hornSize, 0), cam, body.brighter());
            renderer.drawCubeShaded(renderer.getCubeVertices(hornR, hornSize, 0), cam, body.brighter());
        }

        // Ojos
        int eyeSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.4)));
        Color eyeW = new Color(250, 250, 250);
        Color pupil = new Color(40, 20, 60);
        Vector3 eyeL = applyTransform(new Vector3(-voxelSize * 0.5, voxelSize * 1.5, voxelSize * 2.5));
        eyeL = applyScaleToPosition(eyeL);
        Vector3 eyeR = applyTransform(new Vector3(voxelSize * 0.5, voxelSize * 1.5, voxelSize * 2.5));
        eyeR = applyScaleToPosition(eyeR);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeL, eyeSize, 0), cam, eyeW);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeR, eyeSize, 0), cam, eyeW);
        int pup = Math.max(1, (int)(eyeSize * 0.5 * (1.0 - animController.getBlinkAmount())));
        Vector3 pupilL = applyTransform(new Vector3(-voxelSize * 0.5, voxelSize * 1.5, voxelSize * 2.5 + pup));
        pupilL = applyScaleToPosition(pupilL);
        Vector3 pupilR = applyTransform(new Vector3(voxelSize * 0.5, voxelSize * 1.5, voxelSize * 2.5 + pup));
        pupilR = applyScaleToPosition(pupilR);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilL, pup, 0), cam, pupil);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilR, pup, 0), cam, pupil);

        // Boca/hocico
        int mouthSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.4)));
        Vector3 mouth = applyTransform(new Vector3(0, voxelSize * 0.8, voxelSize * 2.8));
        mouth = applyScaleToPosition(mouth);
        renderer.drawCubeShaded(renderer.getCubeVertices(mouth, mouthSize, 0), cam, new Color(70, 30, 50));

        // Cola gruesa (más larga en fases avanzadas)
        int tailLen = apex ? 4 : 1 + growthPhase;
        int tailSize = applyScaleToSize((int)(voxelSize * (apex ? 1.0 : 0.9)));
        for (int i = 0; i < tailLen; i++) {
            double wag = animController.getTailWagOffset(i) * (apex ? 0.9 : 0.6);
            double wagV = animController.getTailWagVertical(i) * (apex ? 0.6 : 0.4);
            Vector3 tpos = applyTransform(new Vector3(wag, voxelSize * 0.5 + wagV, -voxelSize * (1.5 + i)));
            tpos = applyScaleToPosition(tpos);
            Color tailC = apex && i == tailLen - 1 ? body.brighter() : body.darker();
            renderer.drawCubeShaded(renderer.getCubeVertices(tpos, tailSize, 0), cam, tailC);
        }

        // Patas con movimiento pesado en DOS segmentos
        double legAmp = evolved ? (apex ? 0.75 : 0.55) : 0.35;
        double frontLeftLeg = Math.sin(walkPhase) * voxelSize * legAmp;
        double frontRightLeg = Math.sin(walkPhase + Math.PI) * voxelSize * legAmp;
        double backLeftLeg = Math.sin(walkPhase + Math.PI) * voxelSize * legAmp;
        double backRightLeg = Math.sin(walkPhase) * voxelSize * legAmp;
        int upperLeg = applyScaleToSize((int)(voxelSize * 1.0));
        int lowerLeg = applyScaleToSize((int)(voxelSize * 0.9));
        Color paw = body.darker().darker();

        Vector3 flUp = applyTransform(new Vector3(-voxelSize, -voxelSize * 0.3 + Math.abs(frontLeftLeg) * 0.3, voxelSize + frontLeftLeg * 0.5));
        flUp = applyScaleToPosition(flUp);
        Vector3 flLo = applyTransform(new Vector3(-voxelSize, -voxelSize * 0.9 + Math.abs(frontLeftLeg) * 0.2, voxelSize * 0.6 + frontLeftLeg * 0.3));
        flLo = applyScaleToPosition(flLo);
        renderer.drawCubeShaded(renderer.getCubeVertices(flUp, upperLeg, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(flLo, lowerLeg, 0), cam, paw);

        Vector3 frUp = applyTransform(new Vector3(voxelSize, -voxelSize * 0.3 + Math.abs(frontRightLeg) * 0.3, voxelSize + frontRightLeg * 0.5));
        frUp = applyScaleToPosition(frUp);
        Vector3 frLo = applyTransform(new Vector3(voxelSize, -voxelSize * 0.9 + Math.abs(frontRightLeg) * 0.2, voxelSize * 0.6 + frontRightLeg * 0.3));
        frLo = applyScaleToPosition(frLo);
        renderer.drawCubeShaded(renderer.getCubeVertices(frUp, upperLeg, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(frLo, lowerLeg, 0), cam, paw);

        Vector3 blUp = applyTransform(new Vector3(-voxelSize, -voxelSize * 0.3 + Math.abs(backLeftLeg) * 0.3, -voxelSize + backLeftLeg * 0.5));
        blUp = applyScaleToPosition(blUp);
        Vector3 blLo = applyTransform(new Vector3(-voxelSize, -voxelSize * 0.9 + Math.abs(backLeftLeg) * 0.2, -voxelSize * 1.3 + backLeftLeg * 0.3));
        blLo = applyScaleToPosition(blLo);
        renderer.drawCubeShaded(renderer.getCubeVertices(blUp, upperLeg, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(blLo, lowerLeg, 0), cam, paw);

        Vector3 brUp = applyTransform(new Vector3(voxelSize, -voxelSize * 0.3 + Math.abs(backRightLeg) * 0.3, -voxelSize + backRightLeg * 0.5));
        brUp = applyScaleToPosition(brUp);
        Vector3 brLo = applyTransform(new Vector3(voxelSize, -voxelSize * 0.9 + Math.abs(backRightLeg) * 0.2, -voxelSize * 1.3 + backRightLeg * 0.3));
        brLo = applyScaleToPosition(brLo);
        renderer.drawCubeShaded(renderer.getCubeVertices(brUp, upperLeg, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(brLo, lowerLeg, 0), cam, paw);

        // Golpe de casco brillante en fase 3
        if (apex) {
            int stompMark = Math.max(1, lowerLeg / 2);
            Color stompC = body.brighter();
            Vector3 markL = applyTransform(new Vector3(-voxelSize, -voxelSize * 1.0 + Math.abs(frontLeftLeg) * 0.2, voxelSize * 0.6 + frontLeftLeg * 0.3));
            Vector3 markR = applyTransform(new Vector3(voxelSize, -voxelSize * 1.0 + Math.abs(frontRightLeg) * 0.2, voxelSize * 0.6 + frontRightLeg * 0.3));
            markL = applyScaleToPosition(markL); markR = applyScaleToPosition(markR);
            renderer.drawCubeShaded(renderer.getCubeVertices(markL, stompMark, 0), cam, stompC);
            renderer.drawCubeShaded(renderer.getCubeVertices(markR, stompMark, 0), cam, stompC);
        }
    }

    @Override
    protected double getPhaseDuration() {
        return 40.0; // 40 segundos por fase
    }
    
    @Override
    protected void applyPhaseVisuals() {
        // Fase 2: Colores más saturados
        if (growthPhase == 2) {
            int r = Math.min(255, (int)(originalColor.getRed() * 1.25));
            int g = (int)(originalColor.getGreen() * 0.95);
            int b = Math.min(255, (int)(originalColor.getBlue() * 1.2));
            this.color = new Color(r, g, b);
        }
        // Fase 3: Colores oscuros y profundos
        else if (growthPhase == 3) {
            int r = (int)(originalColor.getRed() * 0.8);
            int g = (int)(originalColor.getGreen() * 0.7);
            int b = Math.min(255, (int)(originalColor.getBlue() * 1.3));
            this.color = new Color(r, g, b);
        }
    }

    @Override
    public String getSpeciesName() {
        return "Toro Púrpura";
    }
}
