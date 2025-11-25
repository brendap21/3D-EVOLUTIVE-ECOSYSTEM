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
    private int[] ownerBuffer; // deterministically tracks triangle owner per pixel to break ties
    private int[] writeCountBuffer; // counts writes per pixel for diagnostics
    private boolean diagnosticMode = false;
    private double depthEps = 1e-3; // global depth epsilon (tunable) - compromise to avoid flicker

    // Hole-filling controls to avoid doing expensive post-process every frame.
    // Disabled by default to preserve maximum fluidity; can be enabled for artifact-free terrain.
    private boolean holeFillingEnabled = false;
    private int holeFillInterval = 4; // run fillSmallHoles() once every N frames
    private int frameCounter = 0;

    public SoftwareRenderer(int ancho, int alto) {
        this.ancho = ancho;
        this.alto = alto;
        // Use double buffering: draw to backBuffer, present frontBuffer.
        frontBuffer = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        backBuffer = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
    zBuffer = new double[ancho * alto];
    ownerBuffer = new int[ancho * alto];
    writeCountBuffer = new int[ancho * alto];
    diagnosticMode = false; // off by default for normal rendering
    }

    // Return the currently displayed (front) buffer. Synchronized to avoid
    // tearing when swapping from another thread.
    public synchronized BufferedImage getBuffer() {
        return frontBuffer;
    }

    // Swap front/back buffers atomically. Call this after finishing all draw
    // calls for the frame so the UI can paint the newly rendered image.
    public synchronized void swapBuffers(){
        // Optionally run lightweight hole-filling at a reduced frequency to avoid
        // impacting frame-rate/fluidez. Disabled by default.
        if(holeFillingEnabled){
            if((frameCounter++ % holeFillInterval) == 0) fillSmallHoles();
        }

        // If diagnostic mode is enabled, draw an overlay onto the backBuffer
        // so we can see pixels with multiple writes.
        if(diagnosticMode) drawDiagnosticOverlay();

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
    for(int i=0;i<ownerBuffer.length;i++) ownerBuffer[i] = -1;
    for(int i=0;i<writeCountBuffer.length;i++) writeCountBuffer[i] = 0;
    }

    // ---------------- Pixel / rect / text helpers (HUD) ----------------
    public void drawPixel(int x, int y, Color color){
        if(x>=0 && x<ancho && y>=0 && y<alto){
            int idx = y*ancho + x;
            backBuffer.setRGB(x, y, color.getRGB());
            // mark as written so hole-filling won't overwrite HUD pixels
            writeCountBuffer[idx]++;
            ownerBuffer[idx] = -2; // special owner id for HUD/overlay
        }
    }

    public void fillRect(int x, int y, int w, int h, Color color){
        int rgb = color.getRGB();
        int x0 = Math.max(0, x);
        int y0 = Math.max(0, y);
        int x1 = Math.min(ancho, x + w);
        int y1 = Math.min(alto, y + h);
        for(int yy = y0; yy < y1; yy++){
            for(int xx = x0; xx < x1; xx++){
                int idx = yy * ancho + xx;
                backBuffer.setRGB(xx, yy, rgb);
                writeCountBuffer[idx]++;
                ownerBuffer[idx] = -2;
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
            if(x >= 0 && x < ancho && y >= 0 && y < alto){
                int idx = y*ancho + x;
                backBuffer.setRGB(x, y, rgb);
                // mark as HUD drawing so hole-filler won't overwrite the crosshair/menu
                writeCountBuffer[idx]++;
                ownerBuffer[idx] = -2;
            }
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

        // Near-plane: ignore points too close or behind the camera. A slightly
        // larger near value avoids extremely large projected coordinates when
        // the camera is almost on top of the geometry which produces visual
        // artefacts (lines flashing across the screen). If you need smoother
        // clipping, implement triangle-plane clipping later.
        double near = 1.0;
        if (cz <= near) return null;

        // Prevent pathological projections: if coordinates are extremely large
        // or NaN due to very small cz, treat as non-projectable.
        // This avoids huge triangles/lines that cross the screen when moving the camera.
        if(Double.isNaN(cx) || Double.isNaN(cy) || Double.isNaN(cz)) return null;

        // If projected screen coords would be absurdly large, skip projection.
        // Use screen size as heuristic (100x the screen size is already extreme).
        double maxAllowed = Math.max(ancho, alto) * 100.0;

        if (cam.isOrthographic()){
            double scale = cam.getFov();
            double x2d = cx * scale + ancho/2.0;
            double y2d = alto/2.0 - cy * scale; // invert Y to map world-up to screen-up
            if (Math.abs(x2d) > maxAllowed || Math.abs(y2d) > maxAllowed) return null;
            return new double[]{x2d, y2d, cz};
        } else {
            double scale = cam.getFov() / cz;
            double x2d = cx * scale + ancho/2.0;
            double y2d = alto/2.0 - cy * scale; // use same scale; cy already multiplied by scale
            if (Double.isInfinite(scale) || Math.abs(x2d) > maxAllowed || Math.abs(y2d) > maxAllowed) return null;
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
    int lineId = stableTriId(p1, p2, p2); // deterministic id for this line (use p2 twice)

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
                // Depth epsilon to avoid z-fighting / flip-flopping due to
                // tiny numerical differences when updating the z-buffer.
                if(fz < zBuffer[idx] - depthEps){
                    zBuffer[idx] = fz;
                    ownerBuffer[idx] = lineId;
                    backBuffer.setRGB(xi, yi, rgb);
                    writeCountBuffer[idx]++;
                } else if(Math.abs(fz - zBuffer[idx]) <= depthEps){
                    int cur = ownerBuffer[idx];
                    if(cur == -1 || lineId < cur){
                        ownerBuffer[idx] = lineId;
                        zBuffer[idx] = fz;
                        backBuffer.setRGB(xi, yi, rgb);
                        writeCountBuffer[idx]++;
                    }
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
        if(n == 0) return;
        // compute center points for top and bottom (average)
        Vector3 centerTop = new Vector3(0,0,0);
        Vector3 centerBottom = new Vector3(0,0,0);
        for(int i=0;i<n;i++){
            centerTop = centerTop.add(top[i]);
            centerBottom = centerBottom.add(bottom[i]);
        }
        centerTop = centerTop.scale(1.0 / n);
        centerBottom = centerBottom.scale(1.0 / n);

        // draw filled top cap (triangle fan)
        for(int i=0;i<n;i++){
            int next = (i+1)%n;
            drawTriangle(centerTop, top[i], top[next], cam, color);
        }
        // draw filled bottom cap (triangle fan) - winding reversed to keep consistent culling
        for(int i=0;i<n;i++){
            int next = (i+1)%n;
            drawTriangle(centerBottom, bottom[next], bottom[i], cam, color);
        }

        // draw perimeter lines for visual definition of sides
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

    // Top-left edge rule helper: returns true if the edge a->b is a top or
    // left edge according to typical rasterization rules. This is used to
    // consistently break ties when a pixel center lies exactly on an edge so
    // adjacent triangles do not leave cracks.
    private boolean isTopLeft(double[] a, double[] b){
        double ay = a[1], by = b[1];
        double ax = a[0], bx = b[0];
        double eps = 1e-6;
        // Coordinates use screen Y that grows downward. Top-left rule for
        // screen-space: an edge is top-left if it goes downward (a.y < b.y),
        // or if horizontal (same y) and runs right-to-left (a.x > b.x).
        if(Math.abs(ay - by) < eps) return ax > bx; // horizontal: right-to-left is left edge
        return ay < by; // a above b => edge goes downwards -> top edge
    }

    // ---------------- Rasterizar triángulo (filled) ----------------
    // Proyecta los tres vértices, realiza backface culling en pantalla,
    // rasteriza con barycentric/edge functions, interpola profundidad y
    // escribe en el z-buffer y backBuffer. Aplica sombreado Lambert simple
    // usando la normal de la cara.
    public void drawTriangle(Vector3 v0, Vector3 v1, Vector3 v2, Camera cam, Color color){
        // Convert world-space vertices to camera-space (no projection)
        double[] c0 = worldToCamera(v0, cam);
        double[] c1 = worldToCamera(v1, cam);
        double[] c2 = worldToCamera(v2, cam);

        double near = 1.0;

        // If all vertices are behind the near plane, nothing to draw
        if(c0[2] <= near && c1[2] <= near && c2[2] <= near) return;

        // Clip triangle against near plane (cz > near). This may produce 0,1 or 2 triangles.
        java.util.List<Vector3[]> tris = clipTriangleAgainstNear(v0, v1, v2, c0, c1, c2, near);
        if(tris == null || tris.isEmpty()) return;

        // Precompute face normal and lighting from original triangle (world-space)
        Vector3 e1 = v1.subtract(v0);
        Vector3 e2 = v2.subtract(v0);
        Vector3 normal = e1.cross(e2).normalize();
        Vector3 lightDir = new Vector3(0, 0.7, -1).normalize();
        double intensity = normal.x*lightDir.x + normal.y*lightDir.y + normal.z*lightDir.z;
        if(intensity < 0) intensity = 0;

        double amb = 0.2;

        for(Vector3[] tri : tris){
            int triId = stableTriId(tri[0], tri[1], tri[2]);
            double[] p0 = project(tri[0], cam);
            double[] p1 = project(tri[1], cam);
            double[] p2 = project(tri[2], cam);
            if(p0 == null || p1 == null || p2 == null) continue;

            // Pixel-snapping (local): quantize XY to pixel centers so adjacent triangles
            // that share world vertices map to identical screen coordinates. This
            // prevents sub-pixel seams for rigid meshes (cubes, cylinders, terrain).
            p0[0] = Math.floor(p0[0]) + 0.5; p0[1] = Math.floor(p0[1]) + 0.5;
            p1[0] = Math.floor(p1[0]) + 0.5; p1[1] = Math.floor(p1[1]) + 0.5;
            p2[0] = Math.floor(p2[0]) + 0.5; p2[1] = Math.floor(p2[1]) + 0.5;

            // Backface culling in screen space using signed area (keep previous behavior)
            float area = edgeFunction(p0, p1, p2);
            if(area <= 1e-6f) continue;
            
            // Bounding box in pixel coords
            // Compute bounding box and expand by 1 pixel to avoid cracks at triangle edges
            int rawMinX = (int)Math.floor(Math.min(p0[0], Math.min(p1[0], p2[0])));
            int rawMaxX = (int)Math.ceil (Math.max(p0[0], Math.max(p1[0], p2[0])));
            int rawMinY = (int)Math.floor(Math.min(p0[1], Math.min(p1[1], p2[1])));
            int rawMaxY = (int)Math.ceil (Math.max(p0[1], Math.max(p1[1], p2[1])));
            int minX = Math.max(0, rawMinX - 1);
            int maxX = Math.min(ancho - 1, rawMaxX + 1);
            int minY = Math.max(0, rawMinY - 1);
            int maxY = Math.min(alto - 1, rawMaxY + 1);
 
             for(int y = minY; y <= maxY; y++){
                for(int x = minX; x <= maxX; x++){
                     // Conservative coverage test:
                     // include pixel if the pixel center OR any of its 4 corners
                     // lie inside the triangle (with a tiny tolerance).
                     float epsf = 1e-6f;
                     double[] pc = {x + 0.5, y + 0.5};
                     // Compute center edge functions (these will be used for barycentric)
                     float w0 = edgeFunction(p1, p2, pc);
                     float w1 = edgeFunction(p2, p0, pc);
                     float w2 = edgeFunction(p0, p1, pc);
                     boolean covered = (w0 >= -epsf && w1 >= -epsf && w2 >= -epsf);
                     if(!covered){
                        // test four corners (conservative rasterization) using distinct names
                        double[][] corners = {
                            {x + 0.0, y + 0.0},
                            {x + 1.0, y + 0.0},
                            {x + 1.0, y + 1.0},
                            {x + 0.0, y + 1.0}
                        };
                        for(int ci=0; ci<4 && !covered; ci++){
                            double[] cc = corners[ci];
                            float cc0 = edgeFunction(p1, p2, cc);
                            float cc1 = edgeFunction(p2, p0, cc);
                            float cc2 = edgeFunction(p0, p1, cc);
                            if(cc0 >= -epsf && cc1 >= -epsf && cc2 >= -epsf) covered = true;
                        }
                     }
                     if(covered){
                         double alpha = w0 / area;
                         double beta = w1 / area;
                         double gamma = w2 / area;
 
                         double z = alpha * p0[2] + beta * p1[2] + gamma * p2[2];
                         int idx = y * ancho + x;
                         // Small depth epsilon: require z to be sufficiently closer
                         // than the stored value before overwriting. This prevents
                         // alternating writes when two triangles are extremely
                         // close in depth due to numerical noise.
                         if(z < zBuffer[idx] - depthEps){
                             zBuffer[idx] = z;
                             ownerBuffer[idx] = triId;
                             // Simple Lambert shading + ambient
                             double lit = amb + (1.0 - amb) * intensity;
                             int rr = (int)Math.min(255, Math.max(0, color.getRed() * lit));
                             int gg = (int)Math.min(255, Math.max(0, color.getGreen() * lit));
                             int bb = (int)Math.min(255, Math.max(0, color.getBlue() * lit));
                             backBuffer.setRGB(x, y, (new Color(rr, gg, bb)).getRGB());
                             writeCountBuffer[idx]++;
                         } else if(Math.abs(z - zBuffer[idx]) <= depthEps){
                             int cur = ownerBuffer[idx];
                             if(cur == -1 || triId < cur){
                                 ownerBuffer[idx] = triId;
                                 zBuffer[idx] = z;
                                 double lit = amb + (1.0 - amb) * intensity;
                                 int rr = (int)Math.min(255, Math.max(0, color.getRed() * lit));
                                 int gg = (int)Math.min(255, Math.max(0, color.getGreen() * lit));
                                 int bb = (int)Math.min(255, Math.max(0, color.getBlue() * lit));
                                 backBuffer.setRGB(x, y, (new Color(rr, gg, bb)).getRGB());
                                 writeCountBuffer[idx]++;
                             }
                         }
                     }
                 }
             }
        }
    }

    // Rasterize a triangle already given in projected screen-space coordinates.
    // p arrays are {x_screen, y_screen, cam_z}. This bypasses world->camera projection
    // and ensures adjacent triangles that share projected vertices produce identical edges.
    public void drawTriangleScreen(double[] p0, double[] p1, double[] p2, Color color){
        // Backface culling via signed area
        float area = edgeFunction(p0, p1, p2);
        if(area <= 1e-6f) return;

        // deterministic id from projected positions (stable-ish)
        long ax = Math.round(p0[0]*1000.0), ay = Math.round(p0[1]*1000.0), az = Math.round(p0[2]*1000.0);
        long bx = Math.round(p1[0]*1000.0), by = Math.round(p1[1]*1000.0), bz = Math.round(p1[2]*1000.0);
        long cx = Math.round(p2[0]*1000.0), cy = Math.round(p2[1]*1000.0), cz = Math.round(p2[2]*1000.0);
        long h = ax * 73856093L ^ ay * 19349663L ^ az * 83492791L ^ bx * 2654435761L ^ by * 1361234567L ^ bz * 97531L ^ cx * 7129L ^ cy * 1741L ^ cz * 97L;
        int triId = (int)(h & 0x7FFFFFFF);

        // Bounding box (expand 1px as before)
        int rawMinX = (int)Math.floor(Math.min(p0[0], Math.min(p1[0], p2[0])));
        int rawMaxX = (int)Math.ceil (Math.max(p0[0], Math.max(p1[0], p2[0])));
        int rawMinY = (int)Math.floor(Math.min(p0[1], Math.min(p1[1], p2[1])));
        int rawMaxY = (int)Math.ceil (Math.max(p0[1], Math.max(p1[1], p2[1])));
        int minX = Math.max(0, rawMinX - 1);
        int maxX = Math.min(ancho - 1, rawMaxX + 1);
        int minY = Math.max(0, rawMinY - 1);
        int maxY = Math.min(alto - 1, rawMaxY + 1);

        // Precompute face lighting using geometric normal approximation in camera-space:
        // approximate normal from projected triangle edges (gives consistent simple shading)
        double nx = (p1[1]-p0[1])*(p2[2]-p0[2]) - (p1[2]-p0[2])*(p2[1]-p0[1]);
        double ny = (p1[2]-p0[2])*(p2[0]-p0[0]) - (p1[0]-p0[0])*(p2[2]-p0[2]);
        double nz = (p1[0]-p0[0])*(p2[1]-p0[1]) - (p1[1]-p0[1])*(p2[0]-p0[0]);
        double nlen = Math.sqrt(nx*nx + ny*ny + nz*nz);
        double intensity = 0.5;
        if(nlen > 1e-6){
            nx/=nlen; ny/=nlen; nz/=nlen;
            // lightDir roughly camera-space (-z forward)
            double lx = 0, ly = 0.7, lz = -1;
            double llen = Math.sqrt(lx*lx + ly*ly + lz*lz);
            lx/=llen; ly/=llen; lz/=llen;
            intensity = nx*lx + ny*ly + nz*lz;
            if(intensity < 0) intensity = 0;
        }
        double amb = 0.2;
        double lit = amb + (1.0 - amb) * intensity;

        // Raster loop (conservative coverage same as drawTriangle)
        for(int y = minY; y <= maxY; y++){
            for(int x = minX; x <= maxX; x++){
                double[] pc = {x + 0.5, y + 0.5};
                float w0 = edgeFunction(p1, p2, pc);
                float w1 = edgeFunction(p2, p0, pc);
                float w2 = edgeFunction(p0, p1, pc);
                float epsf = 1e-6f;
                boolean covered = (w0 >= -epsf && w1 >= -epsf && w2 >= -epsf);
                if(!covered){
                    double[][] corners = {
                        {x + 0.0, y + 0.0},
                        {x + 1.0, y + 0.0},
                        {x + 1.0, y + 1.0},
                        {x + 0.0, y + 1.0}
                    };
                    for(int ci=0; ci<4 && !covered; ci++){
                        double[] cc = corners[ci];
                        float cc0 = edgeFunction(p1, p2, cc);
                        float cc1 = edgeFunction(p2, p0, cc);
                        float cc2 = edgeFunction(p0, p1, cc);
                        if(cc0 >= -epsf && cc1 >= -epsf && cc2 >= -epsf) covered = true;
                    }
                }
                if(covered){
                    double alpha = w0 / area;
                    double beta  = w1 / area;
                    double gamma = w2 / area;
                    double z = alpha * p0[2] + beta * p1[2] + gamma * p2[2];
                    int idx = y * ancho + x;
                    if(z < zBuffer[idx] - depthEps){
                        zBuffer[idx] = z;
                        ownerBuffer[idx] = triId;
                        int rr = (int)Math.min(255, Math.max(0, color.getRed() * lit));
                        int gg = (int)Math.min(255, Math.max(0, color.getGreen() * lit));
                        int bb = (int)Math.min(255, Math.max(0, color.getBlue() * lit));
                        backBuffer.setRGB(x, y, (new Color(rr, gg, bb)).getRGB());
                        writeCountBuffer[idx]++;
                    } else if(Math.abs(z - zBuffer[idx]) <= depthEps){
                        int cur = ownerBuffer[idx];
                        if(cur == -1 || triId < cur){
                            ownerBuffer[idx] = triId;
                            zBuffer[idx] = z;
                            int rr = (int)Math.min(255, Math.max(0, color.getRed() * lit));
                            int gg = (int)Math.min(255, Math.max(0, color.getGreen() * lit));
                            int bb = (int)Math.min(255, Math.max(0, color.getBlue() * lit));
                            backBuffer.setRGB(x, y, (new Color(rr, gg, bb)).getRGB());
                            writeCountBuffer[idx]++;
                        }
                    }
                }
            }
        }
    }

    // Deterministic stable id for a triangle given three world-space verts.
    // Rounds coordinates to millimeter-ish precision and mixes them into an int.
    private int stableTriId(Vector3 a, Vector3 b, Vector3 c){
        long ax = Math.round(a.x * 1000.0);
        long ay = Math.round(a.y * 1000.0);
        long az = Math.round(a.z * 1000.0);
        long bx = Math.round(b.x * 1000.0);
        long by = Math.round(b.y * 1000.0);
        long bz = Math.round(b.z * 1000.0);
        long cx = Math.round(c.x * 1000.0);
        long cy = Math.round(c.y * 1000.0);
        long cz = Math.round(c.z * 1000.0);
        long h = ax * 73856093L ^ ay * 19349663L ^ az * 83492791L ^ bx * 2654435761L ^ by * 1361234567L ^ bz * 97531L ^ cx * 7129L ^ cy * 1741L ^ cz * 97L;
        return (int)(h & 0x7FFFFFFF);
    }

    // Diagnostic overlay: paint pixels that were written multiple times in magenta
    private void drawDiagnosticOverlay(){
        int w = ancho, h = alto;
        for(int y=0;y<h;y++){
            for(int x=0;x<w;x++){
                int idx = y*w + x;
                int wc = writeCountBuffer[idx];
                if(wc > 1){
                    // color intensity based on count, clamp
                    int intensity = Math.min(255, 80 + wc*30);
                    int rgb = (255<<16) | (0<<8) | (255); // magenta
                    backBuffer.setRGB(x, y, rgb);
                }
            }
        }
    }

    // Enable/disable diagnostics
    public void setDiagnosticMode(boolean on){ this.diagnosticMode = on; }

    // Transform a world-space point to camera-space coordinates (cx,cy,cz)
    private double[] worldToCamera(Vector3 point, Camera cam){
        Vector3 camPos = cam.getPosicion();
        Vector3 rel = new Vector3(point.x - camPos.x, point.y - camPos.y, point.z - camPos.z);
        Vector3 forward = cam.getForward().normalize();
        Vector3 worldUp = new Vector3(0, 1, 0);
        Vector3 right = worldUp.cross(forward).normalize();
        Vector3 up = forward.cross(right).normalize();
        double cx = rel.x * right.x + rel.y * right.y + rel.z * right.z;
        double cy = rel.x * up.x    + rel.y * up.y    + rel.z * up.z;
        double cz = rel.x * forward.x + rel.y * forward.y + rel.z * forward.z;
        return new double[]{cx, cy, cz};
    }

    private Vector3 lerpVec(Vector3 a, Vector3 b, double t){
        return new Vector3(a.x*(1-t) + b.x*t, a.y*(1-t) + b.y*t, a.z*(1-t) + b.z*t);
    }

    private double[] lerpCam(double[] a, double[] b, double t){
        return new double[]{a[0]*(1-t)+b[0]*t, a[1]*(1-t)+b[1]*t, a[2]*(1-t)+b[2]*t};
    }

    // Clip a triangle (world-space + camera-space representations) against
    // the near plane cz > near. Returns a list of world-space triangles.
    private java.util.List<Vector3[]> clipTriangleAgainstNear(Vector3 v0w, Vector3 v1w, Vector3 v2w,
                                                               double[] c0, double[] c1, double[] c2,
                                                               double near){
        java.util.List<double[]> cam = new java.util.ArrayList<>();
        java.util.List<Vector3> world = new java.util.ArrayList<>();
        cam.add(c0); cam.add(c1); cam.add(c2);
        world.add(v0w); world.add(v1w); world.add(v2w);

        java.util.List<double[]> outCam = new java.util.ArrayList<>();
        java.util.List<Vector3> outWorld = new java.util.ArrayList<>();

        for(int i=0;i<3;i++){
            int j = (i+1)%3;
            double[] ci = cam.get(i); double[] cj = cam.get(j);
            Vector3 wi = world.get(i); Vector3 wj = world.get(j);
            boolean insideI = ci[2] > near;
            boolean insideJ = cj[2] > near;
            if(insideI && insideJ){
                // both inside: keep j
                if(outCam.isEmpty() || outCam.get(outCam.size()-1) != cj){
                    outCam.add(cj); outWorld.add(wj);
                }
            } else if(insideI && !insideJ){
                // leaving: add intersection
                double t = (near - ci[2]) / (cj[2] - ci[2]);
                double[] cip = lerpCam(ci, cj, t);
                Vector3 wip = lerpVec(wi, wj, t);
                outCam.add(cip); outWorld.add(wip);
            } else if(!insideI && insideJ){
                // entering: add intersection then j
                double t = (near - ci[2]) / (cj[2] - ci[2]);
                double[] cip = lerpCam(ci, cj, t);
                Vector3 wip = lerpVec(wi, wj, t);
                outCam.add(cip); outWorld.add(wip);
                outCam.add(cj); outWorld.add(wj);
            } else {
                // both outside: add nothing
            }
        }

        java.util.List<Vector3[]> result = new java.util.ArrayList<>();
        if(outWorld.size() < 3) return result;
        if(outWorld.size() == 3){
            result.add(new Vector3[]{outWorld.get(0), outWorld.get(1), outWorld.get(2)});
            return result;
        }
        if(outWorld.size() == 4){
            // split quad into two triangles (0,1,2) and (0,2,3)
            result.add(new Vector3[]{outWorld.get(0), outWorld.get(1), outWorld.get(2)});
            result.add(new Vector3[]{outWorld.get(0), outWorld.get(2), outWorld.get(3)});
            return result;
        }
        // For polygons with more vertices (unlikely for triangle clipping), fan triangulate
        for(int i=1;i<outWorld.size()-1;i++) result.add(new Vector3[]{outWorld.get(0), outWorld.get(i), outWorld.get(i+1)});
        return result;
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

    // Allow runtime control to enable/disable the hole-filling post-process.
    public void setHoleFillingEnabled(boolean on){ this.holeFillingEnabled = on; }
    public boolean isHoleFillingEnabled(){ return this.holeFillingEnabled; }
    // Adjust how often (in frames) fillSmallHoles runs when enabled. Min 1.
    public void setHoleFillInterval(int frames){ this.holeFillInterval = Math.max(1, frames); }
    public int getHoleFillInterval(){ return this.holeFillInterval; }

    // Small conservative hole-filling: optimized candidate-based single-pass fill.
    private void fillSmallHoles(){
        int w = ancho, h = alto, N = w*h;
        // Quick check: if there are very few written pixels, skip (scene empty or cleared)
        int writtenTotal = 0;
        for(int i=0;i<N;i++){ if(writeCountBuffer[i] > 0) writtenTotal++; }
        if(writtenTotal == 0) return;

        // Gather candidate pixels: unwritten pixels that have at least one written neighbor.
        java.util.ArrayList<Integer> candidates = new java.util.ArrayList<>();
        for(int y=1; y<h-1; y++){
            int base = y*w;
            for(int x=1; x<w-1; x++){
                int idx = base + x;
                if(writeCountBuffer[idx] != 0) continue;
                // 4-neighbor quick test
                if(writeCountBuffer[idx-1] > 0 || writeCountBuffer[idx+1] > 0 || writeCountBuffer[idx-w] > 0 || writeCountBuffer[idx+w] > 0){
                    candidates.add(idx);
                }
            }
        }
        if(candidates.isEmpty()) return;

        // Snapshot arrays we need
        int[] owner = ownerBuffer; // use live ownerBuffer for checks
        double[] depth = zBuffer;  // use live zBuffer

        int depthNeighborsRequired = 3; // require majority among neighbors
        double depthGapThreshold = 0.3; // stricter small threshold

        int[] neighOffsets = { -1, 1, -w, w, -w-1, -w+1, w-1, w+1 };
        java.util.List<Integer> filled = new java.util.ArrayList<>();

        // Single pass over candidates (lightweight)
        for(int idx : candidates){
            // count neighbor owners (exclude HUD owner -2 and unwritten)
            java.util.HashMap<Integer, java.util.ArrayList<Integer>> ownerMap = new java.util.HashMap<>();
            for(int off : neighOffsets){
                int ni = idx + off;
                if(ni < 0 || ni >= N) continue;
                int o = owner[ni];
                if(o >= 0 && writeCountBuffer[ni] > 0){
                    ownerMap.putIfAbsent(o, new java.util.ArrayList<>());
                    ownerMap.get(o).add(ni);
                }
            }
            int bestOwner = -1; java.util.List<Integer> bestList = null; int bestCount = 0;
            for(java.util.Map.Entry<Integer, java.util.ArrayList<Integer>> e : ownerMap.entrySet()){
                int cnt = e.getValue().size();
                if(cnt > bestCount){ bestOwner = e.getKey(); bestList = e.getValue(); bestCount = cnt; }
            }
            if(bestOwner >= 0 && bestCount >= depthNeighborsRequired && bestList != null){
                double dmin = Double.POSITIVE_INFINITY, dmax = Double.NEGATIVE_INFINITY, sumD=0; int nd=0;
                for(int ni : bestList){
                    double zd = depth[ni];
                    if(Double.isInfinite(zd)) continue;
                    dmin = Math.min(dmin, zd); dmax = Math.max(dmax, zd);
                    sumD += zd; nd++;
                }
                if(nd>0 && (dmax - dmin) <= depthGapThreshold){
                    double avgD = sumD / nd;
                    // pick color from one neighbor (we can read back once per candidate)
                    int ny = idx / w; int nx = idx % w;
                    int rgbSample = backBuffer.getRGB(nx, ny); // representative (neighbor already written)
                    // commit immediately
                    zBuffer[idx] = avgD;
                    ownerBuffer[idx] = bestOwner;
                    writeCountBuffer[idx] = 1;
                    backBuffer.setRGB(nx, ny, rgbSample);
                    filled.add(idx);
                }
            }
        }
        // done (single fast pass)
    }
}
