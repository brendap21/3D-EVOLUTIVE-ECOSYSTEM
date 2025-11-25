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
        // Preproject all grid vertices once so shared edges map to identical screen coords.
        double[][][] proj = new double[width][depth][]; // proj[x][z] = {x2d,y2d,cz} or null
        for(int x=0;x<width;x++){
            for(int z=0; z<depth; z++){
                Vector3 wv = new Vector3((x - width/2.0)*scale, heights[x][z] + terrainOffset, (z - depth/2.0)*scale);
                double[] p = renderer.project(wv, cam);
                if(p != null){
                    // snap XY to pixel centers for exact shared-edge equality
                    p[0] = Math.floor(p[0]) + 0.5;
                    p[1] = Math.floor(p[1]) + 0.5;
                }
                proj[x][z] = p;
            }
        }

        // Rasterize cells using the preprojected coordinates (skip triangles with missing projection)
        float shade = 1.0f;
        Color col = new Color(
            (int)(baseColor.getRed()*shade),
            (int)(baseColor.getGreen()*shade),
            (int)(baseColor.getBlue()*shade)
        );
        for(int x=0;x<width-1;x++){
            for(int z=0; z<depth-1; z++){
                double[] p00 = proj[x][z];
                double[] p10 = proj[x+1][z];
                double[] p01 = proj[x][z+1];
                double[] p11 = proj[x+1][z+1];
                // draw two triangles only when projected vertices exist
                if(p00 != null && p10 != null && p11 != null) renderer.drawTriangleScreen(p00, p10, p11, col);
                if(p00 != null && p11 != null && p01 != null) renderer.drawTriangleScreen(p00, p11, p01, col);
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
