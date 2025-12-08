package entities;

import main.Renderable;
import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.Random;

/**
 * Terreno: replacement terrain implementation used when Superficie.java is corrupted.
 * Implements Renderable and HeightProvider.
 */
public class Terreno implements Renderable, HeightProvider {
    private int width, depth;
    private double scale;
    private double[][] heights;
    private Color baseColor;
    private double terrainOffset = -0.01; // shared offset applied to rendered vertices and height queries

    public Terreno(int width, int depth, double scale, long seed, Color baseColor){
        this.width = Math.max(2, width);
        this.depth = Math.max(2, depth);
        this.scale = scale;
        this.baseColor = baseColor;
        this.heights = new double[this.width][this.depth];
        generateHeights(seed);
    }

    private void generateHeights(long seed){
        // For this project requirement the terrain must be completely flat
        // (no montes ni valles). Set every sample to a constant elevation.
        double flatHeight = 0.0; // world Y coordinate for the flat plane
        for(int x=0;x<width;x++){
            for(int z=0;z<depth;z++){
                heights[x][z] = flatHeight;
            }
        }
    }

    @Override
    public void update(){ }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam){
        // Draw a grid-based ground plane using screen-space projection
        // to ensure perfect pixel alignment with cubes/quads
        double gridSize = 800;
        double cellSize = 40;
        int cells = 20;
        
        Color col = new Color(
            Math.max(0, baseColor.getRed() - 20),
            Math.max(0, baseColor.getGreen() - 20),
            Math.max(0, baseColor.getBlue() - 20)
        );
        
        double y = 0.0;
        
        // Pre-project all grid vertices to screen space once
        // This ensures shared vertices project to identical screen coords
        java.util.HashMap<String, double[]> projCache = new java.util.HashMap<>();
        
        for(int ix = 0; ix <= cells; ix++) {
            for(int iz = 0; iz <= cells; iz++) {
                double x = -gridSize/2 + ix*cellSize;
                double z = -gridSize/2 + iz*cellSize;
                String key = ix + "," + iz;
                Vector3 v = new Vector3(x, y, z);
                double[] proj = renderer.project(v, cam);
                if (proj != null) {
                    projCache.put(key, proj);
                }
            }
        }
        
        // Draw grid cells using projected screen coords
        for(int ix = 0; ix < cells; ix++) {
            for(int iz = 0; iz < cells; iz++) {
                String k00 = ix + "," + iz;
                String k10 = (ix+1) + "," + iz;
                String k01 = ix + "," + (iz+1);
                String k11 = (ix+1) + "," + (iz+1);
                
                double[] p00 = projCache.get(k00);
                double[] p10 = projCache.get(k10);
                double[] p01 = projCache.get(k01);
                double[] p11 = projCache.get(k11);
                
                if (p00 != null && p10 != null && p01 != null && p11 != null) {
                    // Use drawQuadScreen (which splits to triangles internally)
                    // This ensures identical pipeline to cubes
                    renderer.drawQuadScreen(p00, p10, p11, p01, col);
                }
            }
        }
    }

    @Override
    public double getHeightAt(double worldX, double worldZ){
        // Return height including the terrainOffset so physics/collisions match rendered surface.
        double fx = worldX / scale + width/2.0;
        double fz = worldZ / scale + depth/2.0;
        int x0 = (int)Math.floor(fx);
        int z0 = (int)Math.floor(fz);
        if(x0 < 0) x0 = 0; if(z0 < 0) z0 = 0;
        if(x0 >= width-1) x0 = width-2; if(z0 >= depth-1) z0 = depth-2;
        double sx = fx - x0;
        double sz = fz - z0;
        double h00 = heights[x0][z0];
        double h10 = heights[x0+1][z0];
        double h01 = heights[x0][z0+1];
        double h11 = heights[x0+1][z0+1];
        double hx0 = h00*(1-sx) + h10*sx;
        double hx1 = h01*(1-sx) + h11*sx;
        return hx0*(1-sz) + hx1*sz + terrainOffset;
    }
}
