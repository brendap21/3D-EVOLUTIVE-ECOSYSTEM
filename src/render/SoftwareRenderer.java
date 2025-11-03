package render;

import java.awt.Color;
import java.awt.image.BufferedImage;
import math.Vector3;
import math.Camera;

public class SoftwareRenderer {
    private BufferedImage buffer;
    private int ancho, alto;
    private float[] zBuffer;

    public SoftwareRenderer(int ancho, int alto) {
        this.ancho = ancho;
        this.alto = alto;
        buffer = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        zBuffer = new float[ancho * alto];
    }

    public BufferedImage getBuffer() {
        return buffer;
    }

    public void clear(Color c){
        int rgb = c.getRGB();
        for(int y=0; y<alto; y++){
            for(int x=0; x<ancho; x++){
                buffer.setRGB(x, y, rgb);
            }
        }
        // reset z-buffer to far (positive infinity)
        for(int i=0;i<zBuffer.length;i++) zBuffer[i] = Float.POSITIVE_INFINITY;
    }

    // ---------------- Pixel / rect / text helpers (HUD) ----------------
    public void drawPixel(int x, int y, Color color){
        if(x>=0 && x<ancho && y>=0 && y<alto) buffer.setRGB(x, y, color.getRGB());
    }

    public void fillRect(int x, int y, int w, int h, Color color){
        int rgb = color.getRGB();
        int x0 = Math.max(0, x);
        int y0 = Math.max(0, y);
        int x1 = Math.min(ancho, x + w);
        int y1 = Math.min(alto, y + h);
        for(int yy = y0; yy < y1; yy++){
            for(int xx = x0; xx < x1; xx++){
                buffer.setRGB(xx, yy, rgb);
            }
        }
    }


    // ---------------- Línea 2D en pantalla (Bresenham) ----------------
    // Dibuja una línea directamente en el buffer en coordenadas de píxel.
    public void drawLine2D(int x1, int y1, int x2, int y2, Color color){
        int rgb = color.getRGB();
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        int x = x1, y = y1;
        while(true){
            if(x >= 0 && x < ancho && y >= 0 && y < alto) buffer.setRGB(x, y, rgb);
            if(x == x2 && y == y2) break;
            int e2 = 2*err;
            if(e2 > -dy){ err -= dy; x += sx; }
            if(e2 < dx){ err += dx; y += sy; }
        }
    }

    // ---------------- Proyección ----------------
    public double[] project(Vector3 point, Camera cam) {
        // Translate point to camera-relative coordinates
        Vector3 p = point.subtract(cam.getPosicion());

        // Apply inverse camera rotation: rotate world by -yaw (Y axis), then -pitch (X axis)
        double yaw = cam.getYaw();
        double pitch = cam.getPitch();
        math.Matrix4 ry = math.Matrix4.rotationY(-yaw);
        p = ry.multiply(p);
        math.Matrix4 rx = math.Matrix4.rotationX(-pitch);
        p = rx.multiply(p);
        // near plane: cull points that are too close or behind the camera to avoid
        // inverted projection and extreme scales which cause flicker.
        double near = 1.0;
        if (p.z <= near) {
            return null;
        }

        double scale = cam.getFov() / p.z;
        double x2d = p.x * scale + ancho/2.0;
        double y2d = p.y * scale + alto/2.0;
        return new double[]{x2d, y2d, p.z};
    }

    // ---------------- Línea 3D ----------------
    public void drawLine3D(Vector3 p1, Vector3 p2, Camera cam, Color color){
        double[] proj1 = project(p1, cam);
        double[] proj2 = project(p2, cam);

        // If either endpoint is behind the near plane or cannot be projected, skip the line.
        if (proj1 == null || proj2 == null) return;

        int x1 = (int)proj1[0], y1 = (int)proj1[1];
        int x2 = (int)proj2[0], y2 = (int)proj2[1];
        double z1 = proj1[2], z2 = proj2[2];
        int rgb = color.getRGB();

        // Use DDA so we can interpolate depth linearly and use z-buffer per pixel.
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            if(x1>=0 && x1<ancho && y1>=0 && y1<alto){
                int idx = y1*ancho + x1;
                if(z1 < zBuffer[idx]){
                    zBuffer[idx] = (float)z1;
                    buffer.setRGB(x1, y1, rgb);
                }
            }
            return;
        }

        double dx = (double)(x2 - x1) / steps;
        double dy = (double)(y2 - y1) / steps;
        double dz = (z2 - z1) / steps;

        double fx = x1, fy = y1, fz = z1;
        for(int i=0;i<=steps;i++){
            int xi = (int)Math.round(fx);
            int yi = (int)Math.round(fy);
            if(xi>=0 && xi<ancho && yi>=0 && yi<alto){
                int idx = yi*ancho + xi;
                if(fz < zBuffer[idx]){
                    zBuffer[idx] = (float)fz;
                    buffer.setRGB(xi, yi, rgb);
                }
            }
            fx += dx; fy += dy; fz += dz;
        }
    }

    // ---------------- Cubo ----------------
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

    // ---------------- Cilindro ----------------
    public void drawCylinder(Vector3[] top, Vector3[] bottom, Camera cam, Color color){
        int n = top.length;
        for(int i=0; i<n; i++){
            int next = (i+1)%n;
            drawLine3D(top[i], top[next], cam, color);
            drawLine3D(bottom[i], bottom[next], cam, color);
            drawLine3D(top[i], bottom[i], cam, color);
        }
    }

    // ---------------- Edge Function ----------------
    private float edgeFunction(double[] a, double[] b, double[] c){
        return (float)((c[0]-a[0])*(b[1]-a[1]) - (c[1]-a[1])*(b[0]-a[0]));
    }

    // ---------------- Generar vértices de cubo ----------------
    public Vector3[] getCubeVertices(Vector3 pos, int tam, double rotY){
        double half = tam / 2.0;
        Vector3[] vertices = new Vector3[8];
        double[][] offsets = {
            {-half,-half,-half},{half,-half,-half},{half,half,-half},{-half,half,-half},
            {-half,-half,half},{half,-half,half},{half,half,half},{-half,half,half}
        };
        double cos = Math.cos(rotY), sin = Math.sin(rotY);

        for(int i=0; i<8; i++){
            double x = offsets[i][0], y = offsets[i][1], z = offsets[i][2];
            double xr = x * cos - z * sin;
            double zr = x * sin + z * cos;
            vertices[i] = new Vector3(pos.x + xr, pos.y + y, pos.z + zr);
        }
        return vertices;
    }

    // ---------------- Generar vértices de cilindro ----------------
    public Vector3[] getCylinderTopVertices(Vector3 pos, int radio, int altura, double rotY){
        int sides = 20;
        Vector3[] top = new Vector3[sides];
        double cos = Math.cos(rotY), sin = Math.sin(rotY);
        for(int i=0; i<sides; i++){
            double angle = 2*Math.PI*i/sides;
            double x = radio * Math.cos(angle);
            double z = radio * Math.sin(angle);
            double xr = x * cos - z * sin;
            double zr = x * sin + z * cos;
            top[i] = new Vector3(pos.x + xr, pos.y + altura/2.0, pos.z + zr);
        }
        return top;
    }

    public Vector3[] getCylinderBottomVertices(Vector3 pos, int radio, int altura, double rotY){
        int sides = 20;
        Vector3[] bottom = new Vector3[sides];
        double cos = Math.cos(rotY), sin = Math.sin(rotY);
        for(int i=0; i<sides; i++){
            double angle = 2*Math.PI*i/sides;
            double x = radio * Math.cos(angle);
            double z = radio * Math.sin(angle);
            double xr = x * cos - z * sin;
            double zr = x * sin + z * cos;
            bottom[i] = new Vector3(pos.x + xr, pos.y - altura/2.0, pos.z + zr);
        }
        return bottom;
    }
}
