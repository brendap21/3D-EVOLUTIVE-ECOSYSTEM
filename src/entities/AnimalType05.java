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

    public AnimalType05(Vector3 posicion, long seed) {
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

        // Patas
        voxels.add(new Vector3(-1, -1, 1));
        voxels.add(new Vector3(1, -1, 1));
        voxels.add(new Vector3(-1, -1, -1));
        voxels.add(new Vector3(1, -1, -1));
    }

    @Override
    protected void renderNormal(SoftwareRenderer renderer, Camera cam) {
        walkPhase += 0.16 * getPhaseSpeedMultiplier();
        Color body = applyGlowToColor(originalColor);

        // Cuerpo base
        for (Vector3 v : voxels) {
            Vector3 wp = applyTransform(new Vector3(v.x * voxelSize, v.y * voxelSize, v.z * voxelSize));
            wp = applyScaleToPosition(wp);
            int s = applyScaleToSize(voxelSize);
            renderer.drawCubeShaded(renderer.getCubeVertices(wp, s, 0), cam, body);
        }

        // Espinas (más numerosas en fases avanzadas)
        int spikeCount = 4 + growthPhase * 3;
        int spikeSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.5)));
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

        // Ojos
        int eyeSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.3)));
        Color eyeW = new Color(240, 240, 240);
        Color pupil = new Color(80, 20, 20);
        Vector3 eyeL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 1.3, voxelSize * 1.5));
        eyeL = applyScaleToPosition(eyeL);
        Vector3 eyeR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 1.3, voxelSize * 1.5));
        eyeR = applyScaleToPosition(eyeR);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeL, eyeSize, 0), cam, eyeW);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeR, eyeSize, 0), cam, eyeW);
        int pup = Math.max(1, eyeSize / 2);
        Vector3 pupilL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 1.3, voxelSize * 1.5 + pup));
        pupilL = applyScaleToPosition(pupilL);
        Vector3 pupilR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 1.3, voxelSize * 1.5 + pup));
        pupilR = applyScaleToPosition(pupilR);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilL, pup, 0), cam, pupil);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilR, pup, 0), cam, pupil);

        // Boca
        int mouthSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.35)));
        Vector3 mouth = applyTransform(new Vector3(0, voxelSize * 0.8, voxelSize * 1.8));
        mouth = applyScaleToPosition(mouth);
        renderer.drawCubeShaded(renderer.getCubeVertices(mouth, mouthSize, 0), cam, new Color(100, 20, 20));

        // Patas
        double frontLeftLeg = Math.sin(walkPhase) * voxelSize * 0.25;
        double frontRightLeg = Math.sin(walkPhase + Math.PI) * voxelSize * 0.25;
        double backLeftLeg = Math.sin(walkPhase + Math.PI) * voxelSize * 0.25;
        double backRightLeg = Math.sin(walkPhase) * voxelSize * 0.25;
        
        int legSize = applyScaleToSize((int)(voxelSize * 0.8));
        Color paw = body.darker();
        
        Vector3 fl = applyTransform(new Vector3(-voxelSize, -voxelSize * 0.5 + Math.abs(frontLeftLeg) * 0.3, voxelSize + frontLeftLeg * 0.4));
        fl = applyScaleToPosition(fl);
        renderer.drawCubeShaded(renderer.getCubeVertices(fl, legSize, 0), cam, paw);
        Vector3 fr = applyTransform(new Vector3(voxelSize, -voxelSize * 0.5 + Math.abs(frontRightLeg) * 0.3, voxelSize + frontRightLeg * 0.4));
        fr = applyScaleToPosition(fr);
        renderer.drawCubeShaded(renderer.getCubeVertices(fr, legSize, 0), cam, paw);
        Vector3 bl = applyTransform(new Vector3(-voxelSize, -voxelSize * 0.5 + Math.abs(backLeftLeg) * 0.3, -voxelSize + backLeftLeg * 0.4));
        bl = applyScaleToPosition(bl);
        renderer.drawCubeShaded(renderer.getCubeVertices(bl, legSize, 0), cam, paw);
        Vector3 br = applyTransform(new Vector3(voxelSize, -voxelSize * 0.5 + Math.abs(backRightLeg) * 0.3, -voxelSize + backRightLeg * 0.4));
        br = applyScaleToPosition(br);
        renderer.drawCubeShaded(renderer.getCubeVertices(br, legSize, 0), cam, paw);
    }

    @Override
    public String getSpeciesName() {
        return "Erizo Carmesí";
    }
}
