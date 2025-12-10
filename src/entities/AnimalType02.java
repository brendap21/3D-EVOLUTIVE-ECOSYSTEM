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
 * 
 * ANIMACIONES:
 * - Marcha bípeda realista con balanceo de cuerpo
 * - Movimiento de alas/brazos coordinados (opuestos a las patas)
 * - Movimiento de mandíbula (pico)
 * - Parpadeo expresivo
 * - Cresta que se alza defensivamente
 */
public class AnimalType02 extends BaseAnimal {
    private double animPhase = 0.0;
    private AnimationController animController;

    public AnimalType02(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxels = new ArrayList<>();
        this.animController = new AnimationController();
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

    // Cuerpo - SIN PATAS NI BRAZOS (se dibujan con animación)
        voxels.add(new Vector3(0, 0, 0));
        voxels.add(new Vector3(0, 1, 0));
        // Cabeza grande
        voxels.add(new Vector3(0, 2, 0));
        voxels.add(new Vector3(0, 3, 0));
    }

    @Override
    protected void renderNormal(SoftwareRenderer renderer, Camera cam) {
        animController.update(0.016);
        animController.setBlinkFrequency(4.0); // Parpadeo muy frecuente
        animController.setJawFrequency(3.5); // Boca muy activa
        animController.setBodySwayFrequency(2.0); // Balanceo pronunciado
        
        animPhase += 0.3 * getPhaseSpeedMultiplier(); // Animación más rápida
        boolean evolved = growthPhase >= 2;
        boolean apex = growthPhase == 3;
        Color body = applyGlowToColor(color);

        // Transición de fase: flare en cresta y pecho
        double tp = transitionPulse;
        if (tp > 0) {
            if (growthPhase == 2) {
                int flare = applyScaleToSize((int)(voxelSize * (1.0 + tp)));
                Vector3 cpos = applyTransform(new Vector3(0, voxelSize * (3.8 + tp * 1.5), voxelSize * 0.2));
                cpos = applyScaleToPosition(cpos);
                renderer.drawCubeShaded(renderer.getCubeVertices(cpos, flare, 0), cam, body.brighter());
            } else if (growthPhase == 3) {
                int core = applyScaleToSize((int)(voxelSize * (1.1 + tp)));
                Vector3 corePos = applyTransform(new Vector3(0, voxelSize * 1.0, 0));
                corePos = applyScaleToPosition(corePos);
                renderer.drawCubeShaded(renderer.getCubeVertices(corePos, core, 0), cam, body.brighter().brighter());
            }
        }

    // Cuerpo base con balanceo corporal EXAGERADO
    double bodySway = animController.getBodySway() * (apex ? 3.8 : (evolved ? 3.0 : 2.0)); // más sway en fase 3
        for (Vector3 v : voxels) {
            Vector3 wp = applyTransform(new Vector3(
                v.x * voxelSize + bodySway * voxelSize * 0.4, // Balanceo muy visible
                v.y * voxelSize,
                v.z * voxelSize
            ));
            wp = applyScaleToPosition(wp);
            int s = applyScaleToSize(voxelSize);
            renderer.drawCubeShaded(renderer.getCubeVertices(wp, s, 0), cam, body);
        }

        // Cresta defensiva (más alta en fase 2)
        int crestSize = Math.max(1, applyScaleToSize((int)(voxelSize * (apex ? 1.0 : (evolved ? 0.8 : 0.6)))));
        double crestRise = Math.abs(Math.sin(animPhase)) * voxelSize * (apex ? 0.9 : (evolved ? 0.6 : 0.3));
        int crestSegments = apex ? 6 : (evolved ? 4 : 2);
        for (int i = 0; i < crestSegments; i++) {
            Vector3 cpos = applyTransform(new Vector3(0, voxelSize * (3.5 + i) + crestRise, voxelSize * 0.2));
            cpos = applyScaleToPosition(cpos);
            renderer.drawCubeShaded(renderer.getCubeVertices(cpos, crestSize, 0), cam, body.darker());
        }

        // Penacho incandescente fase 3
        if (apex) {
            int flame = Math.max(1, crestSize / 2);
            for (int i = 0; i < 3; i++) {
                Vector3 fpos = applyTransform(new Vector3(0, voxelSize * (5.2 + i * 0.6) + crestRise * 1.2, voxelSize * 0.2));
                fpos = applyScaleToPosition(fpos);
                renderer.drawCubeShaded(renderer.getCubeVertices(fpos, flame, 0), cam, body.brighter().brighter());
            }
        }

        // Ojos con parpadeo expresivo
        int eye = Math.max(1, applyScaleToSize((int)(voxelSize * 0.35)));
        Color eyeW = new Color(245, 245, 245);
        Color pupil = new Color(30, 30, 40);
        double blinkAmount = animController.getBlinkAmount();
        
        Vector3 eyeL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 2.6, voxelSize * 0.9));
        eyeL = applyScaleToPosition(eyeL);
        Vector3 eyeR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 2.6, voxelSize * 0.9));
        eyeR = applyScaleToPosition(eyeR);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeL, eye, 0), cam, eyeW);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeR, eye, 0), cam, eyeW);
        
        int pup = Math.max(1, (int)(eye / 2 * (1.0 - blinkAmount)));
        if (pup > 0) {
            Vector3 pupilL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 2.6, voxelSize * 0.9 + pup));
            pupilL = applyScaleToPosition(pupilL);
            Vector3 pupilR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 2.6, voxelSize * 0.9 + pup));
            pupilR = applyScaleToPosition(pupilR);
            renderer.drawCubeShaded(renderer.getCubeVertices(pupilL, pup, 0), cam, pupil);
            renderer.drawCubeShaded(renderer.getCubeVertices(pupilR, pup, 0), cam, pupil);
        }

        // Pico/boca que abre y cierra - EXAGERADO
        double jawOpen = animController.getJawOpen() * (evolved ? 2.5 : 2.0); // AMPLIFICADO
        int beak = Math.max(1, applyScaleToSize((int)(voxelSize * (0.5 + jawOpen * 0.6))));
        Vector3 beakPos = applyTransform(new Vector3(0, voxelSize * 2.1, voxelSize * 1.2 + jawOpen * 1.5));
        beakPos = applyScaleToPosition(beakPos);
        renderer.drawCubeShaded(renderer.getCubeVertices(beakPos, beak, 0), cam, new Color(240, 180, 60));

    // Patas bípedas con articulación EXAGERADA
    double legAmp = evolved ? (apex ? 1.4 : 1.2) : 0.9;
    double leftLegPhase = Math.sin(animPhase) * voxelSize * legAmp;
    double rightLegPhase = Math.sin(animPhase + Math.PI) * voxelSize * legAmp;
    int upperLegSize = applyScaleToSize((int)(voxelSize * 0.9));
    int lowerLegSize = applyScaleToSize((int)(voxelSize * 0.8));
        
        // Articulation effect: estiramiento/compresión EXAGERADO
        double legBend = Math.sin(animPhase * 2) * (evolved ? 0.55 : 0.4);
        
        // Pata izquierda
        Vector3 leftUpper = applyTransform(new Vector3(
            -voxelSize * 0.5,
            -voxelSize * 0.5 + Math.abs(leftLegPhase) * 0.4 + legBend * voxelSize * 0.4,
            leftLegPhase * 0.6
        ));
        leftUpper = applyScaleToPosition(leftUpper);
        Vector3 leftLower = applyTransform(new Vector3(
            -voxelSize * 0.55,
            -voxelSize * 1.0 + Math.abs(leftLegPhase) * 0.3 + legBend * voxelSize * 0.3,
            leftLegPhase * 0.4
        ));
        leftLower = applyScaleToPosition(leftLower);
        renderer.drawCubeShaded(renderer.getCubeVertices(leftUpper, upperLegSize, 0), cam, body.darker());
        renderer.drawCubeShaded(renderer.getCubeVertices(leftLower, lowerLegSize, 0), cam, body.darker());
        
        // Pata derecha
        Vector3 rightUpper = applyTransform(new Vector3(
            voxelSize * 0.5,
            -voxelSize * 0.5 + Math.abs(rightLegPhase) * 0.4 - legBend * voxelSize * 0.4,
            rightLegPhase * 0.6
        ));
        rightUpper = applyScaleToPosition(rightUpper);
        Vector3 rightLower = applyTransform(new Vector3(
            voxelSize * 0.55,
            -voxelSize * 1.0 + Math.abs(rightLegPhase) * 0.3 - legBend * voxelSize * 0.3,
            rightLegPhase * 0.4
        ));
        rightLower = applyScaleToPosition(rightLower);
        renderer.drawCubeShaded(renderer.getCubeVertices(rightUpper, upperLegSize, 0), cam, body.darker());
        renderer.drawCubeShaded(renderer.getCubeVertices(rightLower, lowerLegSize, 0), cam, body.darker());
        
        // Brazos/alas coordinados con patas (opuestos para balance) - EXAGERADO
        double wingOff = Math.sin(animPhase) * voxelSize * (apex ? 1.3 : (evolved ? 1.0 : 0.7));
        int wingUpperSize = applyScaleToSize((int)(voxelSize * (apex ? 1.25 : (evolved ? 1.05 : 0.9))));
        int wingLowerSize = applyScaleToSize((int)(voxelSize * (apex ? 1.1 : (evolved ? 0.95 : 0.8))));
        
        Vector3 wingLUpper = applyTransform(new Vector3(
            -voxelSize * 1.2,
            voxelSize * 1.4 + wingOff * 1.4,
            evolved ? voxelSize * 0.3 : 0
        ));
        wingLUpper = applyScaleToPosition(wingLUpper);
        Vector3 wingLLower = applyTransform(new Vector3(
            -voxelSize * 1.6,
            voxelSize * 1.0 + wingOff * 1.7,
            0.4 * voxelSize
        ));
        wingLLower = applyScaleToPosition(wingLLower);
        Vector3 wingRUpper = applyTransform(new Vector3(
            voxelSize * 1.2,
            voxelSize * 1.4 - wingOff * 1.4,
            evolved ? -voxelSize * 0.3 : 0
        ));
        wingRUpper = applyScaleToPosition(wingRUpper);
        Vector3 wingRLower = applyTransform(new Vector3(
            voxelSize * 1.6,
            voxelSize * 1.0 - wingOff * 1.7,
            0.4 * voxelSize
        ));
        wingRLower = applyScaleToPosition(wingRLower);
        renderer.drawCubeShaded(renderer.getCubeVertices(wingLUpper, wingUpperSize, 0), cam, body.darker());
        renderer.drawCubeShaded(renderer.getCubeVertices(wingLLower, wingLowerSize, 0), cam, body.darker());
        renderer.drawCubeShaded(renderer.getCubeVertices(wingRUpper, wingUpperSize, 0), cam, body.darker());
        renderer.drawCubeShaded(renderer.getCubeVertices(wingRLower, wingLowerSize, 0), cam, body.darker());

        // Repliegue de fuego en bordes de alas solo fase 3
        if (apex) {
            int tip = Math.max(1, wingLowerSize / 2);
            Color ember = body.brighter();
            Vector3 tipL = applyTransform(new Vector3(-voxelSize * 1.8, voxelSize * 0.9 + wingOff * 1.8, voxelSize * 0.6));
            Vector3 tipR = applyTransform(new Vector3(voxelSize * 1.8, voxelSize * 0.9 - wingOff * 1.8, voxelSize * 0.6));
            tipL = applyScaleToPosition(tipL); tipR = applyScaleToPosition(tipR);
            renderer.drawCubeShaded(renderer.getCubeVertices(tipL, tip, 0), cam, ember);
            renderer.drawCubeShaded(renderer.getCubeVertices(tipR, tip, 0), cam, ember);
        }

        // Plumas laterales extra solo fase 2/3
        if (evolved) {
            int feather = applyScaleToSize((int)(voxelSize * 0.7));
            Color featherC = body.brighter();
            Vector3 lf = applyTransform(new Vector3(-voxelSize * 1.0, voxelSize * 0.8, -voxelSize * 0.6));
            Vector3 rf = applyTransform(new Vector3(voxelSize * 1.0, voxelSize * 0.8, -voxelSize * 0.6));
            lf = applyScaleToPosition(lf); rf = applyScaleToPosition(rf);
            renderer.drawCubeShaded(renderer.getCubeVertices(lf, feather, 0), cam, featherC);
            renderer.drawCubeShaded(renderer.getCubeVertices(rf, feather, 0), cam, featherC);
        }

        // Núcleo incandescente en abdomen en fase 3
        if (apex) {
            int core = applyScaleToSize((int)(voxelSize * 0.9));
            Vector3 corePos = applyTransform(new Vector3(0, voxelSize * 0.2 + bodySway * 0.2, -voxelSize * 0.4));
            corePos = applyScaleToPosition(corePos);
            renderer.drawCubeShaded(renderer.getCubeVertices(corePos, core, 0), cam, body.brighter().brighter());
        }
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
