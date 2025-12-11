package math;

/**
 * ============================================================================================
 * Camera - Cámara 3D con controles FPS (First-Person Shooter)
 * ============================================================================================
 * 
 * PROPÓSITO:
 * Representa el punto de vista del observador en el espacio 3D.
 * Define desde dónde y hacia dónde se ve la escena.
 * 
 * CONCEPTOS DE GRÁFICAS 3D:
 * 1. SISTEMA DE COORDENADAS DE CÁMARA:
 *    - Position (posición): Donde está el ojo del observador
 *    - Forward (adelante): Hacia dónde mira la cámara (dirección de vista)
 *    - Up (arriba): Qué dirección es "arriba" para la cámara
 *    - Right (derecha): Calculado como up × forward (producto cruz)
 * 
 * 2. ÁNGULOS DE EULER (Yaw y Pitch):
 *    - Yaw (guiñada): Rotación horizontal (girar cabeza izq-der)
 *    - Pitch (cabeceo): Rotación vertical (mirar arriba-abajo)
 *    - Roll (alabeo): NO usado para evitar mareo en FPS
 * 
 * 3. COORDENADAS ESFÉRICAS:
 *    - Forward se calcula desde (yaw, pitch) usando trigonometría esférica
 *    - fx = sin(yaw) * cos(pitch)
 *    - fy = sin(pitch)
 *    - fz = cos(yaw) * cos(pitch)
 * 
 * 4. VIEW MATRIX:
 *    - La cámara define la transformación de World Space → Camera Space
 *    - Todos los vértices se transforman relativos a la cámara antes de proyectar
 * 
 * 5. PROYECCIÓN:
 *    - Perspectiva: Objetos lejanos se ven más pequeños (FOV controla ángulo)
 *    - Ortográfica: Distancia no afecta tamaño (proyección paralela)
 * 
 * ============================================================================================
 */
public class Camera {
    // POSICIÓN: Coordenadas del ojo de la cámara en el mundo
    private Vector3 posicion;
    
    // FORWARD: Vector unitario que apunta hacia donde mira la cámara
    // Se recalcula desde yaw/pitch usando coordenadas esféricas
    private Vector3 forward;
    
    // UP: Vector unitario que define "arriba" para la cámara
    // En este proyecto siempre es (0, 1, 0) = eje Y mundial
    private Vector3 up;
    
    // FOV (Field of View): Distancia focal para proyección perspectiva
    // Valores más altos = campo de visión más amplio (más "zoom out")
    // En este proyecto: 500 unidades
    private double fov;
    
    // ORTHOGRAPHIC: Modo de proyección (false = perspectiva, true = paralela)
    private boolean orthographic = false;
    
    // YAW: Ángulo de rotación alrededor del eje Y (horizontal)
    // 0 = mirando hacia +Z, π/2 = mirando hacia +X
    private double yaw;
    
    // PITCH: Ángulo de rotación alrededor del eje X (vertical)
    // 0 = mirando horizontal, π/2 = mirando hacia arriba
    // Limitado a ±89° para evitar gimbal lock
    private double pitch;

    /**
     * Constructor: Inicializa la cámara en una posición con FOV especificado.
     * 
     * ESTADO INICIAL:
     * - Forward = (0, 0, 1) = mirando hacia +Z
     * - Up = (0, 1, 0) = +Y es arriba
     * - Yaw = 0, Pitch = 0 = mirando horizontal hacia +Z
     * 
     * @param posicion Posición inicial de la cámara en el mundo
     * @param fov Campo de visión (distancia focal para perspectiva)
     */
    public Camera(Vector3 posicion, double fov){
        this.posicion = posicion;
        this.fov = fov;
        this.forward = new Vector3(0,0,1); // Mirando hacia +Z por defecto
        this.up = new Vector3(0,1,0);      // +Y es arriba (world up)
        this.yaw = 0.0;
        this.pitch = 0.0;
    }

    // ========================================================================================
    // GETTERS Y SETTERS
    // ========================================================================================
    
    public Vector3 getPosicion() { return posicion; }
    public void setPosicion(Vector3 p) { this.posicion = p; }
    public Vector3 getForward() { return forward; }
    public Vector3 getUp() { return up; }
    public double getFov() { return fov; }

    public boolean isOrthographic() { return orthographic; }
    public void setOrthographic(boolean o) { this.orthographic = o; }

    /**
     * ========================================================================================
     * getRight - Calcula el vector "derecha" de la cámara
     * ========================================================================================
     * 
     * CÁLCULO:
     * Right = Up × Forward (producto cruz)
     * 
     * EXPLICACIÓN:
     * - En sistema derecho (right-handed): Thumb=X, Index=Y, Middle=Z
     * - Up × Forward da un vector perpendicular a ambos apuntando a la derecha
     * - Se normaliza para obtener vector unitario (longitud 1)
     * 
     * USO:
     * - Movimiento lateral (strafe) en controles FPS
     * - Construcción de la matriz de vista (view matrix)
     * 
     * @return Vector unitario apuntando a la derecha de la cámara
     */
    public Vector3 getRight() {
        // up.cross(forward) produce el vector derecho en sistema derecho
        // Se normaliza porque up y forward son unitarios pero el cross product
        // podría no serlo si no son exactamente perpendiculares
        return up.cross(forward).normalize();
    }

    /**
     * ========================================================================================
     * rotate - Rota la cámara incrementalmente (controles de mouse)
     * ========================================================================================
     * 
     * ÁNGULOS DE EULER:
     * - deltaYaw: Cuánto girar horizontalmente (radianes)
     * - deltaPitch: Cuánto girar verticalmente (radianes)
     * 
     * CLAMPING DE PITCH:
     * - Limitado a ±89° para evitar gimbal lock
     * - Gimbal lock ocurre cuando pitch = ±90° (mirando directo arriba/abajo)
     * - Causa: up y forward se vuelven paralelos, right se indefine
     * 
     * CONVERSIÓN ESFÉRICA → CARTESIANA:
     * - Forward se recalcula desde (yaw, pitch) usando trigonometría
     * - fx = sin(yaw) * cos(pitch)  [componente X]
     * - fy = sin(pitch)              [componente Y]
     * - fz = cos(yaw) * cos(pitch)  [componente Z]
     * 
     * COORDENADAS ESFÉRICAS:
     * - Yaw es el ángulo azimutal (rotación en plano XZ)
     * - Pitch es el ángulo polar (elevación desde el plano XZ)
     * 
     * @param deltaYaw Incremento de rotación horizontal (radianes)
     * @param deltaPitch Incremento de rotación vertical (radianes)
     */
    public void rotate(double deltaYaw, double deltaPitch){
        // Acumular incrementos a los ángulos actuales
        yaw += deltaYaw;
        pitch += deltaPitch;
        
        // CLAMPING: Limitar pitch a ±89° para evitar gimbal lock
        double limit = Math.toRadians(89.0);
        if(pitch > limit) pitch = limit;
        if(pitch < -limit) pitch = -limit;

        // CONVERSIÓN DE COORDENADAS ESFÉRICAS A CARTESIANAS
        // Fórmulas de transformación esférica estándar
        double cosPitch = Math.cos(pitch);
        double fx = Math.sin(yaw) * cosPitch;
        double fy = Math.sin(pitch);
        double fz = Math.cos(yaw) * cosPitch;

        // Actualizar forward con el nuevo vector normalizado
        this.forward = new Vector3(fx, fy, fz).normalize();
        
        // Up siempre es world-up (0, 1, 0) para simplicidad
        // Esto previene roll (alabeo) que causa mareo en juegos FPS
        this.up = new Vector3(0,1,0);
    }

    public double getYaw(){ return yaw; }
    public double getPitch(){ return pitch; }

    /**
     * ========================================================================================
     * setOrientation - Establece orientación absoluta (no incremental)
     * ========================================================================================
     * 
     * DIFERENCIA CON rotate():
     * - rotate() suma deltas (incremental, para mouse look)
     * - setOrientation() establece valores absolutos (para free-look o reset)
     * 
     * USO:
     * - Free-look mode (cuando mouse no está bloqueado)
     * - Teleportar cámara con orientación específica
     * - Reset de cámara a orientación inicial
     * 
     * @param newYaw Ángulo horizontal absoluto (radianes)
     * @param newPitch Ángulo vertical absoluto (radianes, será clamped)
     */
    public void setOrientation(double newYaw, double newPitch){
        this.yaw = newYaw;
        
        // Clamp pitch a ±89°
        double limit = Math.toRadians(89.0);
        if(newPitch > limit) newPitch = limit;
        if(newPitch < -limit) newPitch = -limit;
        this.pitch = newPitch;

        // Recalcular forward desde yaw/pitch (igual que en rotate)
        double cosPitch = Math.cos(pitch);
        double fx = Math.sin(yaw) * cosPitch;
        double fy = Math.sin(pitch);
        double fz = Math.cos(yaw) * cosPitch;

        this.forward = new Vector3(fx, fy, fz).normalize();
        this.up = new Vector3(0,1,0);
    }

    /**
     * MOVIMIENTO HACIA ADELANTE: Desplaza la cámara en dirección forward.
     * @param amount Distancia a mover (puede ser negativa para retroceder)
     */
    public void moveForward(double amount){
        posicion = posicion.add(forward.scale(amount));
    }

    /**
     * MOVIMIENTO HACIA ATRÁS: Desplaza la cámara opuesto a forward.
     * @param amount Distancia a mover
     */
    public void moveBackward(double amount){
        posicion = posicion.subtract(forward.scale(amount));
    }

    /**
     * MOVIMIENTO HACIA LA DERECHA: Strafe derecho (sin rotar).
     * @param amount Distancia a mover
     */
    public void moveRight(double amount){
        posicion = posicion.add(getRight().scale(amount));
    }

    /**
     * MOVIMIENTO HACIA LA IZQUIERDA: Strafe izquierdo (sin rotar).
     * @param amount Distancia a mover
     */
    public void moveLeft(double amount){
        posicion = posicion.subtract(getRight().scale(amount));
    }
}
