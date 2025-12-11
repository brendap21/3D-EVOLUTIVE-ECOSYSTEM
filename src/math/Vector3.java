package math;

/**
 * ============================================================================================
 * Vector3 - Vector 3D (geometría vectorial y álgebra lineal)
 * ============================================================================================
 * 
 * PROPÓSITO:
 * Representa un punto o dirección en el espacio 3D usando coordenadas cartesianas (x, y, z).
 * 
 * USOS EN EL PROYECTO:
 * - Posiciones de entidades en el mundo
 * - Direcciones de movimiento (forward, right, up de la cámara)
 * - Normales de caras (para backface culling e iluminación)
 * - Vectores de velocidad y aceleración
 * 
 * CONCEPTOS MATEMÁTICOS:
 * 1. ESPACIO VECTORIAL 3D:
 *    - Tres componentes: x (horizontal), y (vertical), z (profundidad)
 *    - Sistema de coordenadas derecho: thumb=X, index=Y, middle=Z
 *    - Operaciones: suma, resta, escala, producto punto, producto cruz
 * 
 * 2. INMUTABILIDAD:
 *    - Todos los métodos retornan NUEVOS vectores (no modifican el original)
 *    - Previene bugs sutiles en cálculos complejos
 *    - Ejemplo: v1.add(v2) no modifica v1, retorna v1+v2
 * 
 * ============================================================================================
 */
public class Vector3 {
    // Componentes del vector (públicos para acceso directo)
    public double x, y, z;

    /**
     * Constructor: Crea un vector 3D con componentes especificadas.
     * 
     * @param x Componente horizontal (izquierda-derecha)
     * @param y Componente vertical (abajo-arriba)
     * @param z Componente de profundidad (adelante-atrás)
     */
    public Vector3(double x, double y, double z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * SUMA VECTORIAL: Suma componente a componente.
     * Geométricamente: coloca el inicio de v en la punta de this.
     * Usado para: desplazamientos, combinar fuerzas, actualizar posiciones.
     * F\u00f3rmula: (x1, y1, z1) + (x2, y2, z2) = (x1+x2, y1+y2, z1+z2)
     * 
     * @param v Vector a sumar
     * @return Nuevo vector this + v
     */
    public Vector3 add(Vector3 v){
        return new Vector3(this.x + v.x, this.y + v.y, this.z + v.z);
    }

    /**
     * RESTA VECTORIAL: Resta componente a componente.
     * Geométricamente: vector que va desde la punta de v hasta la punta de this.
     * Usado para: calcular direcciones entre puntos, diferencias de posición.
     * Fórmula: (x1, y1, z1) - (x2, y2, z2) = (x1-x2, y1-y2, z1-z2)
     * 
     * @param v Vector a restar
     * @return Nuevo vector this - v
     */
    public Vector3 subtract(Vector3 v){
        return new Vector3(this.x - v.x, this.y - v.y, this.z - v.z);
    }

    /**
     * ESCALA (multiplicación por escalar): Multiplica cada componente por s.
     * Geométricamente: alarga o acorta el vector sin cambiar dirección.
     * Si s<0, invierte la dirección. Si s=0, vector cero.
     * Usado para: velocidades, amplificar desplazamientos, invertir direcciones.
     * Fórmula: s * (x, y, z) = (s*x, s*y, s*z)
     * 
     * @param s Escalar (número)
     * @return Nuevo vector this * s
     */
    public Vector3 scale(double s){
        return new Vector3(this.x * s, this.y * s, this.z * s);
    }

    /**
     * NORMALIZACIÓN: Convierte el vector a longitud 1 manteniendo dirección.
     * Vector unitario = vector / longitud
     * Usado para: direcciones puras (sin magnitud), cálculos de iluminación.
     * Un vector normalizado se llama "vector unitario" o "versor".
     * Fórmula: v_norm = v / ||v|| donde ||v|| = sqrt(x² + y² + z²)
     * 
     * @return Vector unitario en la misma dirección que this
     */
    public Vector3 normalize(){
        double len = Math.sqrt(x*x + y*y + z*z);
        if(len == 0) return new Vector3(0,0,0); // Evitar división por cero
        return new Vector3(x/len, y/len, z/len);
    }

    /**
     * PRODUCTO CRUZ (Cross Product): Calcula vector perpendicular a this y v.
     * Resultado: vector perpendicular a ambos, magnitud = área del paralelogramo.
     * Dirección: regla de la mano derecha (dedos this, doblar hacia v, pulgar = resultado).
     * Usado para: calcular normales de caras, sistema de coordenadas de cámara.
     * 
     * Fórmula: this × v = (y1*z2 - z1*y2, z1*x2 - x1*z2, x1*y2 - y1*x2)
     * 
     * PROPIEDADES:
     * - Anticonmutativo: a × b = -(b × a)
     * - a × a = (0, 0, 0)
     * - Resultado perpendicular a ambos inputs
     * 
     * @param v Vector con el que calcular el producto cruz
     * @return Nuevo vector perpendicular a this y v
     */
    public Vector3 cross(Vector3 v){
        return new Vector3(
            this.y*v.z - this.z*v.y,
            this.z*v.x - this.x*v.z,
            this.x*v.y - this.y*v.x
        );
    }
    
    /**
     * COPIA: Crea una copia independiente del vector.
     * Útil para evitar modificaciones accidentales.
     * 
     * @return Nuevo vector con los mismos componentes
     */
    public Vector3 copy() {
        return new Vector3(this.x, this.y, this.z);
    }
    
    /**
     * PRODUCTO PUNTO (Dot Product): Suma de productos de componentes.
     * Fórmula: this · v = x1*x2 + y1*y2 + z1*z2
     * 
     * INTERPRETACIÓN GEOMÉTRICA:
     * - dot(v) = ||this|| * ||v|| * cos(θ) donde θ = ángulo entre vectores
     * - Si dot > 0: ángulo agudo (vectores apuntan en direcciones similares)
     * - Si dot = 0: perpendiculares (ángulo 90°)
     * - Si dot < 0: ángulo obtuso (direcciones opuestas)
     * 
     * USOS:
     * - Backface culling: dot(normal, view) < 0 → cara visible
     * - Proyección de un vector sobre otro
     * - Calcular ángulo entre vectores: θ = acos(dot / (len1 * len2))
     * - Iluminación difusa: intensidad = max(0, dot(normal, luz))
     * 
     * @param v Vector con el que calcular el producto punto
     * @return Escalar (número) resultado del producto punto
     */
    public double dot(Vector3 v) {
        return this.x * v.x + this.y * v.y + this.z * v.z;
    }
    
    /**
     * LONGITUD (magnitud, norma): Distancia del origen al punto.
     * Fórmula: ||v|| = sqrt(x² + y² + z²) (Teorema de Pitágoras en 3D)
     * 
     * Usado para:
     * - Calcular distancias entre puntos
     * - Normalización (dividir por longitud)
     * - Verificar si un vector es unitario (length ≈ 1)
     * 
     * @return Longitud del vector (siempre ≥ 0)
     */
    public double length() {
        return Math.sqrt(x*x + y*y + z*z);
    }
}
