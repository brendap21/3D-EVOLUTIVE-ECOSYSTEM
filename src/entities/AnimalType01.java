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
 */
public class AnimalType01 extends BaseAnimal {
    private double walkPhase = 0.0;

    public AnimalType01(Vector3 posicion, long seed) {
        this.posicion = posicion;
        this.seed = seed;
        this.voxels = new ArrayList<>();
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

        // Voxels base (torso compacto)
        voxels.add(new Vector3(0, 0, 0));
        voxels.add(new Vector3(1, 0, 0));
        voxels.add(new Vector3(-1, 0, 0));
        voxels.add(new Vector3(0, 1, 0));

        // Cabeza
        voxels.add(new Vector3(0, 2, 1));

        // Patas delanteras
        voxels.add(new Vector3(-1, -1, 1));
        voxels.add(new Vector3(1, -1, 1));
        // Patas traseras
        voxels.add(new Vector3(-1, -1, -1));
        voxels.add(new Vector3(1, -1, -1));

        // Cola base
        voxels.add(new Vector3(0, 1, -1));
    }

    @Override
    protected void renderNormal(SoftwareRenderer renderer, Camera cam) {
        walkPhase += 0.18 * getPhaseSpeedMultiplier();
        Color bodyColor = applyGlowToColor(originalColor);

        // Dibujar cuerpo base con rotación
        for (Vector3 voxel : voxels) {
            Vector3 relativePos = new Vector3(
                voxel.x * voxelSize,
                voxel.y * voxelSize,
                voxel.z * voxelSize
            );
            Vector3 wp = applyTransform(relativePos);
            wp = applyScaleToPosition(wp);
            int s = applyScaleToSize(voxelSize);
            double rot = 0;
            Vector3[] verts = renderer.getCubeVertices(wp, s, rot);
            renderer.drawCubeShaded(verts, cam, bodyColor);
        }

        // Orejas (triangulitos usando cubos pequeños) - rotadas con el cuerpo
        int earSize = applyScaleToSize((int)(voxelSize * 0.6));
        Vector3 earL = applyTransform(new Vector3(-voxelSize * 0.6, voxelSize * 3.0, voxelSize * 1.0));
        Vector3 earR = applyTransform(new Vector3(voxelSize * 0.6, voxelSize * 3.0, voxelSize * 1.0));
        earL = applyScaleToPosition(earL);
        earR = applyScaleToPosition(earR);
        renderer.drawCubeShaded(renderer.getCubeVertices(earL, earSize, 0), cam, bodyColor.darker());
        renderer.drawCubeShaded(renderer.getCubeVertices(earR, earSize, 0), cam, bodyColor.darker());

        // Ojos
        int eyeSize = Math.max(1, applyScaleToSize((int)(voxelSize * 0.35)));
        Color eyeWhite = new Color(240, 240, 240);
        Color pupil = new Color(20, 40, 40);
        Vector3 eyeL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 2.2, voxelSize * 1.6));
        Vector3 eyeR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 2.2, voxelSize * 1.6));
        eyeL = applyScaleToPosition(eyeL);
        eyeR = applyScaleToPosition(eyeR);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeL, eyeSize, 0), cam, eyeWhite);
        renderer.drawCubeShaded(renderer.getCubeVertices(eyeR, eyeSize, 0), cam, eyeWhite);
        int pupilSize = Math.max(1, eyeSize / 2);
        Vector3 pupilL = applyTransform(new Vector3(-voxelSize * 0.4, voxelSize * 2.2, voxelSize * 1.6 + pupilSize));
        Vector3 pupilR = applyTransform(new Vector3(voxelSize * 0.4, voxelSize * 2.2, voxelSize * 1.6 + pupilSize));
        pupilL = applyScaleToPosition(pupilL);
        pupilR = applyScaleToPosition(pupilR);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilL, pupilSize, 0), cam, pupil);
        renderer.drawCubeShaded(renderer.getCubeVertices(pupilR, pupilSize, 0), cam, pupil);

        // Boca
        int mouthSize = Math.max(1, eyeSize / 2);
        Vector3 mouth = applyTransform(new Vector3(0, voxelSize * 1.7, voxelSize * 1.8));
        mouth = applyScaleToPosition(mouth);
        renderer.drawCubeShaded(renderer.getCubeVertices(mouth, mouthSize, 0), cam, new Color(90, 40, 40));

        // Cola extendida segun fase
        int tailLen = growthPhase == 1 ? 1 : (growthPhase == 2 ? 2 : 3);
        for (int i = 0; i < tailLen; i++) {
            Vector3 tpos = applyTransform(new Vector3(0, voxelSize * 1.0, -voxelSize * (1.2 + i)));
            tpos = applyScaleToPosition(tpos);
            int ts = applyScaleToSize((int)(voxelSize * 0.8));
            renderer.drawCubeShaded(renderer.getCubeVertices(tpos, ts, 0), cam, bodyColor);
        }

        // Animación realista de caminar: patas delanteras y traseras se alternan
        // Pata delantera izquierda y trasera derecha se mueven juntas
        // Pata delantera derecha y trasera izquierda se mueven juntas
        double frontLeftLeg = Math.sin(walkPhase) * voxelSize * 0.3;
        double frontRightLeg = Math.sin(walkPhase + Math.PI) * voxelSize * 0.3;
        double backLeftLeg = Math.sin(walkPhase + Math.PI) * voxelSize * 0.3; // opuesto a front left
        double backRightLeg = Math.sin(walkPhase) * voxelSize * 0.3; // opuesto a front right
        
        int legSize = applyScaleToSize((int)(voxelSize * 0.85));
        Color paw = bodyColor.darker();
        
        // Pata delantera izquierda (avanza con trasera derecha) - AJUSTAR Y para que sea visible
        Vector3 frontLeft = applyTransform(new Vector3(
            -voxelSize, 
            -voxelSize * 0.5 + Math.abs(frontLeftLeg) * 0.3,  // Ajustado para ser más visible
            voxelSize + frontLeftLeg * 0.5
        ));
        frontLeft = applyScaleToPosition(frontLeft);
        renderer.drawCubeShaded(renderer.getCubeVertices(frontLeft, legSize, 0), cam, paw);
        
        // Pata delantera derecha (avanza con trasera izquierda)
        Vector3 frontRight = applyTransform(new Vector3(
            voxelSize,
            -voxelSize * 0.5 + Math.abs(frontRightLeg) * 0.3,  // Ajustado
            voxelSize + frontRightLeg * 0.5
        ));
        frontRight = applyScaleToPosition(frontRight);
        renderer.drawCubeShaded(renderer.getCubeVertices(frontRight, legSize, 0), cam, paw);
        
        // Pata trasera izquierda (avanza con delantera derecha)
        Vector3 backLeft = applyTransform(new Vector3(
            -voxelSize,
            -voxelSize * 0.5 + Math.abs(backLeftLeg) * 0.3,  // Ajustado
            -voxelSize + backLeftLeg * 0.5
        ));
        backLeft = applyScaleToPosition(backLeft);
        renderer.drawCubeShaded(renderer.getCubeVertices(backLeft, legSize, 0), cam, paw);
        
        // Pata trasera derecha (avanza con delantera izquierda)
        Vector3 backRight = applyTransform(new Vector3(
            voxelSize,
            -voxelSize * 0.5 + Math.abs(backRightLeg) * 0.3,  // Ajustado
            -voxelSize + backRightLeg * 0.5
        ));
        backRight = applyScaleToPosition(backRight);
        renderer.drawCubeShaded(renderer.getCubeVertices(backRight, legSize, 0), cam, paw);
    }
}
