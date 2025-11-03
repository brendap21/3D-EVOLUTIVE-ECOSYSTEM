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

    public Terreno(int width, int depth, double scale, long seed, Color baseColor){
        this.width = Math.max(2, width);
        this.depth = Math.max(2, depth);
        this.scale = scale;
        this.baseColor = baseColor;
        this.heights = new double[this.width][this.depth];
        generateHeights(seed);
    }

    private void generateHeights(long seed){
        Random r = new Random(seed);
        double freq1 = 0.08; double amp1 = 18.0;
        double freq2 = 0.16; double amp2 = 6.0;
        double ox = r.nextDouble()*1000.0;
        double oz = r.nextDouble()*1000.0;
        for(int x=0;x<width;x++){
            for(int z=0;z<depth;z++){
                double wx = (x+ox) * freq1;
                double wz = (z+oz) * freq1;
                double h = Math.sin(wx) * Math.cos(wz) * amp1;
                double wx2 = (x+ox)*freq2; double wz2 = (z+oz)*freq2;
                h += Math.sin(wx2*1.3 + wz2*0.7) * amp2;
                h += (r.nextDouble() - 0.5) * 2.0;
                heights[x][z] = h;
            }
        }
    }

    @Override
    public void update(){ }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam){
        for(int x=0;x<width-1;x++){
            for(int z=0;z<depth-1;z++){
                Vector3 v00 = new Vector3((x - width/2)*scale, heights[x][z], (z - depth/2)*scale);
                Vector3 v10 = new Vector3((x+1 - width/2)*scale, heights[x+1][z], (z - depth/2)*scale);
                Vector3 v01 = new Vector3((x - width/2)*scale, heights[x][z+1], (z+1 - depth/2)*scale);
                Vector3 v11 = new Vector3((x+1 - width/2)*scale, heights[x+1][z+1], (z+1 - depth/2)*scale);

                double avgH = (heights[x][z] + heights[x+1][z] + heights[x][z+1] + heights[x+1][z+1]) / 4.0;
                float shade = (float)Math.max(0.0, Math.min(1.0, 0.4 + (avgH/40.0)));
                Color col = new Color(
                    (int)(baseColor.getRed()*shade),
                    (int)(baseColor.getGreen()*shade),
                    (int)(baseColor.getBlue()*shade)
                );

                renderer.drawTriangle(v00, v10, v11, cam, col);
                renderer.drawTriangle(v00, v11, v01, cam, col);
            }
        }
    }

    @Override
    public double getHeightAt(double worldX, double worldZ){
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
        return hx0*(1-sz) + hx1*sz;
    }
}
