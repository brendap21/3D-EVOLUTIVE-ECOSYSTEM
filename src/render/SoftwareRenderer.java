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

    // Small depth epsilon to avoid z-fighting and unstable writes
    // Reduced to avoid suppressing valid near-coplanar triangle pixels during rotation.
    private double depthEps = 1e-5; // slightly looser to prevent cracks on coplanar quads (terrain)

    // ---------------- Helpers faltantes (proyección/recorte) ----------------
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

    // Clip a triangle (world-space + camera-space representations) against the near plane cz > near.
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
                if(outCam.isEmpty() || outCam.get(outCam.size()-1) != cj){
                    outCam.add(cj); outWorld.add(wj);
                }
            } else if(insideI && !insideJ){
                double t = (near - ci[2]) / (cj[2] - ci[2]);
                double[] cip = lerpCam(ci, cj, t);
                Vector3 wip = lerpVec(wi, wj, t);
                outCam.add(cip); outWorld.add(wip);
            } else if(!insideI && insideJ){
                double t = (near - ci[2]) / (cj[2] - ci[2]);
                double[] cip = lerpCam(ci, cj, t);
                Vector3 wip = lerpVec(wi, wj, t);
                outCam.add(cip); outWorld.add(wip);
                outCam.add(cj); outWorld.add(wj);
            }
        }

        java.util.List<Vector3[]> result = new java.util.ArrayList<>();
        if(outWorld.size() < 3) return result;
        if(outWorld.size() == 3){
            result.add(new Vector3[]{outWorld.get(0), outWorld.get(1), outWorld.get(2)});
            return result;
        }
        if(outWorld.size() == 4){
            result.add(new Vector3[]{outWorld.get(0), outWorld.get(1), outWorld.get(2)});
            result.add(new Vector3[]{outWorld.get(0), outWorld.get(2), outWorld.get(3)});
            return result;
        }
        for(int i=1;i<outWorld.size()-1;i++) result.add(new Vector3[]{outWorld.get(0), outWorld.get(i), outWorld.get(i+1)});
        return result;
    }

    // Primitivas básicas: cubo y cilindro
    public Vector3[] getCubeVertices(Vector3 pos, int tam, double rotY){
        double half = tam / 2.0;
        Vector3[] vertices = new Vector3[8];
        
        // Offsets locales (coordenadas relativas al centro)
        double[][] offsets = {
            {-half,-half,-half},{half,-half,-half},{half,half,-half},{-half,half,-half}, // Front face
            {-half,-half,half},{half,-half,half},{half,half,half},{-half,half,half}      // Back face
        };
        
        // Pre-calcular seno y coseno (optimización)
        double cos = Math.cos(rotY), sin = Math.sin(rotY);
        
        // Aplicar transformaciones: ROTACIÓN Y + TRASLACIÓN
        for(int i=0;i<8;i++){
            double x = offsets[i][0], y = offsets[i][1], z = offsets[i][2];
            
            // ROTACIÓN alrededor de Y (matriz 2x2 en el plano XZ)
            double xr = x * cos - z * sin;
            double zr = x * sin + z * cos;
            
            // TRASLACIÓN: añadir posición del mundo
            vertices[i] = new Vector3(pos.x + xr, pos.y + y, pos.z + zr);
        }
        return vertices;
    }

    public Vector3[] getCylinderTopVertices(Vector3 pos, int radio, int altura, double rotY){
        int sides = 20; // Aproximación poligonal (20-gon ≈ círculo)
        Vector3[] top = new Vector3[sides];
        
        // Pre-calcular rotación
        double cos = Math.cos(rotY), sin = Math.sin(rotY);
        
        // GENERACIÓN PROCEDURAL: Círculo paramétrico
        for(int i=0;i<sides;i++){
            double angle = 2*Math.PI*i/sides; // Ángulo equidistante
            
            // CURVA PARAMÉTRICA 3D: Círculo en plano XZ
            // x(t) = r*cos(2πt)
            // z(t) = r*sin(2πt)
            // y(t) = altura/2 (constante)
            double x = radio * Math.cos(angle);
            double z = radio * Math.sin(angle);
            
            // ROTACIÓN alrededor de Y
            double xr = x * cos - z * sin;
            double zr = x * sin + z * cos;
            
            // TRASLACIÓN: posición + offset vertical
            top[i] = new Vector3(pos.x + xr, pos.y + altura/2.0, pos.z + zr);
        }
        return top;
    }

    public Vector3[] getCylinderBottomVertices(Vector3 pos, int radio, int altura, double rotY){
        int sides = 20; // Aproximación poligonal
        Vector3[] bottom = new Vector3[sides];
        
        double cos = Math.cos(rotY), sin = Math.sin(rotY);
        
        // GENERACIÓN PROCEDURAL: Círculo paramétrico (igual que top pero en Y negativo)
        for(int i=0;i<sides;i++){
            double angle = 2*Math.PI*i/sides;
            
            // CURVA PARAMÉTRICA: Círculo en plano XZ
            double x = radio * Math.cos(angle);
            double z = radio * Math.sin(angle);
            
            // ROTACIÓN + TRASLACIÓN
            double xr = x * cos - z * sin;
            double zr = x * sin + z * cos;
            bottom[i] = new Vector3(pos.x + xr, pos.y - altura/2.0, pos.z + zr);
        }
        return bottom;
    }

    /**
     * ========================================================================================
     * Constructor - Inicializa el renderer con DOBLE BUFFER
     * ========================================================================================
     * 
     * DOBLE BUFFER (Double Buffering) - REQUISITO DEL PROYECTO:
     * 
     * PROBLEMA SIN DOBLE BUFFER:
     * - Si se renderiza directamente en pantalla, el usuario ve el dibujo parcial
     * - Esto causa parpadeo (flickering) y tearing (imagen cortada)
     * - Especialmente notorio en animaciones @ 60+ FPS
     * 
     * SOLUCIÓN: DOBLE BUFFER
     * Usar 2 BufferedImage:
     * 1. backBuffer: Donde se DIBUJA (invisible al usuario)
     * 2. frontBuffer: Donde se MUESTRA (visible en pantalla)
     * 
     * CICLO DE RENDERIZADO:
     * 1. Limpiar backBuffer (clear)
     * 2. Renderizar todos los objetos en backBuffer (setRGB píxel por píxel)
     * 3. swapBuffers(): Intercambiar referencias (backBuffer ↔ frontBuffer)
     * 4. paintComponent() dibuja frontBuffer en pantalla
     * 
     * VENTAJAS:
     * - No se ve renderizado parcial (imagen siempre completa)
     * - Elimina parpadeo (flickering)
     * - Reduce tearing visual
     * - Permite multi-threading seguro
     * 
    */
    public SoftwareRenderer(int ancho, int alto) {
        this.ancho = ancho;
        this.alto = alto;
        
        // DOBLE BUFFER: Crear dos imágenes separadas
        // frontBuffer = visible en pantalla (lectura por AWT)
        // backBuffer = invisible, donde se dibuja (escritura por RenderThread)
        frontBuffer = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        backBuffer = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        
        // Z-buffer para depth testing (oclusión 3D)
        zBuffer = new double[ancho * alto];
        ownerBuffer = new int[ancho * alto]; // Tracking de triángulos por píxel
    }

    /**
     * ========================================================================================
     * getBuffer - Obtiene el buffer FRONTAL (visible)
     * ========================================================================================
     * 
     * THREAD-SAFETY:
     * - synchronized para evitar tearing cuando se intercambian buffers
     * - AWT Event Dispatch Thread llama este método desde paintComponent()
     * - RenderThread puede estar haciendo swap simultáneamente
     * 
     * @return frontBuffer (imagen visible en pantalla)
     */
    public synchronized BufferedImage getBuffer() {
        return frontBuffer;
    }

    /**
     * ========================================================================================
     * swapBuffers - Intercambia buffers front/back (DOBLE BUFFER)
     * ========================================================================================
     * 
     * OPERACIÓN ATÓMICA:
     * Intercambia las referencias de los buffers para que:
     * - Lo que estaba en backBuffer (recién dibujado) → pase a frontBuffer (visible)
     * - Lo que estaba en frontBuffer → pase a backBuffer (será sobreescrito)
     * 
     * SINCRONIZACIÓN:
     * - synchronized para atomicidad (operación indivisible)
     * - Evita que paintComponent() lea un buffer a medio intercambiar
     * 
     * LLAMADO POR:
     * - RenderThread al finalizar cada frame (~143 veces/segundo)
     * - Después de renderizar todas las entidades y UI
     * 
     * EQUIVALENTE EN OPENGL/DIRECTX:
     * - glSwapBuffers() en OpenGL
     * - Present() en DirectX
     * - Este proyecto usa software rendering (sin GPU)
     */
    public synchronized void swapBuffers(){
        // No diagnostic overlay / hole-filling in production renderer to keep pipeline tight.

        // Intercambio de referencias (operación O(1), instantánea)
        BufferedImage tmp = frontBuffer;
        frontBuffer = backBuffer; // backBuffer (nuevo) → pantalla
        backBuffer = tmp;         // frontBuffer (viejo) → reutilizar
    }

    public void clear(Color c){
        int rgb = c.getRGB();
        for(int y=0; y<alto; y++){
            for(int x=0; x<ancho; x++){
                backBuffer.setRGB(x, y, rgb);
            }
        }
        // reset z-buffer to far (positive infinity) - optimized
        java.util.Arrays.fill(zBuffer, Double.POSITIVE_INFINITY);
        java.util.Arrays.fill(ownerBuffer, -1);
    }

    // ---------------- Pixel / rect / text helpers (HUD) ----------------
    public void drawPixel(int x, int y, Color color){
        if(x>=0 && x<ancho && y>=0 && y<alto){
            int idx = y*ancho + x;
            backBuffer.setRGB(x, y, color.getRGB());
            // HUD pixels: set special owner id so tie-break behavior remains consistent
            ownerBuffer[idx] = -2;
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
        double near = 0.01; // Muy permisivo para evitar clips cerca del suelo
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
                    ownerBuffer[idx] = lineId;
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
                } else if(Math.abs(fz - zBuffer[idx]) <= depthEps){
                    int cur = ownerBuffer[idx];
                    if(cur == -1 || lineId < cur){
                        ownerBuffer[idx] = lineId;
                        zBuffer[idx] = fz;
                        backBuffer.setRGB(xi, yi, rgb);
                    }
                }
            }
            fx += dx; fy += dy; fz += dz;
        }
    }

    // ---------------- Cubo ----------------
    public void drawCube(Vector3[] vertices, Camera cam, Color color){
        int[][] faces = {
            {0,1,2,3}, {4,5,6,7}, {0,4,5,1},
            {1,5,6,2}, {2,6,7,3}, {3,7,4,0}
        };
        for(int[] f : faces){
            Vector3 a = vertices[f[0]];
            Vector3 b = vertices[f[1]];
            Vector3 c = vertices[f[2]];
            Vector3 d = vertices[f[3]];
            double[] pa = project(a, cam);
            double[] pb = project(b, cam);
            double[] pc = project(c, cam);
            double[] pd = project(d, cam);
            if(pa != null && pb != null && pc != null && pd != null){
                drawQuadScreen(pa, pb, pc, pd, color);
            } else {
                // fallback to world-space triangles if projection not available
                drawTriangle(a, b, c, cam, color);
                drawTriangle(a, c, d, cam, color);
            }
        }
    }

    /**
     * ============================================================================================
     * drawCylinder - Renderiza un CILINDRO (PRIMITIVA 3D)
     * ============================================================================================
     * 
     * ESTRUCTURA DEL CILINDRO:
     * - 2 tapas circulares (top y bottom)
     * - Superficie lateral conectando ambas tapas
     * - Total de triángulos: 2n (n por cada tapa) + wireframe lateral
     * 
     * ALGORITMO DE RENDERIZADO:
     * 
     * 1. TAPAS (Triangle Fan):
     *    - Calcular centro promediando todos los vértices
     *    - Para cada par de vértices consecutivos (i, i+1):
     *      * Top: centerTop → top[i] → top[i+1]
     *      * Bottom: centerBottom → bottom[i+1] → bottom[i] (winding invertido)
     * 
     * 2. SUPERFICIE LATERAL (Wireframe):
     *    - Dibujar líneas verticales: top[i] ↔ bottom[i]
     *    - Dibujar líneas horizontales en cada tapa
     * 
     * WINDING ORDER (Orden de vértices):
     * - Top: Counter-clockwise (CCW) desde arriba
     * - Bottom: Clockwise (CW) desde abajo (invertido para consistencia)
     * - Esto permite backface culling correcto
     * 
     * BACKFACE CULLING:
     * - Solo se renderizan caras visibles desde la cámara
     * - Normal apunta hacia afuera del cilindro
     * - Se calcula cross product (v1-v0) × (v2-v0)
     * 
     * USOS EN EL PROYECTO:
     * - Troncos de árboles
     * - Tallos de flores
     * - Posibles extremidades de animales
     * 
     * @param top Array de vértices del círculo superior
     * @param bottom Array de vértices del círculo inferior
     * @param cam Cámara para proyección
     * @param color Color del cilindro
     */
    public void drawCylinder(Vector3[] top, Vector3[] bottom, Camera cam, Color color){
        int n = top.length;
        if(n == 0) return;
        
        // PASO 1: Calcular centros de las tapas (promedio de vértices)
        Vector3 centerTop = new Vector3(0,0,0);
        Vector3 centerBottom = new Vector3(0,0,0);
        for(int i=0;i<n;i++){
            centerTop = centerTop.add(top[i]);
            centerBottom = centerBottom.add(bottom[i]);
        }
        centerTop = centerTop.scale(1.0 / n);       // Centro = promedio
        centerBottom = centerBottom.scale(1.0 / n);

        // PASO 2: Renderizar tapa superior (triangle fan, n triángulos)
        for(int i=0;i<n;i++){
            int next = (i+1)%n; // Siguiente vértice (con wrap-around)
            // Triángulo: centro → vértice i → vértice i+1
            drawTriangle(centerTop, top[i], top[next], cam, color);
        }
        
        // PASO 3: Renderizar tapa inferior (triangle fan, winding invertido)
        for(int i=0;i<n;i++){
            int next = (i+1)%n;
            // Orden invertido para mantener normal apuntando hacia afuera
            drawTriangle(centerBottom, bottom[next], bottom[i], cam, color);
        }

        // PASO 4: Dibujar wireframe de la superficie lateral (definición visual)
        for(int i=0; i<n; i++){
            int next = (i+1)%n;
            // Líneas horizontales (perímetro de las tapas)
            drawLine3D(top[i], top[next], cam, color);
            drawLine3D(bottom[i], bottom[next], cam, color);
            // Líneas verticales (conectan tapas)
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

        double near = 0.01; // Coincidir con project()

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

            // Pixel-snapping: quantize XY to pixel centers so adjacent triangles
            // that share world vertices map to identical screen coordinates.
            // This prevents 1-pixel seams/huecos en caras planas al rotar la cámara.
            p0[0] = Math.floor(p0[0]) + 0.5; p0[1] = Math.floor(p0[1]) + 0.5;
            p1[0] = Math.floor(p1[0]) + 0.5; p1[1] = Math.floor(p1[1]) + 0.5;
            p2[0] = Math.floor(p2[0]) + 0.5; p2[1] = Math.floor(p2[1]) + 0.5;

            // Skip only degenerate triangles (very small projected area).
            // Do NOT cull by sign here — render both sides so external faces don't desaparecer
            // when winding flips due to clipping/projection.
            float area = edgeFunction(p0, p1, p2);
            if (Math.abs(area) <= 1e-9f) continue;
            float absArea = area >= 0 ? area : -area;
            float orient = area >= 0 ? 1f : -1f;
            // Precompute top-left flags that match the oriented edges
            boolean tl01 = orient >= 0 ? isTopLeft(p0, p1) : isTopLeft(p1, p0);
            boolean tl12 = orient >= 0 ? isTopLeft(p1, p2) : isTopLeft(p2, p1);
            boolean tl20 = orient >= 0 ? isTopLeft(p2, p0) : isTopLeft(p0, p2);
 
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
                     // Conservative + deterministic edge rule:
                     // use top-left tie-break when an edge function is (near) zero so adjacent triangles
                     // do not both exclude the pixel; this prevents permanent seams on flat faces.
                     float epsf = 1e-6f;
                     double[] pc = {x + 0.5, y + 0.5};
                     float w0 = edgeFunction(p1, p2, pc) * orient;
                     float w1 = edgeFunction(p2, p0, pc) * orient;
                     float w2 = edgeFunction(p0, p1, pc) * orient;
                     // Determine inclusion per-edge using oriented top-left rule
                     boolean in0 = (w0 > epsf) || (Math.abs(w0) <= epsf && tl12);
                     boolean in1 = (w1 > epsf) || (Math.abs(w1) <= epsf && tl20);
                     boolean in2 = (w2 > epsf) || (Math.abs(w2) <= epsf && tl01);
                     boolean covered = in0 && in1 && in2;
                     // If center test fails, fallback to corner conservative test (keeps prior behavior)
                     if(!covered){
                        double[][] corners = {
                            {x + 0.0, y + 0.0},
                            {x + 1.0, y + 0.0},
                            {x + 1.0, y + 1.0},
                            {x + 0.0, y + 1.0}
                        };
                        for(int ci=0; ci<4 && !covered; ci++){
                            double[] cc = corners[ci];
                            float cc0 = edgeFunction(p1, p2, cc) * orient;
                            float cc1 = edgeFunction(p2, p0, cc) * orient;
                            float cc2 = edgeFunction(p0, p1, cc) * orient;
                            boolean cin0 = (cc0 > epsf) || (Math.abs(cc0) <= epsf && tl12);
                            boolean cin1 = (cc1 > epsf) || (Math.abs(cc1) <= epsf && tl20);
                            boolean cin2 = (cc2 > epsf) || (Math.abs(cc2) <= epsf && tl01);
                            if(cin0 && cin1 && cin2) covered = true;
                        }
                     }
                     if(covered){
                         double alpha = w0 / absArea;
                         double beta = w1 / absArea;
                         double gamma = w2 / absArea;
 
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
                             int rr = (int)(color.getRed() * lit);
                             int gg = (int)(color.getGreen() * lit);
                             int bb = (int)(color.getBlue() * lit);
                             backBuffer.setRGB(x, y, packRGB(rr, gg, bb));
                         } else if(Math.abs(z - zBuffer[idx]) <= depthEps){
                             int cur = ownerBuffer[idx];
                             if(cur == -1 || triId < cur){
                                 ownerBuffer[idx] = triId;
                                 zBuffer[idx] = z;
                                 double lit = amb + (1.0 - amb) * intensity;
                                 int rr = (int)(color.getRed() * lit);
                                 int gg = (int)(color.getGreen() * lit);
                                 int bb = (int)(color.getBlue() * lit);
                                 backBuffer.setRGB(x, y, packRGB(rr, gg, bb));
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
        // Pixel-snap at the START of triangle rasterization
        p0[0] = Math.floor(p0[0]) + 0.5; p0[1] = Math.floor(p0[1]) + 0.5;
        p1[0] = Math.floor(p1[0]) + 0.5; p1[1] = Math.floor(p1[1]) + 0.5;
        p2[0] = Math.floor(p2[0]) + 0.5; p2[1] = Math.floor(p2[1]) + 0.5;
        
        float area = edgeFunction(p0, p1, p2);
        if(Math.abs(area) <= 1e-9f) return;
        float absArea = area >= 0 ? area : -area;
        float orient = area >= 0 ? 1f : -1f;
        // Precompute top-left flags that match the oriented edges
        boolean tl01 = orient >= 0 ? isTopLeft(p0, p1) : isTopLeft(p1, p0);
        boolean tl12 = orient >= 0 ? isTopLeft(p1, p2) : isTopLeft(p2, p1);
        boolean tl20 = orient >= 0 ? isTopLeft(p2, p0) : isTopLeft(p0, p2);

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
                float w0 = edgeFunction(p1, p2, pc) * orient;
                float w1 = edgeFunction(p2, p0, pc) * orient;
                float w2 = edgeFunction(p0, p1, pc) * orient;
                float epsf = 1e-6f;
                boolean in0 = (w0 > epsf) || (Math.abs(w0) <= epsf && tl12);
                boolean in1 = (w1 > epsf) || (Math.abs(w1) <= epsf && tl20);
                boolean in2 = (w2 > epsf) || (Math.abs(w2) <= epsf && tl01);
                boolean covered = in0 && in1 && in2;
                if(!covered){
                    double[][] corners = {
                        {x + 0.0, y + 0.0},
                        {x + 1.0, y + 0.0},
                        {x + 1.0, y + 1.0},
                        {x + 0.0, y + 1.0}
                    };
                    for(int ci=0; ci<4 && !covered; ci++){
                        double[] cc = corners[ci];
                        float cc0 = edgeFunction(p1, p2, cc) * orient;
                        float cc1 = edgeFunction(p2, p0, cc) * orient;
                        float cc2 = edgeFunction(p0, p1, cc) * orient;
                        boolean cin0 = (cc0 > epsf) || (Math.abs(cc0) <= epsf && tl12);
                        boolean cin1 = (cc1 > epsf) || (Math.abs(cc1) <= epsf && tl20);
                        boolean cin2 = (cc2 > epsf) || (Math.abs(cc2) <= epsf && tl01);
                        if(cin0 && cin1 && cin2) covered = true;
                    }
                }
                if(covered){
                    double alpha = w0 / absArea;
                    double beta  = w1 / absArea;
                    double gamma = w2 / absArea;
                    double z = alpha * p0[2] + beta * p1[2] + gamma * p2[2];
                    int idx = y * ancho + x;
                    if(z < zBuffer[idx] - depthEps){
                        zBuffer[idx] = z;
                        ownerBuffer[idx] = triId;
                        int rr = (int)(color.getRed() * lit);
                        int gg = (int)(color.getGreen() * lit);
                        int bb = (int)(color.getBlue() * lit);
                        backBuffer.setRGB(x, y, packRGB(rr, gg, bb));
                    } else if(Math.abs(z - zBuffer[idx]) <= depthEps){
                        int cur = ownerBuffer[idx];
                        if(cur == -1 || triId < cur){
                            ownerBuffer[idx] = triId;
                            zBuffer[idx] = z;
                            int rr = (int)(color.getRed() * lit);
                            int gg = (int)(color.getGreen() * lit);
                            int bb = (int)(color.getBlue() * lit);
                            backBuffer.setRGB(x, y, packRGB(rr, gg, bb));
                        }
                    }
                }
            }
        }
    }

    // Rasterize a convex quad given in projected screen-space coordinates (p = {x_screen, y_screen, cam_z})
    // Split into two triangles and render using drawTriangleScreen to ensure consistent edge-testing
    public void drawQuadScreen(double[] p0, double[] p1, double[] p2, double[] p3, Color color){
        if(p0 == null || p1 == null || p2 == null || p3 == null) return;

        // DON'T pixel-snap here - let drawTriangleScreen handle it to ensure
        // both triangles share the exact same snapped diagonal vertices
        drawTriangleScreen(p0, p1, p2, color);
        drawTriangleScreen(p0, p2, p3, color);
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

    // small helper to clamp and pack colors to int RGB to avoid allocating Color objects per pixel
    private int clampColor(int v){
        if(v < 0) return 0;
        if(v > 255) return 255;
        return v;
    }

    private int packRGB(int r, int g, int b){
        return (clampColor(r) << 16) | (clampColor(g) << 8) | clampColor(b);
    }

    public void fillTinyHoles(){
        // Skip hole filling in production for speed - causes lag
        // Uncomment if visual quality is more important than performance
        // int w = this.ancho, h = this.alto;
        // ... existing hole-fill code ...
    }

    public void fillHorizontalSeams(){
        // Skip seam filling in production for speed
        // Uncomment if visual quality is more important than performance
        // int w = this.ancho, h = this.alto;
        // ... existing seam-fill code ...
    }

    /**
     * Calculate Lambert shading based on surface normal and light direction.
     * Light is positioned above and behind the camera (top-right-back).
     * Implementa: ILUMINACIÓN por píxel usando modelo Lambert.
     */
    public Color applyLambertShading(Color baseColor, Vector3 normal) {
        if (normal == null) return baseColor;
        normal = normal.normalize();
        
        // Light direction: fixed position (top-right-back)
        Vector3 lightDir = new Vector3(0.5, 1, 0.3).normalize();
        
        // Calculate dot product (clamped to 0.3-1.0 range for ambient + diffuse)
        double intensity = Math.max(0.3, Math.min(1.0, 
            Math.abs(normal.x * lightDir.x + normal.y * lightDir.y + normal.z * lightDir.z)
        ));
        
        // Apply intensity to color
        int r = (int)(baseColor.getRed() * intensity);
        int g = (int)(baseColor.getGreen() * intensity);
        int b = (int)(baseColor.getBlue() * intensity);
        
        return new Color(Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    /**
     * Calculate face normal from three vertices using cross product.
     * Implementa TRANSFORMACIÓN: cálculo de normal mediante producto cruz.
     */
    public Vector3 calculateFaceNormal(Vector3 v0, Vector3 v1, Vector3 v2) {
        Vector3 edge1 = v1.subtract(v0);
        Vector3 edge2 = v2.subtract(v0);
        Vector3 normal = edge1.cross(edge2);
        double len = Math.sqrt(normal.x * normal.x + normal.y * normal.y + normal.z * normal.z);
        if (len == 0) return new Vector3(0, 0, 1);
        return new Vector3(normal.x / len, normal.y / len, normal.z / len);
    }

    /**
     * Draw a cube with Lambert shading for realistic lighting.
     */
    public void drawCubeShaded(Vector3[] vertices, Camera cam, Color color) {
        // Define faces with vertex indices (cube has 6 faces)
        int[][] faces = {
            {0, 1, 2, 3}, // front
            {4, 5, 6, 7}, // back
            {0, 4, 5, 1}, // right
            {1, 5, 6, 2}, // top
            {2, 6, 7, 3}, // left
            {3, 7, 4, 0}  // bottom
        };
        
        Vector3[] faceNormals = {
            calculateFaceNormal(vertices[0], vertices[1], vertices[2]),
            calculateFaceNormal(vertices[4], vertices[6], vertices[5]),
            calculateFaceNormal(vertices[0], vertices[5], vertices[4]),
            calculateFaceNormal(vertices[1], vertices[6], vertices[5]),
            calculateFaceNormal(vertices[2], vertices[7], vertices[6]),
            calculateFaceNormal(vertices[3], vertices[4], vertices[7])
        };
        
        for (int f = 0; f < faces.length; f++) {
            int[] face = faces[f];
            Vector3 a = vertices[face[0]];
            Vector3 b = vertices[face[1]];
            Vector3 c = vertices[face[2]];
            Vector3 d = vertices[face[3]];
            
            Color shadedColor = applyLambertShading(color, faceNormals[f]);
            
            double[] pa = project(a, cam);
            double[] pb = project(b, cam);
            double[] pc = project(c, cam);
            double[] pd = project(d, cam);
            
            if (pa != null && pb != null && pc != null && pd != null) {
                drawQuadScreen(pa, pb, pc, pd, shadedColor);
            } else {
                drawTriangle(a, b, c, cam, shadedColor);
                drawTriangle(a, c, d, cam, shadedColor);
            }
        }
    }
}

