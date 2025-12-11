package math;

/**
 * ============================================================================================
 * Transform - Transformaciones 3D → 2D (Pipeline de Proyección)
 * ============================================================================================
 * 
 * PROPÓSITO:
 * Implementa el PIPELINE DE GRÁFICAS 3D: transforma puntos del mundo 3D a coordenadas
 * de pantalla 2D. Este es el núcleo matemático del renderizado.
 * 
 * PIPELINE DE TRANSFORMACIONES:
 * 1. World Space → Camera Space (View Transform)
 * 2. Camera Space → Clip Space (Projection)
 * 3. Clip Space → Screen Space (Viewport Transform)
 * 
 * CONCEPTOS FUNDAMENTALES:
 * 1. ESPACIOS DE COORDENADAS:
 *    - World Space: Sistema global donde viven todas las entidades
 *    - Camera Space: Sistema local a la cámara (origen = posición cámara)
 *    - Screen Space: Coordenadas de píxeles en la pantalla
 * 
 * 2. PROYECCIÓN PERSPECTIVA vs ORTOGRÁFICA:
 *    - Perspectiva: División por profundidad (objetos lejanos son pequeños)
 *    - Ortográfica: Sin división (objetos mantienen tamaño independiente de distancia)
 * 
 * 3. VIEW MATRIX:
 *    - Construida desde base ortonormal de la cámara (right, up, forward)
 *    - Transforma puntos de world space a camera space
 *    - Usa productos punto con vectores de la base
 * 
 * ============================================================================================
 */
public class Transform {

    /**
     * ========================================================================================
     * project - Proyecta un punto 3D a coordenadas de pantalla 2D
     * ========================================================================================
     * 
     * ALGORITMO COMPLETO:
     * 
     * PASO 1: CALCULAR VECTOR RELATIVO A LA CÁMARA
     * -------------------------------------------------
     * rel = punto - camPos
     * Esto da el vector desde la cámara hasta el punto en world space.
     * 
     * PASO 2: CONSTRUIR BASE ORTONORMAL DE LA CÁMARA
     * -------------------------------------------------
     * La cámara tiene su propio sistema de coordenadas definido por 3 vectores:
     * - forward: Dirección hacia donde mira (eje Z local)
     * - right: Dirección "derecha" de la cámara (eje X local)
     * - up: Dirección "arriba" de la cámara (eje Y local)
     * 
     * Construcción de la base:
     * a) forward ya es conocido (de la cámara)
     * b) right = worldUp × forward (perpendicular a ambos)
     * c) up = forward × right (perpendicular a ambos, completa base derecha)
     * 
     * ORTONORMALIDAD:
     * - Ortogonal: Los 3 vectores son perpendiculares entre sí
     * - Normal: Los 3 vectores tienen longitud 1 (unitarios)
     * - Propiedad clave: Producto punto con base ortonormal = proyección
     * 
     * PASO 3: TRANSFORMAR A CAMERA SPACE (VIEW TRANSFORM)
     * -------------------------------------------------
     * Para transformar el vector relativo a coordenadas de cámara, proyectamos
     * sobre cada eje de la base usando producto punto:
     * 
     * cx = rel · right   [cuánto del vector va en dirección right]
     * cy = rel · up      [cuánto del vector va en dirección up]
     * cz = rel · forward [cuánto del vector va en dirección forward = profundidad]
     * 
     * Esto es equivalente a multiplicar por la matriz de vista (view matrix):
     * [right.x   right.y   right.z  ]   [rel.x]
     * [up.x      up.y      up.z     ] × [rel.y]
     * [forward.x forward.y forward.z]   [rel.z]
     * 
     * PASO 4: PROYECCIÓN (PERSPECTIVA U ORTOGRÁFICA)
     * -------------------------------------------------
     * a) PROYECCIÓN ORTOGRÁFICA (paralela):
     *    - Ignora profundidad para el cálculo de posición
     *    - x2d = cx * fov + ancho/2
     *    - y2d = alto/2 - cy * fov
     *    - Objetos lejanos y cercanos tienen mismo tamaño
     *    - Usado en CAD, arquitectura, juegos isométricos
     * 
     * b) PROYECCIÓN PERSPECTIVA (realista):
     *    - DIVIDE por profundidad (cz)
     *    - x2d = (cx * fov) / cz + ancho/2
     *    - y2d = alto/2 - (cy * fov) / cz
     *    - Objetos lejanos (cz grande) → coordenadas cerca del centro
     *    - Objetos cercanos (cz pequeño) → coordenadas lejos del centro
     *    - Simula cómo funciona el ojo humano y cámaras reales
     * 
     * FÓRMULA DE PROYECCIÓN PERSPECTIVA:
     * La división por cz implementa la PROYECCIÓN GEOMÉTRICA:
     * Triángulos similares: x_screen / distancia_pantalla = x_mundo / profundidad
     * Por lo tanto: x_screen = (x_mundo * distancia_pantalla) / profundidad
     * 
     * PASO 5: VIEWPORT TRANSFORM (centrado y flip de Y)
     * -------------------------------------------------
     * - + ancho/2: Centra horizontalmente (origen en centro de pantalla)
     * - alto/2 - y: Invierte Y porque pantallas crecen hacia abajo
     *               En 3D: +Y = arriba, En pantalla: +Y = abajo
     * 
     * MANEJO DE CASOS ESPECIALES:
     * - cz = 0: Punto en el plano de la cámara → división por cero → usar epsilon
     * - cz < 0: Punto detrás de la cámara → no debería dibujarse (clipping)
     * 
     * VALOR DE RETORNO:
     * Vector3 donde:
     * - x, y: Coordenadas de píxel en pantalla
     * - z: Profundidad (guardada para z-buffer test)
     * 
     * @param punto Punto 3D en world space
     * @param cam Cámara (posición, orientación, FOV)
     * @param ancho Ancho de la pantalla en píxeles
     * @param alto Alto de la pantalla en píxeles
     * @return Vector3 con (x_pantalla, y_pantalla, profundidad)
     */
    public static Vector3 project(Vector3 punto, Camera cam, int ancho, int alto){
        // ================================================================================
        // PASO 1: CALCULAR VECTOR RELATIVO A LA CÁMARA
        // ================================================================================
        Vector3 camPos = cam.getPosicion();
        Vector3 rel = new Vector3(punto.x - camPos.x, punto.y - camPos.y, punto.z - camPos.z);

        // ================================================================================
        // PASO 2: CONSTRUIR BASE ORTONORMAL DE LA CÁMARA
        // ================================================================================
        // Forward: Dirección de vista (hacia donde mira la cámara)
        Vector3 forward = cam.getForward().normalize();
        
        // World Up: Asumimos que "arriba" en el mundo es +Y (0, 1, 0)
        Vector3 worldUp = new Vector3(0, 1, 0);
        
        // Right: Perpendicular a forward y worldUp (producto cruz)
        // worldUp × forward da un vector apuntando a la derecha de la cámara
        Vector3 right = worldUp.cross(forward).normalize();
        
        // Up: Perpendicular a forward y right (completa base ortonormal)
        // forward × right da un vector apuntando "arriba" para la cámara
        Vector3 up = forward.cross(right).normalize();

        // ================================================================================
        // PASO 3: TRANSFORMAR A CAMERA SPACE (producto punto con base ortonormal)
        // ================================================================================
        // Proyectar el vector relativo sobre cada eje de la base de la cámara
        // Esto transforma de world space a camera space
        double cx = rel.x * right.x + rel.y * right.y + rel.z * right.z;     // Componente derecha
        double cy = rel.x * up.x    + rel.y * up.y    + rel.z * up.z;        // Componente arriba
        double cz = rel.x * forward.x + rel.y * forward.y + rel.z * forward.z; // Componente profundidad

        // ================================================================================
        // PASO 4: EVITAR DIVISIÓN POR CERO
        // ================================================================================
        // Si cz = 0, el punto está exactamente en el plano de la cámara
        // Usar epsilon pequeño para evitar división por cero
        if (cz == 0) cz = 0.0001;

        // ================================================================================
        // PASO 5: PROYECCIÓN (PERSPECTIVA U ORTOGRÁFICA)
        // ================================================================================
        double fov = cam.getFov(); // Distancia focal (controla "zoom")
        
        if (cam.isOrthographic()) {
            // PROYECCIÓN ORTOGRÁFICA (paralela)
            // Sin división por profundidad → objetos mantienen tamaño
            double x2d = cx * fov + ancho / 2.0;
            double y2d = alto / 2.0 - cy * fov;  // Flip Y (pantalla crece hacia abajo)
            return new Vector3(x2d, y2d, cz);
        } else {
            // PROYECCIÓN PERSPECTIVA (realista)
            // División por profundidad → objetos lejanos son más pequeños
            double x2d = (cx * fov) / cz + ancho / 2.0;
            double y2d = alto / 2.0 - (cy * fov) / cz;  // Flip Y
            return new Vector3(x2d, y2d, cz);
        }
    }
}
