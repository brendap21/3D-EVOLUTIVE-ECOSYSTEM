package entities;

import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * AnimalType01: Felino ágil cuadrúpedo con orejas puntiagudas y cola.
 * Paleta fase 1 (3 colores posibles): teal, menta, azul verdoso.
 * Fase 2-3 crece en tamaño, más velocidad y cola más larga.
 * 
 * ANIMACIONES:
 * - Caminar cuadrúpedo realista: patas diagonales coordinadas
 * - Movimiento de cabeza siguiendo la dirección del movimiento
 * - Cola ondulante con movimiento lateral
 * - Parpadeo natural de ojos
 * - Movimiento de orejas reactivo
 */
public class AnimalType01 extends BaseAnimal {
    private double walkPhase = 0.0;
    private AnimationController animController;

    public AnimalType01(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxels = new ArrayList<>();
        this.animController = new AnimationController();
        generateFromSeed(seed);
        initializeSpawnAnimation();
    }

    private void generateFromSeed(long seed) {
        Random r = new Random(seed);
        this.voxelSize = 3 + r.nextInt(2); // MÁS pequeño (3-4 en lugar de 6-8)
        this.baseVoxelSize = voxelSize;
        Color[] palette = new Color[]{
            new Color(60, 180, 170),
            new Color(40, 200, 140),
            new Color(70, 170, 200)
        };
        this.color = palette[r.nextInt(palette.length)];
        this.originalColor = this.color;
        this.baseSpeed = 1.6 + r.nextDouble() * 0.6;

    // Voxels base (torso compacto) - SIN PATAS (se dibujan con animación)
        voxels.add(new Vector3(0, 0, 0));
        voxels.add(new Vector3(1, 0, 0));
        voxels.add(new Vector3(-1, 0, 0));
        voxels.add(new Vector3(0, 1, 0));

        // Cabeza
        voxels.add(new Vector3(0, 2, 1));

        // Cola base
        voxels.add(new Vector3(0, 1, -1));
    }

    @Override
    protected void renderNormal(SoftwareRenderer renderer, Camera cam) {
        // Actualizar controller de animaciones
        animController.update(0.016); // ~60 FPS
        animController.setBlinkFrequency(3.0); // Parpadeo más frecuente
        animController.setTailWagFrequency(3.5); // Cola más activa
        animController.setHeadTurnFrequency(1.5);
        animController.setEarTwitchFrequency(5.0); // Orejas más activas
        
        walkPhase += 0.25 * getPhaseSpeedMultiplier(); // Animación más rápida y visible
        boolean evolved = growthPhase >= 2;
        boolean apex = growthPhase == 3;
        Color bodyColor = applyGlowToColor(color);

        // Transición de fase: destellos en lomo y cola
        double tp = transitionPulse;
        if (tp > 0) {
            if (growthPhase == 2) {
                int flash = applyScaleToSize((int)(voxelSize * (1.2 + tp)));
                Vector3 fpos = applyTransform(new Vector3(0, voxelSize * (1.0 + tp * 2.0), 0));
                fpos = applyScaleToPosition(fpos);
                renderer.drawCubeShaded(renderer.getCubeVertices(fpos, flash, 0), cam, bodyColor.brighter());
            } else if (growthPhase == 3) {
                int spark = applyScaleToSize((int)(voxelSize * (1.0 + tp)));
                for (int i = 0; i < 3; i++) {
                    Vector3 s = applyTransform(new Vector3(0, voxelSize * (0.8 + i * 0.6), -voxelSize * (0.6 + i * 0.5)));
                    s = applyScaleToPosition(s);
                    renderer.drawCubeShaded(renderer.getCubeVertices(s, spark, 0), cam, bodyColor.brighter().brighter());
                }
            }
        }

        // Dibujar cuerpo base con rotación
        double bodyBob = evolved ? Math.sin(walkPhase * 0.6) * voxelSize * (apex ? 0.6 : 0.4) : 0.0;
        for (Vector3 voxel : voxels) {
            Vector3 relativePos = new Vector3(
                voxel.x * voxelSize,
                voxel.y * voxelSize + bodyBob,
                voxel.z * voxelSize
            );
            Vector3 wp = applyTransform(relativePos);
            wp = applyScaleToPosition(wp);
            int s = applyScaleToSize(voxelSize);
            double rot = 0;
            Vector3[] verts = renderer.getCubeVertices(wp, s, rot);
            renderer.drawCubeShaded(verts, cam, bodyColor);
        }

        // Crin dorsal y rayas lumbares solo en fase 2/3
        if (evolved) {
            int maneSize = applyScaleToSize((int)(voxelSize * 0.8));
            Color maneColor = bodyColor.darker();
            for (int i = 0; i < 3; i++) {
                Vector3 mp = applyTransform(new Vector3(0, voxelSize * (1.5 + i * 0.5) + bodyBob, voxelSize * (0.2 - i * 0.6)));
                mp = applyScaleToPosition(mp);
                renderer.drawCubeShaded(renderer.getCubeVertices(mp, maneSize, 0), cam, maneColor);
            }
            int stripeSize = applyScaleToSize((int)(voxelSize * 0.9));
            Color stripe = new Color(Math.max(0, bodyColor.getRed() - 60), Math.max(0, bodyColor.getGreen() - 60), Math.max(0, bodyColor.getBlue() - 20));
            for (int i = 0; i < 2; i++) {
                Vector3 sp = applyTransform(new Vector3(voxelSize * (i == 0 ? -0.9 : 0.9), voxelSize * 0.6 + bodyBob, voxelSize * (0.2 - i * 0.2)));
                sp = applyScaleToPosition(sp);
                renderer.drawCubeShaded(renderer.getCubeVertices(sp, stripeSize, 0), cam, stripe);
            }
        }

        // Armadura luminosa y hombreras visibles solo en fase 3
        if (apex) {
            int shoulder = applyScaleToSize((int)(voxelSize * 1.1));
            Color glow = bodyColor.brighter();
            Vector3 shL = applyTransform(new Vector3(-voxelSize * 1.2, voxelSize * 1.4 + bodyBob, voxelSize * 0.4));
            Vector3 shR = applyTransform(new Vector3(voxelSize * 1.2, voxelSize * 1.4 + bodyBob, voxelSize * 0.4));
            shL = applyScaleToPosition(shL); shR = applyScaleToPosition(shR);
            renderer.drawCubeShaded(renderer.getCubeVertices(shL, shoulder, 0), cam, glow);
            renderer.drawCubeShaded(renderer.getCubeVertices(shR, shoulder, 0), cam, glow);

            int spineSize = applyScaleToSize((int)(voxelSize * 0.9));
            for (int i = 0; i < 3; i++) {
                Vector3 spine = applyTransform(new Vector3(0, voxelSize * (1.0 + i * 0.7) + bodyBob, -voxelSize * (0.6 + i * 0.4)));
                spine = applyScaleToPosition(spine);
                renderer.drawCubeShaded(renderer.getCubeVertices(spine, spineSize, 0), cam, glow);
            }
        }

        // Orejas con movimiento reactivo
        int earSize = applyScaleToSize((int)(voxelSize * 0.6));
        double earRotL = animController.getEarTwitch(0);
        double earRotR = -animController.getEarTwitch(1);
        Vector3 earL = applyTransform(new Vector3(-voxelSize * 0.6 + earRotL * voxelSize, voxelSize * 3.0, voxelSize * 1.0));
        Vector3 earR = applyTransform(new Vector3(voxelSize * 0.6 + earRotR * voxelSize, voxelSize * 3.0, voxelSize * 1.0));
        earL = applyScaleToPosition(earL);
        earR = applyScaleToPosition(earR);
        renderer.drawCubeShaded(renderer.getCubeVertices(earL, earSize, 0), cam, bodyColor.darker());
        renderer.drawCubeShaded(renderer.getCubeVertices(earR, earSize, 0), cam, bodyColor.darker());

        // Ojos con parpadeo
        int eyeSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.35)));
        Color eyeWhite = new Color(240, 240, 240);
        Color pupil = new Color(20, 40, 40);
        double blinkAmount = animController.getBlinkAmount();
        
        Vector3 eyeL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 2.2, voxelSize * 1.6));
        Vector3 eyeR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 2.2, voxelSize * 1.6));
        eyeL = applyScaleToPosition(eyeL);
        eyeR = applyScaleToPosition(eyeR);
        
        // Renderizar ojo blanco
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeL, eyeSize, 0), cam, eyeWhite);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeR, eyeSize, 0), cam, eyeWhite);
        
        // Renderizar pupila (reducida si está parpadeando)
        int pupilSize = Math.max(1, (int)(eyeSize / 2 * (1.0 - blinkAmount)));
        Vector3 pupilL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 2.2, voxelSize * 1.6 + pupilSize));
        Vector3 pupilR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 2.2, voxelSize * 1.6 + pupilSize));
        pupilL = applyScaleToPosition(pupilL);
        pupilR = applyScaleToPosition(pupilR);
        if (pupilSize > 0) {
            renderer.drawCubeShaded(renderer.getCubeVertices(pupilL, pupilSize, 0), cam, pupil);
            renderer.drawCubeShaded(renderer.getCubeVertices(pupilR, pupilSize, 0), cam, pupil);
        }

        // Boca
        int mouthSize = Math.max(1, eyeSize / 2);
        Vector3 mouth = applyTransform(new Vector3(0, voxelSize * 1.7, voxelSize * 1.8));
        mouth = applyScaleToPosition(mouth);
        renderer.drawCubeShaded(renderer.getCubeVertices(mouth, mouthSize, 0), cam, new Color(90, 40, 40));

        // Cola extendida segun fase, con movimiento ondulante
        int tailLen = growthPhase == 1 ? 2 : (growthPhase == 2 ? 3 : 5); // Más segmentos desde fase 1
        for (int i = 0; i < tailLen; i++) {
            double tailWag = animController.getTailWagOffset(i) * 2.5; // AMPLIFICADO para visibilidad
            double tailVertical = animController.getTailWagVertical(i) * 1.5;
            Vector3 tpos = applyTransform(new Vector3(
                tailWag * voxelSize * 0.6, // Mayor movimiento lateral
                voxelSize * 1.0 + tailVertical,
                -voxelSize * (1.2 + i * 0.8) + tailWag * 0.8
            ));
            tpos = applyScaleToPosition(tpos);
            int ts = applyScaleToSize((int)(voxelSize * 0.9));
            Color tailC = apex && i == tailLen - 1 ? bodyColor.brighter().brighter() : bodyColor;
            renderer.drawCubeShaded(renderer.getCubeVertices(tpos, ts, 0), cam, tailC);
        }

    // Animación EXAGERADA de caminar: patas diagonales coordinadas MUY VISIBLES
        // Patrón: (FL, BR) vs (FR, BL) - patas diagonales se mueven juntas
        double legAmp = evolved ? (apex ? 1.35 : 1.1) : 0.8;
        double frontLeftLeg = Math.sin(walkPhase) * voxelSize * legAmp;
        double frontRightLeg = Math.sin(walkPhase + Math.PI) * voxelSize * legAmp;
        double backLeftLeg = Math.sin(walkPhase + Math.PI) * voxelSize * legAmp;
        double backRightLeg = Math.sin(walkPhase) * voxelSize * legAmp;
        
        int upperLegSize = applyScaleToSize((int)(voxelSize * 0.9));
        int lowerLegSize = applyScaleToSize((int)(voxelSize * 0.8));
        Color paw = new Color(
            Math.max(0, bodyColor.getRed() - 40),
            Math.max(0, bodyColor.getGreen() - 40),
            Math.max(0, bodyColor.getBlue() - 40)
        );
        
        // Articulate legs: estiramiento y compresión EXAGERADO
        double legArticulation = Math.sin(walkPhase * 2) * (evolved ? 0.65 : 0.5);
        
        // Pata delantera izquierda - ARTICULACIÓN MUY VISIBLE
        Vector3 flUpper = applyTransform(new Vector3(
            -voxelSize * 1.0,
            -voxelSize * 0.5 + Math.abs(frontLeftLeg) * 0.4 + legArticulation * voxelSize * 0.3,
            voxelSize * 1.1 + frontLeftLeg * 0.6
        ));
        flUpper = applyScaleToPosition(flUpper);
        Vector3 flLower = applyTransform(new Vector3(
            -voxelSize * 1.05,
            -voxelSize * 1.0 + Math.abs(frontLeftLeg) * 0.2 + legArticulation * voxelSize * 0.2,
            voxelSize * 0.8 + frontLeftLeg * 0.4
        ));
        flLower = applyScaleToPosition(flLower);
        renderer.drawCubeShaded(renderer.getCubeVertices(flUpper, upperLegSize, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(flLower, lowerLegSize, 0), cam, paw);
        
        // Pata delantera derecha
        Vector3 frUpper = applyTransform(new Vector3(
            voxelSize * 1.0,
            -voxelSize * 0.5 + Math.abs(frontRightLeg) * 0.4 - legArticulation * voxelSize * 0.3,
            voxelSize * 1.1 + frontRightLeg * 0.6
        ));
        frUpper = applyScaleToPosition(frUpper);
        Vector3 frLower = applyTransform(new Vector3(
            voxelSize * 1.05,
            -voxelSize * 1.0 + Math.abs(frontRightLeg) * 0.2 - legArticulation * voxelSize * 0.2,
            voxelSize * 0.8 + frontRightLeg * 0.4
        ));
        frLower = applyScaleToPosition(frLower);
        renderer.drawCubeShaded(renderer.getCubeVertices(frUpper, upperLegSize, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(frLower, lowerLegSize, 0), cam, paw);
        
        // Pata trasera izquierda
        Vector3 blUpper = applyTransform(new Vector3(
            -voxelSize * 1.0,
            -voxelSize * 0.5 + Math.abs(backLeftLeg) * 0.4 - legArticulation * voxelSize * 0.3,
            -voxelSize * 1.0 + backLeftLeg * 0.6
        ));
        blUpper = applyScaleToPosition(blUpper);
        Vector3 blLower = applyTransform(new Vector3(
            -voxelSize * 1.05,
            -voxelSize * 1.0 + Math.abs(backLeftLeg) * 0.2 - legArticulation * voxelSize * 0.2,
            -voxelSize * 1.3 + backLeftLeg * 0.4
        ));
        blLower = applyScaleToPosition(blLower);
        renderer.drawCubeShaded(renderer.getCubeVertices(blUpper, upperLegSize, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(blLower, lowerLegSize, 0), cam, paw);
        
        // Pata trasera derecha
        Vector3 brUpper = applyTransform(new Vector3(
            voxelSize * 1.0,
            -voxelSize * 0.5 + Math.abs(backRightLeg) * 0.4 + legArticulation * voxelSize * 0.3,
            -voxelSize * 1.0 + backRightLeg * 0.6
        ));
        brUpper = applyScaleToPosition(brUpper);
        Vector3 brLower = applyTransform(new Vector3(
            voxelSize * 1.05,
            -voxelSize * 1.0 + Math.abs(backRightLeg) * 0.2 + legArticulation * voxelSize * 0.2,
            -voxelSize * 1.3 + backRightLeg * 0.4
        ));
        brLower = applyScaleToPosition(brLower);
        renderer.drawCubeShaded(renderer.getCubeVertices(brUpper, upperLegSize, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(brLower, lowerLegSize, 0), cam, paw);

        // Garras brillantes en fase 3 para destacar las patas
        if (apex) {
            int claw = Math.max(1, lowerLegSize / 2);
            Color clawC = bodyColor.brighter();
            Vector3[] paws = new Vector3[]{
                applyTransform(new Vector3(-voxelSize * 1.05, -voxelSize * 1.0 + Math.abs(frontLeftLeg) * 0.2, voxelSize * 0.6 + frontLeftLeg * 0.4)),
                applyTransform(new Vector3(voxelSize * 1.05, -voxelSize * 1.0 + Math.abs(frontRightLeg) * 0.2, voxelSize * 0.6 + frontRightLeg * 0.4)),
                applyTransform(new Vector3(-voxelSize * 1.05, -voxelSize * 1.0 + Math.abs(backLeftLeg) * 0.2, -voxelSize * 1.3 + backLeftLeg * 0.4)),
                applyTransform(new Vector3(voxelSize * 1.05, -voxelSize * 1.0 + Math.abs(backRightLeg) * 0.2, -voxelSize * 1.3 + backRightLeg * 0.4))
            };
            for (Vector3 p : paws) {
                p = applyScaleToPosition(p);
                renderer.drawCubeShaded(renderer.getCubeVertices(p, claw, 0), cam, clawC);
            }
        }
    }
    
    @Override
    protected double getPhaseDuration(int phase) {
        // Total: 180 segundos (3 minutos)
        // Fase 1: 30s, Fase 2: 60s, Fase 3: 90s
        switch (phase) {
            case 1: return 30.0;
            case 2: return 60.0;
            case 3: return 90.0;
            default: return 60.0;
        }
    }
    
    @Override
    public int getSpeciesType() {
        return 0;
    }
    
    @Override
    protected void applyPhaseVisuals() {
        // Fase 2: Color más intenso y vibrante
        if (growthPhase == 2) {
            int r = Math.min(255, (int)(originalColor.getRed() * 1.3));
            int g = Math.min(255, (int)(originalColor.getGreen() * 1.2));
            int b = Math.min(255, (int)(originalColor.getBlue() * 1.1));
            this.color = new Color(r, g, b);
        }
        // Fase 3: Color oscuro y profundo
        else if (growthPhase == 3) {
            int r = Math.min(255, (int)(originalColor.getRed() * 1.5));
            int g = (int)(originalColor.getGreen() * 0.9);
            int b = Math.min(255, (int)(originalColor.getBlue() * 1.4));
            this.color = new Color(r, g, b);
        }
    }
}
