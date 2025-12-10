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

    public AnimalType03(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxels = new ArrayList<>();
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

        // Patas gruesas
        voxels.add(new Vector3(-1, -1, 1));
        voxels.add(new Vector3(1, -1, 1));
        voxels.add(new Vector3(-1, -1, -1));
        voxels.add(new Vector3(1, -1, -1));
    }

    @Override
    protected void renderNormal(SoftwareRenderer renderer, Camera cam) {
        walkPhase += 0.12 * getPhaseSpeedMultiplier();
        Color body = applyGlowToColor(originalColor);

        // Cuerpo base
        for (Vector3 v : voxels) {
            Vector3 wp = applyTransform(new Vector3(v.x * voxelSize, v.y * voxelSize, v.z * voxelSize));
            wp = applyScaleToPosition(wp);
            int s = applyScaleToSize(voxelSize);
            renderer.drawCubeShaded(renderer.getCubeVertices(wp, s, 0), cam, body);
        }

        // Cuernos (crecen con la fase)
        int hornLen = growthPhase;
        int hornSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.5)));
        for (int i = 0; i < hornLen; i++) {
            Vector3 hornL = applyTransform(new Vector3(-voxelSize * 0.7, voxelSize * (2.0 + i * 0.8), voxelSize * 2.0));
            hornL = applyScaleToPosition(hornL);
            Vector3 hornR = applyTransform(new Vector3(voxelSize * 0.7, voxelSize * (2.0 + i * 0.8), voxelSize * 2.0));
            hornR = applyScaleToPosition(hornR);
            renderer.drawCubeShaded(renderer.getCubeVertices(hornL, hornSize, 0), cam, body.brighter());
            renderer.drawCubeShaded(renderer.getCubeVertices(hornR, hornSize, 0), cam, body.brighter());
        }

        // Ojos
        int eyeSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.3)));
        Color eyeW = new Color(250, 250, 250);
        Color pupil = new Color(40, 20, 60);
        Vector3 eyeL = applyTransform(new Vector3(-voxelSize * 0.5, voxelSize * 1.5, voxelSize * 2.5));
        eyeL = applyScaleToPosition(eyeL);
        Vector3 eyeR = applyTransform(new Vector3(voxelSize * 0.5, voxelSize * 1.5, voxelSize * 2.5));
        eyeR = applyScaleToPosition(eyeR);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeL, eyeSize, 0), cam, eyeW);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeR, eyeSize, 0), cam, eyeW);
        int pup = Math.max(1, eyeSize / 2);
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
        int tailLen = 1 + growthPhase;
        int tailSize = applyScaleToSize((int)(voxelSize * 0.9));
        for (int i = 0; i < tailLen; i++) {
            Vector3 tpos = applyTransform(new Vector3(0, voxelSize * 0.5, -voxelSize * (1.5 + i)));
            tpos = applyScaleToPosition(tpos);
            renderer.drawCubeShaded(renderer.getCubeVertices(tpos, tailSize, 0), cam, body.darker());
        }

        // Patas con movimiento pesado
        double frontLeftLeg = Math.sin(walkPhase) * voxelSize * 0.2;
        double frontRightLeg = Math.sin(walkPhase + Math.PI) * voxelSize * 0.2;
        double backLeftLeg = Math.sin(walkPhase + Math.PI) * voxelSize * 0.2;
        double backRightLeg = Math.sin(walkPhase) * voxelSize * 0.2;
        
        int legSize = applyScaleToSize((int)(voxelSize * 1.0));
        Color paw = body.darker().darker();
        
        Vector3 frontLeft = applyTransform(new Vector3(-voxelSize, -voxelSize * 0.5 + Math.abs(frontLeftLeg) * 0.2, voxelSize + frontLeftLeg * 0.3));
        frontLeft = applyScaleToPosition(frontLeft);
        renderer.drawCubeShaded(renderer.getCubeVertices(frontLeft, legSize, 0), cam, paw);
        
        Vector3 frontRight = applyTransform(new Vector3(voxelSize, -voxelSize * 0.5 + Math.abs(frontRightLeg) * 0.2, voxelSize + frontRightLeg * 0.3));
        frontRight = applyScaleToPosition(frontRight);
        renderer.drawCubeShaded(renderer.getCubeVertices(frontRight, legSize, 0), cam, paw);
        
        Vector3 backLeft = applyTransform(new Vector3(-voxelSize, -voxelSize * 0.5 + Math.abs(backLeftLeg) * 0.2, -voxelSize + backLeftLeg * 0.3));
        backLeft = applyScaleToPosition(backLeft);
        renderer.drawCubeShaded(renderer.getCubeVertices(backLeft, legSize, 0), cam, paw);
        
        Vector3 backRight = applyTransform(new Vector3(voxelSize, -voxelSize * 0.5 + Math.abs(backRightLeg) * 0.2, -voxelSize + backRightLeg * 0.3));
        backRight = applyScaleToPosition(backRight);
        renderer.drawCubeShaded(renderer.getCubeVertices(backRight, legSize, 0), cam, paw);
    }

    @Override
    public String getSpeciesName() {
        return "Toro Púrpura";
    }
}
