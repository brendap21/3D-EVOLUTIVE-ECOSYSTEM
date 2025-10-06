package render;

import java.awt.Color;
import java.awt.image.BufferedImage;
import math.Vector3;
import math.Matrix4;
import math.Camera;

public class SoftwareRenderer {
    private BufferedImage buffer;
    private int ancho, alto;

    public SoftwareRenderer(int ancho, int alto) {
        this.ancho = ancho;
        this.alto = alto;
        buffer = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
    }

    public BufferedImage getBuffer() {
        return buffer;
    }

    public double[] project(Vector3 point, Camera cam) {
        Vector3 p = point.sub(cam.getPosicion());
        double scale = cam.getFov() / p.z;
        double x2d = p.x * scale + ancho/2.0;
        double y2d = p.y * scale + alto/2.0;
        return new double[]{x2d, y2d, p.z};
    }

    public void drawLine3D(Vector3 p1, Vector3 p2, Camera cam, Color color) {
        double[] proj1 = project(p1, cam);
        double[] proj2 = project(p2, cam);

        int x1 = (int)proj1[0], y1 = (int)proj1[1];
        int x2 = (int)proj2[0], y2 = (int)proj2[1];

        int rgb = color.getRGB();

        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while(true){
            if(x1>=0 && x1<ancho && y1>=0 && y1<alto)
                buffer.setRGB(x1, y1, rgb);

            if(x1 == x2 && y1 == y2) break;
            int e2 = 2*err;
            if(e2 > -dy){ err -= dy; x1 += sx; }
            if(e2 < dx){ err += dx; y1 += sy; }
        }
    }

    // Método para dibujar un cubo simple conectando 8 vértices
    public void drawCube(Vector3[] vertices, Camera cam, Color color){
        int[][] edges = {
            {0,1},{1,2},{2,3},{3,0},
            {4,5},{5,6},{6,7},{7,4},
            {0,4},{1,5},{2,6},{3,7}
        };
        for(int[] e : edges){
            drawLine3D(vertices[e[0]], vertices[e[1]], cam, color);
        }
    }

    public void drawCylinder(Vector3[] top, Vector3[] bottom, Camera cam, Color color){
        int n = top.length;
        for(int i=0;i<n;i++){
            int next = (i+1)%n;
            drawLine3D(top[i], top[next], cam, color);
            drawLine3D(bottom[i], bottom[next], cam, color);
            drawLine3D(top[i], bottom[i], cam, color);
        }
    }

    public Vector3 transform(Vector3 v, Matrix4 m){
        return m.multiply(v);
    }

    public void clear(Color c){
        int rgb = c.getRGB();
        for(int y=0;y<alto;y++){
            for(int x=0;x<ancho;x++){
                buffer.setRGB(x, y, rgb);
            }
        }
    }
}
