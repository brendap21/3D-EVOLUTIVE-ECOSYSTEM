package math;

/**
 * ============================================================================================
 * Matrix4 - Matriz 4x4 para transformaciones 3D
 * ============================================================================================
 * 
 * PROPÓSITO:
 * Representa transformaciones afines en espacio 3D usando coordenadas homogéneas.
 * Permite combinar traslación, rotación y escala en una sola matriz.
 * 
 * CONCEPTOS FUNDAMENTALES:
 * 1. COORDENADAS HOMOGÉNEAS:
 *    - Punto 3D (x, y, z) se representa como (x, y, z, 1) en 4D
 *    - La cuarta componente (w=1) permite representar traslaciones con multiplicación
 *    - Matriz 4x4 puede codificar rotación, escala y traslación simultáneamente
 * 
 * 2. ESTRUCTURA DE LA MATRIZ:
 *    [R R R Tx]   R = Rotación/Escala (3x3)
 *    [R R R Ty]   T = Traslación (columna derecha)
 *    [R R R Tz]   Última fila = [0 0 0 1] para transformaciones afines
 *    [0 0 0 1 ]
 * 
 * 3. TRANSFORMACIONES IMPLEMENTADAS:
 *    - Rotación alrededor eje Y (Yaw): Para rotar animales horizontalmente
 *    - Rotación alrededor eje X (Pitch): Para inclinaciones
 *    - Traslación: Para mover objetos en el espacio
 * 
 * 4. ORDEN DE TRANSFORMACIONES:
 *    - Matriz Model: Escala → Rotación → Traslación (SRT)
 *    - Se aplica de derecha a izquierda: M = T * R * S
 *    - Orden importa: rotar luego trasladar ≠ trasladar luego rotar
 * 
 * MATRICES DE ROTACIÓN (Fórmulas estándar):
 * - RotX: Rota alrededor del eje X (pitch, cabeceo)
 * - RotY: Rota alrededor del eje Y (yaw, guiñada)
 * - RotZ: Rota alrededor del eje Z (roll, alabeo) - NO implementado aquí
 * 
 * ============================================================================================
 */
public class Matrix4 {
    // Matriz 4x4 almacenada en formato row-major: m[fila][columna]
    public float[][] m;

    /**
     * Constructor: Inicializa matriz 4x4 con ceros.
     * Para obtener matriz identidad, establecer m[i][i] = 1.
     */
    public Matrix4(){
        m = new float[4][4];
    }

    /**
     * ========================================================================================
     * rotationY - Matriz de rotación alrededor del eje Y (Yaw)
     * ========================================================================================
     * 
     * PROPÓSITO:
     * Rota puntos alrededor del eje Y (vertical). Usado para girar animales horizontalmente.
     * 
     * FÓRMULA (convención right-handed):
     * [  cos(θ)   0   sin(θ)   0 ]
     * [    0      1     0      0 ]
     * [ -sin(θ)   0   cos(θ)   0 ]
     * [    0      0     0      1 ]
     * 
     * INTERPRETACIÓN:
     * - Eje Y permanece sin cambios (segunda fila/columna = identidad)
     * - Puntos en el plano XZ rotan θ radianes alrededor del origen
     * - Rotación positiva: sentido antihorario visto desde arriba (+Y)
     * 
     * APLICACIÓN:
     * Si P = (x, y, z), entonces P' = RotY * P
     * x' =  x*cos(θ) + z*sin(θ)
     * y' =  y                      [sin cambios]
     * z' = -x*sin(θ) + z*cos(θ)
     * 
     * USO EN EL PROYECTO:
     * - Rotar animales para que miren en dirección de movimiento
     * - Rotación de cámara horizontal (yaw)
     * 
     * @param angle Ángulo de rotación en radianes (positivo = antihorario)
     * @return Matriz 4x4 de rotación alrededor de Y
     */
    public static Matrix4 rotationY(double angle){
        Matrix4 r = new Matrix4();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        r.m[0][0] = (float) cos; r.m[0][1] = 0; r.m[0][2] = (float) sin; r.m[0][3] = 0;
        r.m[1][0] = 0;           r.m[1][1] = 1; r.m[1][2] = 0;           r.m[1][3] = 0;
        r.m[2][0] = (float)-sin; r.m[2][1] = 0; r.m[2][2] = (float) cos; r.m[2][3] = 0;
        r.m[3][0] = 0;           r.m[3][1] = 0; r.m[3][2] = 0;           r.m[3][3] = 1;

        return r;
    }

    /**
     * ========================================================================================
     * rotationX - Matriz de rotación alrededor del eje X (Pitch)
     * ========================================================================================
     * 
     * PROPÓSITO:
     * Rota puntos alrededor del eje X (horizontal). Usado para inclinaciones.
     * 
     * FÓRMULA (convención right-handed):
     * [ 1     0        0      0 ]
     * [ 0  cos(θ)  -sin(θ)   0 ]
     * [ 0  sin(θ)   cos(θ)   0 ]
     * [ 0     0        0      1 ]
     * 
     * INTERPRETACIÓN:
     * - Eje X permanece sin cambios (primera fila/columna = identidad)
     * - Puntos en el plano YZ rotan θ radianes alrededor del eje X
     * - Rotación positiva: sentido antihorario visto desde +X
     * 
     * APLICACIÓN:
     * Si P = (x, y, z), entonces P' = RotX * P
     * x' =  x                      [sin cambios]
     * y' =  y*cos(θ) - z*sin(θ)
     * z' =  y*sin(θ) + z*cos(θ)
     * 
     * USO EN EL PROYECTO:
     * - Inclinaciones de objetos
     * - Rotación de cámara vertical (pitch)
     * - Menos común que RotY en este proyecto
     * 
     * @param angle Ángulo de rotación en radianes (positivo = antihorario desde +X)
     * @return Matriz 4x4 de rotación alrededor de X
     */
    public static Matrix4 rotationX(double angle){
        Matrix4 r = new Matrix4();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        r.m[0][0] = 1; r.m[0][1] = 0;    r.m[0][2] = 0;    r.m[0][3] = 0;
        r.m[1][0] = 0; r.m[1][1] = (float) cos; r.m[1][2] = (float) -sin; r.m[1][3] = 0;
        r.m[2][0] = 0; r.m[2][1] = (float) sin; r.m[2][2] = (float) cos;  r.m[2][3] = 0;
        r.m[3][0] = 0; r.m[3][1] = 0;    r.m[3][2] = 0;    r.m[3][3] = 1;

        return r;
    }

    /**
     * ========================================================================================
     * translation - Matriz de traslación (desplazamiento)
     * ========================================================================================
     * 
     * PROPÓSITO:
     * Desplaza puntos por un offset (x, y, z). Implementa TRASLACIÓN en 3D.
     * 
     * FÓRMULA:
     * [ 1  0  0  Tx ]
     * [ 0  1  0  Ty ]
     * [ 0  0  1  Tz ]
     * [ 0  0  0  1  ]
     * 
     * INTERPRETACIÓN:
     * - Submatriz 3x3 es identidad (no rota ni escala)
     * - Columna derecha (Tx, Ty, Tz) contiene el desplazamiento
     * - Coordenadas homogéneas permiten codificar traslación con multiplicación
     * 
     * APLICACIÓN:
     * Si P = (x, y, z, 1), entonces P' = Trans * P
     * x' = x + Tx
     * y' = y + Ty
     * z' = z + Tz
     * w' = 1 [sin cambios]
     * 
     * USO EN EL PROYECTO:
     * - Colocar entidades en posiciones del mundo
     * - Mover objetos frame a frame
     * - Parte de la matriz Model (combinada con rotación)
     * 
     * NOTA:
     * Esta es la razón por la que usamos matrices 4x4 en lugar de 3x3:
     * sin coordenadas homogéneas, la traslación no se puede representar como multiplicación.
     * 
     * @param x Desplazamiento en X
     * @param y Desplazamiento en Y
     * @param z Desplazamiento en Z
     * @return Matriz 4x4 de traslación
     */
    public static Matrix4 translation(double x, double y, double z){
        Matrix4 r = new Matrix4();
        r.m[0][0] = 1; r.m[0][1] = 0; r.m[0][2] = 0; r.m[0][3] = (float)x;
        r.m[1][0] = 0; r.m[1][1] = 1; r.m[1][2] = 0; r.m[1][3] = (float)y;
        r.m[2][0] = 0; r.m[2][1] = 0; r.m[2][2] = 1; r.m[2][3] = (float)z;
        r.m[3][0] = 0; r.m[3][1] = 0; r.m[3][2] = 0; r.m[3][3] = 1;
        return r;
    }

    /**
     * ========================================================================================
     * multiply - Multiplica la matriz por un vector (transformación)
     * ========================================================================================
     * 
     * PROPÓSITO:
     * Aplica la transformación representada por esta matriz a un punto/vector 3D.
     * 
     * PROCESO:
     * 1. Convertir Vector3 a coordenadas homogéneas (x, y, z, 1)
     * 2. Multiplicar matriz 4x4 por vector 4D (producto matriz-vector)
     * 3. Convertir resultado de vuelta a Vector3 (descartar w)
     * 
     * FÓRMULA:
     * Para cada componente i del resultado:
     * result[i] = Σ(j=0 to 3) M[i][j] * v[j]
     * 
     * En forma expandida:
     * x' = m[0][0]*x + m[0][1]*y + m[0][2]*z + m[0][3]*1
     * y' = m[1][0]*x + m[1][1]*y + m[1][2]*z + m[1][3]*1
     * z' = m[2][0]*x + m[2][1]*y + m[2][2]*z + m[2][3]*1
     * w' = m[3][0]*x + m[3][1]*y + m[3][2]*z + m[3][3]*1  [descartado]
     * 
     * ORDEN DE OPERACIONES:
     * Si tenemos múltiples transformaciones, se aplican de derecha a izquierda:
     * P' = M3 * M2 * M1 * P
     * Ejemplo: P' = Translation * RotationY * Scale * P
     * 
     * @param v Vector 3D a transformar
     * @return Vector 3D transformado
     */
    public Vector3 multiply(Vector3 v){
        // Convertir a coordenadas homogéneas (x, y, z, 1)
        float[] p = {(float)v.x, (float)v.y, (float)v.z, 1};
        float[] res = new float[4];

        // Multiplicación matriz-vector: res = M * p
        for(int i=0;i<4;i++){
            res[i] = 0;
            for(int j=0;j<4;j++){
                res[i] += m[i][j]*p[j];
            }
        }
        
        // Convertir de vuelta a Vector3 (descartar componente w = res[3])
        return new Vector3(res[0], res[1], res[2]);
    }
}
