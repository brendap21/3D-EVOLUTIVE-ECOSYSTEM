package entities;

import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * AnimalType09: Criatura ágil saltadora con orejas grandes.
 * Paleta fase 1 (3 colores): rosa claro, fucsia, magenta.
 * Fase 2-3 orejas más grandes, saltos más altos.
 */
public class AnimalType09 extends BaseAnimal {
    private double hopPhase = 0.0;
    private double earPhase = 0.0;
    private AnimationController animController;

    public AnimalType09(Vector3 posicion, long seed) {
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
            new Color(255, 180, 200),
            new Color(230, 80, 150),
            new Color(200, 50, 120)
        };
        this.color = palette[r.nextInt(palette.length)];
        this.originalColor = color;
        this.baseSpeed = 1.6 + r.nextDouble() * 0.6;

        // Cuerpo compacto (patas se dibujan animadas)
        voxels.add(new Vector3(0, 0, 0));
        voxels.add(new Vector3(0, 1, 0));

        // Cabeza
        voxels.add(new Vector3(0, 2, 0));
    }

    @Override
    protected void renderNormal(SoftwareRenderer renderer, Camera cam) {
        animController.update(0.016);
        animController.setBlinkFrequency(2.8);
        animController.setJawFrequency(1.6);
        animController.setTailWagFrequency(1.9);
            hopPhase += 0.18 * getPhaseSpeedMultiplier();
            earPhase += 0.25;
            boolean evolved = growthPhase >= 2;
            boolean apex = growthPhase == 3;
            Color body = applyGlowToColor(color);

        // Transición de fase: brillo en orejas/pecho
        double tp = transitionPulse;
        if (tp > 0) {
            if (growthPhase == 2) {
                int flash = applyScaleToSize((int)(voxelSize * (1.0 + tp)));
                Vector3 epos = applyTransform(new Vector3(0, voxelSize * (3.0 + tp), 0));
                epos = applyScaleToPosition(epos);
                renderer.drawCubeShaded(renderer.getCubeVertices(epos, flash, 0), cam, body.brighter());
            } else if (growthPhase == 3) {
                int chest = applyScaleToSize((int)(voxelSize * (1.2 + tp)));
                Vector3 cpos = applyTransform(new Vector3(0, voxelSize * (1.0 + tp), voxelSize * 0.4));
                cpos = applyScaleToPosition(cpos);
                renderer.drawCubeShaded(renderer.getCubeVertices(cpos, chest, 0), cam, body.brighter().brighter());
            }
        }

        // Efecto de salto
            double hop = Math.abs(Math.sin(hopPhase)) * voxelSize * (apex ? 1.2 : (evolved ? 0.8 : 0.5));
        
        // Cuerpo base (con salto)
        for (Vector3 v : voxels) {
            Vector3 wp = applyTransform(new Vector3(v.x * voxelSize, v.y * voxelSize + hop, v.z * voxelSize));
            wp = applyScaleToPosition(wp);
            int s = applyScaleToSize(voxelSize);
            renderer.drawCubeShaded(renderer.getCubeVertices(wp, s, 0), cam, body);
        }

        // Orejas grandes (crecen con la fase) con oscilación visible
        int earLen = apex ? 6 : 2 + growthPhase;
            int earSize = applyScaleToSize((int)(voxelSize * (apex ? 1.0 : (evolved ? 0.85 : 0.7))));
            double earTilt = Math.sin(earPhase) * voxelSize * (apex ? 0.8 : (evolved ? 0.6 : 0.45));
        for (int i = 0; i < earLen; i++) {
            Vector3 earL = applyTransform(new Vector3(-voxelSize * 0.7 - earTilt, voxelSize * (3.0 + i * 0.8) + hop, 0));
            earL = applyScaleToPosition(earL);
            Vector3 earR = applyTransform(new Vector3(voxelSize * 0.7 + earTilt, voxelSize * (3.0 + i * 0.8) + hop, 0));
            earR = applyScaleToPosition(earR);
            renderer.drawCubeShaded(renderer.getCubeVertices(earL, earSize, 0), cam, body.brighter());
            renderer.drawCubeShaded(renderer.getCubeVertices(earR, earSize, 0), cam, body.brighter());
        }

        if (apex) {
            int earTip = Math.max(1, earSize / 2);
            Vector3 tipL = applyTransform(new Vector3(-voxelSize * 0.7 - earTilt, voxelSize * (3.0 + earLen * 0.8) + hop, voxelSize * 0.2));
            Vector3 tipR = applyTransform(new Vector3(voxelSize * 0.7 + earTilt, voxelSize * (3.0 + earLen * 0.8) + hop, voxelSize * 0.2));
            tipL = applyScaleToPosition(tipL); tipR = applyScaleToPosition(tipR);
            renderer.drawCubeShaded(renderer.getCubeVertices(tipL, earTip, 0), cam, body.brighter().brighter());
            renderer.drawCubeShaded(renderer.getCubeVertices(tipR, earTip, 0), cam, body.brighter().brighter());
        }

        // Ojos grandes y tiernos
        int eyeSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.5)));
        Color eyeW = new Color(255, 255, 255);
        Color pupil = new Color(100, 40, 70);
        Vector3 eyeL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 2.3 + hop, voxelSize * 0.6));
        eyeL = applyScaleToPosition(eyeL);
        Vector3 eyeR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 2.3 + hop, voxelSize * 0.6));
        eyeR = applyScaleToPosition(eyeR);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeL, eyeSize, 0), cam, eyeW);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeR, eyeSize, 0), cam, eyeW);
        int pup = Math.max(1, (int)(eyeSize * 0.55 * (1.0 - animController.getBlinkAmount())));
        Vector3 pupilL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 2.3 + hop, voxelSize * 0.6 + pup));
        pupilL = applyScaleToPosition(pupilL);
        Vector3 pupilR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 2.3 + hop, voxelSize * 0.6 + pup));
        pupilR = applyScaleToPosition(pupilR);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilL, pup, 0), cam, pupil);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilR, pup, 0), cam, pupil);

        // Nariz y boca
        int noseSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.3)));
        Vector3 nose = applyTransform(new Vector3(0, voxelSize * 2.0 + hop + animController.getJawOpen() * 0.1, voxelSize * 0.9 + animController.getJawOpen() * 0.2));
        nose = applyScaleToPosition(nose);
        renderer.drawCubeShaded(renderer.getCubeVertices(nose, noseSize, 0), cam, new Color(180, 80, 100));
        int mouthSize = Math.max(1, applyScaleToSize((int)(voxelSize * (0.25 + animController.getJawOpen() * 0.25))));
        Vector3 mouth = applyTransform(new Vector3(0, voxelSize * 1.8 + hop - animController.getJawOpen() * 0.1, voxelSize * 0.8 + animController.getJawOpen() * 0.25));
        mouth = applyScaleToPosition(mouth);
        renderer.drawCubeShaded(renderer.getCubeVertices(mouth, mouthSize, 0), cam, new Color(150, 60, 90));

        // Cola pompón animada
            int tailSize = applyScaleToSize((int)(voxelSize * (apex ? 1.2 : (evolved ? 1.0 : 0.85))));
            double wag = animController.getTailWagOffset(0) * (apex ? 1.1 : (evolved ? 0.9 : 0.7));
            double wagV = animController.getTailWagVertical(0) * (apex ? 0.8 : (evolved ? 0.6 : 0.45));
            Vector3 tail = applyTransform(new Vector3(wag, voxelSize * 0.5 + hop * 0.6 + wagV, -voxelSize * 1.2));
        tail = applyScaleToPosition(tail);
        renderer.drawCubeShaded(renderer.getCubeVertices(tail, tailSize, 0), cam, body.brighter());

        if (apex) {
            int tailSpark = Math.max(1, tailSize / 2);
            Vector3 spark = applyTransform(new Vector3(wag * 1.2, voxelSize * 0.5 + hop * 0.6 + wagV + voxelSize * 0.2, -voxelSize * 1.6));
            spark = applyScaleToPosition(spark);
            renderer.drawCubeShaded(renderer.getCubeVertices(spark, tailSpark, 0), cam, body.brighter().brighter());
        }

        // Patas traseras fuertes para saltar (dos segmentos) y delanteras pequeñas articuladas
        double legBend = Math.sin(hopPhase) * voxelSize * (apex ? 0.9 : 0.6);
        int thigh = applyScaleToSize((int)(voxelSize * 1.0));
        int shin = applyScaleToSize((int)(voxelSize * 0.85));
        Color paw = body.darker();

        Vector3 backLThigh = applyTransform(new Vector3(-voxelSize * 0.8, -voxelSize * 0.2 + Math.abs(legBend) * 0.3, -voxelSize));
        Vector3 backLShin = applyTransform(new Vector3(-voxelSize * 0.9, -voxelSize * 0.9 + Math.abs(legBend) * 0.6, -voxelSize * 1.1 + legBend * 0.3));
        backLThigh = applyScaleToPosition(backLThigh); backLShin = applyScaleToPosition(backLShin);
        renderer.drawCubeShaded(renderer.getCubeVertices(backLThigh, thigh, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(backLShin, shin, 0), cam, paw);

        Vector3 backRThigh = applyTransform(new Vector3(voxelSize * 0.8, -voxelSize * 0.2 + Math.abs(legBend) * 0.3, -voxelSize));
        Vector3 backRShin = applyTransform(new Vector3(voxelSize * 0.9, -voxelSize * 0.9 + Math.abs(legBend) * 0.6, -voxelSize * 1.1 + legBend * 0.3));
        backRThigh = applyScaleToPosition(backRThigh); backRShin = applyScaleToPosition(backRShin);
        renderer.drawCubeShaded(renderer.getCubeVertices(backRThigh, thigh, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(backRShin, shin, 0), cam, paw);

        int foreUpper = applyScaleToSize((int)(voxelSize * (apex ? 0.8 : 0.6)));
        int foreLower = applyScaleToSize((int)(voxelSize * (apex ? 0.7 : 0.5)));
        double foreSwing = Math.sin(hopPhase + Math.PI / 2) * voxelSize * (apex ? 0.7 : 0.45);
        Vector3 frontLUp = applyTransform(new Vector3(-voxelSize * 0.4, -voxelSize * 0.1 + Math.abs(foreSwing) * 0.2, voxelSize * 0.5 + foreSwing * 0.2));
        Vector3 frontLLo = applyTransform(new Vector3(-voxelSize * 0.45, -voxelSize * 0.6 + Math.abs(foreSwing) * 0.15, voxelSize * 0.4 + foreSwing * 0.15));
        frontLUp = applyScaleToPosition(frontLUp); frontLLo = applyScaleToPosition(frontLLo);
        renderer.drawCubeShaded(renderer.getCubeVertices(frontLUp, foreUpper, 0), cam, paw.brighter());
        renderer.drawCubeShaded(renderer.getCubeVertices(frontLLo, foreLower, 0), cam, paw.brighter());

        Vector3 frontRUp = applyTransform(new Vector3(voxelSize * 0.4, -voxelSize * 0.1 + Math.abs(foreSwing) * 0.2, voxelSize * 0.5 + foreSwing * 0.2));
        Vector3 frontRLo = applyTransform(new Vector3(voxelSize * 0.45, -voxelSize * 0.6 + Math.abs(foreSwing) * 0.15, voxelSize * 0.4 + foreSwing * 0.15));
        frontRUp = applyScaleToPosition(frontRUp); frontRLo = applyScaleToPosition(frontRLo);
        renderer.drawCubeShaded(renderer.getCubeVertices(frontRUp, foreUpper, 0), cam, paw.brighter());
        renderer.drawCubeShaded(renderer.getCubeVertices(frontRLo, foreLower, 0), cam, paw.brighter());
        // Pecho claro en fase 2/3
        if (evolved) {
            int belly = applyScaleToSize((int)(voxelSize * 0.9));
            Vector3 bpos = applyTransform(new Vector3(0, voxelSize * 0.5 + hop, voxelSize * 0.2));
            bpos = applyScaleToPosition(bpos);
            renderer.drawCubeShaded(renderer.getCubeVertices(bpos, belly, 0), cam, body.brighter());
        }

        if (apex) {
            int chest = applyScaleToSize((int)(voxelSize * 1.0));
            Vector3 cpos = applyTransform(new Vector3(0, voxelSize * 1.0 + hop, voxelSize * 0.5));
            cpos = applyScaleToPosition(cpos);
            renderer.drawCubeShaded(renderer.getCubeVertices(cpos, chest, 0), cam, body.brighter().brighter());
        }

    }

    @Override
    protected double getPhaseDuration(int phase) {
        // Total: 180 segundos (3 minutos)
        // Fase 1: 35s, Fase 2: 65s, Fase 3: 80s
        switch (phase) {
            case 1: return 35.0;
            case 2: return 65.0;
            case 3: return 80.0;
            default: return 60.0;
        }
    }
    
    @Override
    public int getSpeciesType() {
        return 8;
    }
    
    @Override
    protected void applyPhaseVisuals() {
        // Fase 2: Rosa más brillante
        if (growthPhase == 2) {
            int r = Math.min(255, (int)(originalColor.getRed() * 1.2));
            int g = Math.min(255, (int)(originalColor.getGreen() * 1.05));
            int b = Math.min(255, (int)(originalColor.getBlue() * 1.1));
            this.color = new Color(r, g, b);
        }
        // Fase 3: Magenta profundo con brillo
        else if (growthPhase == 3) {
            int r = Math.min(255, (int)(originalColor.getRed() * 1.3));
            int g = Math.min(255, (int)(originalColor.getGreen() * 1.1));
            int b = Math.min(255, (int)(originalColor.getBlue() * 1.2));
            this.color = new Color(r, g, b);
        }
    }

    @Override
    public String getSpeciesName() {
        return "Liebre Saltarina";
    }
}
