package render;

import java.awt.Color;
import java.awt.image.BufferedImage;
import math.Vector3;
import math.Camera;

public class SoftwareRenderer {
    private BufferedImage frontBuffer;
    private BufferedImage backBuffer;
    private int ancho, alto;
    private double[] zBuffer;

    public SoftwareRenderer(int ancho, int alto) {
        this.ancho = ancho;
        this.alto = alto;
        // Use double buffering: draw to backBuffer, present frontBuffer.
        frontBuffer = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        backBuffer = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
    zBuffer = new double[ancho * alto];
    }

    // Return the currently displayed (front) buffer. Synchronized to avoid
    // tearing when swapping from another thread.
    public synchronized BufferedImage getBuffer() {
        return frontBuffer;
    }

    // Swap front/back buffers atomically. Call this after finishing all draw
    // calls for the frame so the UI can paint the newly rendered image.
    public synchronized void swapBuffers(){
        BufferedImage tmp = frontBuffer;
        frontBuffer = backBuffer;
        backBuffer = tmp;
    }

    public void clear(Color c){
        int rgb = c.getRGB();
        for(int y=0; y<alto; y++){
            for(int x=0; x<ancho; x++){
                backBuffer.setRGB(x, y, rgb);
            }
        }
        // reset z-buffer to far (positive infinity)
    for(int i=0;i<zBuffer.length;i++) zBuffer[i] = Double.POSITIVE_INFINITY;
    }

    // ---------------- Pixel / rect / text helpers (HUD) ----------------
    public void drawPixel(int x, int y, Color color){
        if(x>=0 && x<ancho && y>=0 && y<alto) backBuffer.setRGB(x, y, color.getRGB());
    }

    public void fillRect(int x, int y, int w, int h, Color color){
        int rgb = color.getRGB();
        int x0 = Math.max(0, x);
        int y0 = Math.max(0, y);
        int x1 = Math.min(ancho, x + w);
        int y1 = Math.min(alto, y + h);
        for(int yy = y0; yy < y1; yy++){
            for(int xx = x0; xx < x1; xx++){
                backBuffer.setRGB(xx, yy, rgb);
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
            if(x >= 0 && x < ancho && y >= 0 && y < alto) backBuffer.setRGB(x, y, rgb);
            if(x == x2 && y == y2) break;
            int e2 = 2*err;
            if(e2 > -dy){ err -= dy; x += sx; }
            if(e2 < dx){ err += dx; y += sy; }
        }
    }

    // ---------------- Proyección ----------------
    public double[] project(Vector3 point, Camera cam) {
        // Transform point into camera (view) space using camera basis vectors
        Vector3 camPos = cam.getPosicion();
        Vector3 rel = new Vector3(point.x - camPos.x, point.y - camPos.y, point.z - camPos.z);

    // Build orthonormal camera basis from forward and world-up to avoid skew
    Vector3 forward = cam.getForward().normalize();
    Vector3 worldUp = new Vector3(0, 1, 0);
    Vector3 right = worldUp.cross(forward).normalize();
    Vector3 up = forward.cross(right).normalize();

    // Camera-space coordinates (dot with orthonormal basis)
    double cx = rel.x * right.x + rel.y * right.y + rel.z * right.z;
    double cy = rel.x * up.x    + rel.y * up.y    + rel.z * up.z;
    double cz = rel.x * forward.x + rel.y * forward.y + rel.z * forward.z;

        double near = 0.1;
        if (cz <= near) return null;

        if (cam.isOrthographic()){
            double scale = cam.getFov();
            double x2d = cx * scale + ancho/2.0;
            double y2d = alto/2.0 - cy * scale; // invert Y to map world-up to screen-up
            return new double[]{x2d, y2d, cz};
        } else {
            double scale = cam.getFov() / cz;
            double x2d = cx * scale + ancho/2.0;
            double y2d = alto/2.0 - cy * scale; // use same scale; cy already multiplied by scale
            return new double[]{x2d, y2d, cz};
        }
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
                    zBuffer[idx] = z1;
                    backBuffer.setRGB(x1, y1, rgb);
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
                    zBuffer[idx] = fz;
                    backBuffer.setRGB(xi, yi, rgb);
                }
            }
            fx += dx; fy += dy; fz += dz;
        }
    }

    // ---------------- Cubo ----------------
    public void drawCube(Vector3[] vertices, Camera cam, Color color){
        // Render cube as filled faces (6 faces, 2 triangles each)
        int[][] faces = {
            {0,1,2,3}, // back
            {4,5,6,7}, // front
            {0,4,5,1}, // bottom
            {1,5,6,2}, // right
            {2,6,7,3}, // top
            {3,7,4,0}  // left
        };

        for(int[] f : faces){
            // split quad into two triangles: (a,b,c) and (a,c,d)
            Vector3 a = vertices[f[0]];
            Vector3 b = vertices[f[1]];
            Vector3 c = vertices[f[2]];
            Vector3 d = vertices[f[3]];
            drawTriangle(a, b, c, cam, color);
            drawTriangle(a, c, d, cam, color);
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

    // ---------------- Rasterizar triángulo (filled) ----------------
    // Proyecta los tres vértices, realiza backface culling en pantalla,
    // rasteriza con barycentric/edge functions, interpola profundidad y
    // escribe en el z-buffer y backBuffer. Aplica sombreado Lambert simple
    // usando la normal de la cara.
    public void drawTriangle(Vector3 v0, Vector3 v1, Vector3 v2, Camera cam, Color color){
        double[] p0 = project(v0, cam);
        double[] p1 = project(v1, cam);
        double[] p2 = project(v2, cam);

        if(p0 == null || p1 == null || p2 == null) return;

        // Backface culling in screen space using signed area
        float area = edgeFunction(p0, p1, p2);
        if(area <= 0) return; // triangle facing away or degenerate

        // Bounding box in pixel coords
        int minX = (int)Math.max(0, Math.floor(Math.min(p0[0], Math.min(p1[0], p2[0]))));
        int maxX = (int)Math.min(ancho-1, Math.ceil(Math.max(p0[0], Math.max(p1[0], p2[0]))));
        int minY = (int)Math.max(0, Math.floor(Math.min(p0[1], Math.min(p1[1], p2[1]))));
        int maxY = (int)Math.min(alto-1, Math.ceil(Math.max(p0[1], Math.max(p1[1], p2[1]))));

        // Precompute face normal and lighting
        Vector3 e1 = v1.subtract(v0);
        Vector3 e2 = v2.subtract(v0);
        Vector3 normal = e1.cross(e2).normalize();
        Vector3 lightDir = new Vector3(0, 0.7, -1).normalize();
        double intensity = normal.x*lightDir.x + normal.y*lightDir.y + normal.z*lightDir.z;
        if(intensity < 0) intensity = 0;

        for(int y = minY; y <= maxY; y++){
            for(int x = minX; x <= maxX; x++){
                // center of pixel
                double[] p = {x + 0.5, y + 0.5};
                float w0 = edgeFunction(p1, p2, p);
                float w1 = edgeFunction(p2, p0, p);
                float w2 = edgeFunction(p0, p1, p);
                if(w0 >= 0 && w1 >= 0 && w2 >= 0){
                    double alpha = w0 / area;
                    double beta = w1 / area;
                    double gamma = w2 / area;

                    double z = alpha * p0[2] + beta * p1[2] + gamma * p2[2];
                    int idx = y * ancho + x;
                    if(z < zBuffer[idx]){
                        zBuffer[idx] = z;
                        // Simple Lambert shading + ambient
                        double amb = 0.2;
                        double lit = amb + (1.0 - amb) * intensity;
                        int r = (int)Math.min(255, Math.max(0, color.getRed() * lit));
                        int g = (int)Math.min(255, Math.max(0, color.getGreen() * lit));
                        int b = (int)Math.min(255, Math.max(0, color.getBlue() * lit));
                        backBuffer.setRGB(x, y, (new Color(r, g, b)).getRGB());
                    }
                }
            }
        }
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
