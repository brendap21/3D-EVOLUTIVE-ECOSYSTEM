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

    public AnimalType08(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxels = new ArrayList<>();
        generateFromSeed(seed);
        initializeSpawnAnimation();
    }

    private void generateFromSeed(long seed) {
        Random r = new Random(seed);
        this.voxelSize = 4 + r.nextInt(2);
        this.baseVoxelSize = voxelSize;
        Color[] palette = new Color[]{
            new Color(120, 90, 60),
            new Color(150, 80, 50),
            new Color(90, 70, 50)
        };
        this.color = palette[r.nextInt(palette.length)];
        this.originalColor = color;
        this.baseSpeed = 0.8 + r.nextDouble() * 0.4;

        // Cuerpo blindado
        voxels.add(new Vector3(0, 0, 0));
        voxels.add(new Vector3(1, 0, 0));
        voxels.add(new Vector3(-1, 0, 0));
        voxels.add(new Vector3(0, 0, 1));
        voxels.add(new Vector3(0, 0, -1));
        voxels.add(new Vector3(0, 1, 0));

        // Cabeza
        voxels.add(new Vector3(0, 0, 2));

        // Patas
        voxels.add(new Vector3(-1, -1, 1));
        voxels.add(new Vector3(1, -1, 1));
        voxels.add(new Vector3(-1, -1, -1));
        voxels.add(new Vector3(1, -1, -1));
    }

    @Override
    protected void renderNormal(SoftwareRenderer renderer, Camera cam) {
        walkPhase += 0.10 * getPhaseSpeedMultiplier();
        Color body = applyGlowToColor(color);

        // Cuerpo base
        for (Vector3 v : voxels) {
            Vector3 wp = applyTransform(new Vector3(v.x * voxelSize, v.y * voxelSize, v.z * voxelSize));
            wp = applyScaleToPosition(wp);
            int s = applyScaleToSize(voxelSize);
            renderer.drawCubeShaded(renderer.getCubeVertices(wp, s, 0), cam, body);
        }

        // Placas dorsales (más numerosas en fases avanzadas)
        int plateCount = 2 + growthPhase * 2;
        int plateSize = applyScaleToSize((int)(voxelSize * 0.7));
        Color plateColor = body.brighter();
        for (int i = 0; i < plateCount; i++) {
            Vector3 plate = applyTransform(new Vector3(
                0,
                voxelSize * (1.5 + i * 0.2),
                voxelSize * (1.0 - i * 0.6)
            ));
            plate = applyScaleToPosition(plate);
            renderer.drawCubeShaded(renderer.getCubeVertices(plate, plateSize, 0), cam, plateColor);
        }

        // Ojos pequeños
        int eyeSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.25)));
        Color eyeW = new Color(220, 220, 180);
        Color pupil = new Color(60, 40, 20);
        Vector3 eyeL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 0.3, voxelSize * 2.3));
        eyeL = applyScaleToPosition(eyeL);
        Vector3 eyeR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 0.3, voxelSize * 2.3));
        eyeR = applyScaleToPosition(eyeR);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeL, eyeSize, 0), cam, eyeW);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeR, eyeSize, 0), cam, eyeW);
        int pup = Math.max(1, eyeSize / 2);
        Vector3 pupilL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 0.3, voxelSize * 2.3 + pup));
        pupilL = applyScaleToPosition(pupilL);
        Vector3 pupilR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 0.3, voxelSize * 2.3 + pup));
        pupilR = applyScaleToPosition(pupilR);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilL, pup, 0), cam, pupil);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilR, pup, 0), cam, pupil);

        // Boca
        int mouthSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.3)));
        Vector3 mouth = applyTransform(new Vector3(0, -voxelSize * 0.2, voxelSize * 2.5));
        mouth = applyScaleToPosition(mouth);
        renderer.drawCubeShaded(renderer.getCubeVertices(mouth, mouthSize, 0), cam, new Color(80, 50, 30));

        // Cola acorazada
        int tailLen = 1 + growthPhase;
        int tailSize = applyScaleToSize((int)(voxelSize * 0.8));
        for (int i = 0; i < tailLen; i++) {
            Vector3 tpos = applyTransform(new Vector3(0, voxelSize * 0.3, -voxelSize * (1.5 + i * 0.8)));
            tpos = applyScaleToPosition(tpos);
            renderer.drawCubeShaded(renderer.getCubeVertices(tpos, tailSize, 0), cam, body.darker());
        }

        // Patas robustas con movimiento lento
        double frontLeftLeg = Math.sin(walkPhase) * voxelSize * 0.15;
        double frontRightLeg = Math.sin(walkPhase + Math.PI) * voxelSize * 0.15;
        double backLeftLeg = Math.sin(walkPhase + Math.PI) * voxelSize * 0.15;
        double backRightLeg = Math.sin(walkPhase) * voxelSize * 0.15;
        
        int legSize = applyScaleToSize((int)(voxelSize * 1.1));
        Color paw = body.darker().darker();
        
        Vector3 fl = applyTransform(new Vector3(-voxelSize * 1.2, -voxelSize * 0.5 + Math.abs(frontLeftLeg) * 0.15, voxelSize + frontLeftLeg * 0.2));
        fl = applyScaleToPosition(fl);
        renderer.drawCubeShaded(renderer.getCubeVertices(fl, legSize, 0), cam, paw);
        Vector3 fr = applyTransform(new Vector3(voxelSize * 1.2, -voxelSize * 0.5 + Math.abs(frontRightLeg) * 0.15, voxelSize + frontRightLeg * 0.2));
        fr = applyScaleToPosition(fr);
        renderer.drawCubeShaded(renderer.getCubeVertices(fr, legSize, 0), cam, paw);
        Vector3 bl = applyTransform(new Vector3(-voxelSize * 1.2, -voxelSize * 0.5 + Math.abs(backLeftLeg) * 0.15, -voxelSize + backLeftLeg * 0.2));
        bl = applyScaleToPosition(bl);
        renderer.drawCubeShaded(renderer.getCubeVertices(bl, legSize, 0), cam, paw);
        Vector3 br = applyTransform(new Vector3(voxelSize * 1.2, -voxelSize * 0.5 + Math.abs(backRightLeg) * 0.15, -voxelSize + backRightLeg * 0.2));
        br = applyScaleToPosition(br);
        renderer.drawCubeShaded(renderer.getCubeVertices(br, legSize, 0), cam, paw);
    }

    @Override
    protected double getPhaseDuration() {
        return 55.0; // 55 segundos por fase
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
