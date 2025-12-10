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

    public AnimalType09(Vector3 posicion, long seed) {
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
            new Color(255, 180, 200),
            new Color(230, 80, 150),
            new Color(200, 50, 120)
        };
        this.color = palette[r.nextInt(palette.length)];
        this.originalColor = color;
        this.baseSpeed = 1.6 + r.nextDouble() * 0.6;

        // Cuerpo compacto
        voxels.add(new Vector3(0, 0, 0));
        voxels.add(new Vector3(0, 1, 0));

        // Cabeza
        voxels.add(new Vector3(0, 2, 0));

        // Patas traseras fuertes (para saltar)
        voxels.add(new Vector3(-1, -1, -1));
        voxels.add(new Vector3(1, -1, -1));
    }

    @Override
    protected void renderNormal(SoftwareRenderer renderer, Camera cam) {
        hopPhase += 0.18 * getPhaseSpeedMultiplier();
        Color body = applyGlowToColor(color);

        // Efecto de salto
        double hop = Math.abs(Math.sin(hopPhase)) * voxelSize * 0.5;
        
        // Cuerpo base (con salto)
        for (Vector3 v : voxels) {
            Vector3 wp = applyTransform(new Vector3(v.x * voxelSize, v.y * voxelSize + hop, v.z * voxelSize));
            wp = applyScaleToPosition(wp);
            int s = applyScaleToSize(voxelSize);
            renderer.drawCubeShaded(renderer.getCubeVertices(wp, s, 0), cam, body);
        }

        // Orejas grandes (crecen con la fase)
        int earLen = 2 + growthPhase;
        int earSize = applyScaleToSize((int)(voxelSize * 0.5));
        for (int i = 0; i < earLen; i++) {
            Vector3 earL = applyTransform(new Vector3(-voxelSize * 0.7, voxelSize * (3.0 + i * 0.8) + hop, 0));
            earL = applyScaleToPosition(earL);
            Vector3 earR = applyTransform(new Vector3(voxelSize * 0.7, voxelSize * (3.0 + i * 0.8) + hop, 0));
            earR = applyScaleToPosition(earR);
            renderer.drawCubeShaded(renderer.getCubeVertices(earL, earSize, 0), cam, body.brighter());
            renderer.drawCubeShaded(renderer.getCubeVertices(earR, earSize, 0), cam, body.brighter());
        }

        // Ojos grandes y tiernos
        int eyeSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.45)));
        Color eyeW = new Color(255, 255, 255);
        Color pupil = new Color(100, 40, 70);
        Vector3 eyeL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 2.3 + hop, voxelSize * 0.6));
        eyeL = applyScaleToPosition(eyeL);
        Vector3 eyeR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 2.3 + hop, voxelSize * 0.6));
        eyeR = applyScaleToPosition(eyeR);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeL, eyeSize, 0), cam, eyeW);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeR, eyeSize, 0), cam, eyeW);
        int pup = Math.max(1, eyeSize / 2);
        Vector3 pupilL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 2.3 + hop, voxelSize * 0.6 + pup));
        pupilL = applyScaleToPosition(pupilL);
        Vector3 pupilR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 2.3 + hop, voxelSize * 0.6 + pup));
        pupilR = applyScaleToPosition(pupilR);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilL, pup, 0), cam, pupil);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilR, pup, 0), cam, pupil);

        // Nariz pequeña
        int noseSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.25)));
        Vector3 nose = applyTransform(new Vector3(0, voxelSize * 2.0 + hop, voxelSize * 0.9));
        nose = applyScaleToPosition(nose);
        renderer.drawCubeShaded(renderer.getCubeVertices(nose, noseSize, 0), cam, new Color(180, 80, 100));

        // Cola pompón
        int tailSize = applyScaleToSize((int)(voxelSize * 0.6));
        Vector3 tail = applyTransform(new Vector3(0, voxelSize * 0.5 + hop * 0.5, -voxelSize * 1.2));
        tail = applyScaleToPosition(tail);
        renderer.drawCubeShaded(renderer.getCubeVertices(tail, tailSize, 0), cam, body.brighter());

        // Patas traseras (siempre en el suelo cuando no está saltando)
        double legBend = Math.sin(hopPhase) * voxelSize * 0.4;
        int legSize = applyScaleToSize((int)(voxelSize * 0.9));
        Color paw = body.darker();
        
        Vector3 leg1 = applyTransform(new Vector3(-voxelSize * 0.5, -voxelSize * 0.5 + Math.abs(legBend) * 0.3, -voxelSize));
        leg1 = applyScaleToPosition(leg1);
        renderer.drawCubeShaded(renderer.getCubeVertices(leg1, legSize, 0), cam, paw);
        Vector3 leg2 = applyTransform(new Vector3(voxelSize * 0.5, -voxelSize * 0.5 + Math.abs(legBend) * 0.3, -voxelSize));
        leg2 = applyScaleToPosition(leg2);
        renderer.drawCubeShaded(renderer.getCubeVertices(leg2, legSize, 0), cam, paw);

        // Patas delanteras pequeñas
        int frontLegSize = applyScaleToSize((int)(voxelSize * 0.5));
        Vector3 frontL = applyTransform(new Vector3(-voxelSize * 0.4, -voxelSize * 0.2 + hop, voxelSize * 0.5));
        frontL = applyScaleToPosition(frontL);
        Vector3 frontR = applyTransform(new Vector3(voxelSize * 0.4, -voxelSize * 0.2 + hop, voxelSize * 0.5));
        frontR = applyScaleToPosition(frontR);
        renderer.drawCubeShaded(renderer.getCubeVertices(frontL, frontLegSize, 0), cam, paw);
        renderer.drawCubeShaded(renderer.getCubeVertices(frontR, frontLegSize, 0), cam, paw);
    }

    @Override
    protected double getPhaseDuration() {
        return 48.0; // 48 segundos por fase
    }
    
    @Override
    protected void applyPhaseVisuals() {
        // Fase 2: Azul más brillante
        if (growthPhase == 2) {
            int r = Math.min(255, (int)(originalColor.getRed() * 1.1));
            int g = Math.min(255, (int)(originalColor.getGreen() * 1.15));
            int b = Math.min(255, (int)(originalColor.getBlue() * 1.3));
            this.color = new Color(r, g, b);
        }
        // Fase 3: Azul profundo con toques cálidos
        else if (growthPhase == 3) {
            int r = Math.min(255, (int)(originalColor.getRed() * 1.2));
            int g = Math.min(255, (int)(originalColor.getGreen() * 1.1));
            int b = Math.min(255, (int)(originalColor.getBlue() * 1.4));
            this.color = new Color(r, g, b);
        }
    }

    @Override
    public String getSpeciesName() {
        return "Ave Voladora";
    }
}
