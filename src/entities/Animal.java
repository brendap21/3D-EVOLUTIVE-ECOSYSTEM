package entities;

import main.Renderable;
import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * Animal compuesto por múltiples voxels (cubos).
 * Generado procedimentalmente a partir de una seed para permitir determinismo
 * y reproducción de poblaciones. Implementa mutación simple.
 */
public class Animal implements Renderable { // Clase pública que implementa interfaz Renderable
    // === ATRIBUTOS PRINCIPALES ===
    private Vector3 posicion; // Posición del animal en el mundo 3D (x, y, z)
    private List<Vector3> voxels; // Lista de offsets de voxels relativos al centro del animal
    private int voxelSize; // Tamaño en píxeles de cada cubo voxel
    private Color color; // Color del animal (RGB)
    private double rotY = 0.0; // Rotación en eje Y (actualmente no usada)
    private double bob = 0.0; // Variable para animación de bobbing (actualmente no usada)
    private double speed = 1.0; // Velocidad de animación del animal
    private long seed; // Semilla para generación procedural determinista
    
    private boolean isSpawning = true;
    private double spawnProgress = 0.0;
    private static final double SPAWN_DURATION = 3.0;
    private List<SpawnParticle> spawnParticles = new ArrayList<>();
    
    // Clase interna estática para representar partículas de spawn
    private static class SpawnParticle {
        double angle; // Ángulo de rotación en espiral (radianes)
        double height; // Altura inicial de la partícula
        double radius; // Radio de la espiral
        double speed; // Velocidad de rotación de la partícula
        int voxelIndex; // Índice del voxel objetivo al que converge
        
        SpawnParticle(double angle, double height, double radius, double speed, int voxelIndex) {
            this.angle = angle;
            this.height = height;
            this.radius = radius;
            this.speed = speed;
            this.voxelIndex = voxelIndex;
        }
    }

    // Constructor principal que genera animal desde seed
    public Animal(Vector3 posicion, long seed){
        this.posicion = posicion; // Asigna posición inicial del animal
        this.seed = seed; // Guarda seed para reproducibilidad
        this.voxels = new ArrayList<>(); // Inicializa lista vacía de voxels
        generateFromSeed(seed); // Genera geometría del animal basada en seed
        initializeSpawnAnimation(); // Prepara animación de aparición
    }

    public Animal(Vector3 posicion, long seed, Color color, int voxelSize, List<Vector3> voxels){
        this.posicion = posicion;
        this.seed = seed;
        this.color = color;
        this.voxelSize = voxelSize;
        this.voxels = new ArrayList<>(voxels);
        this.isSpawning = false;
        this.spawnProgress = 1.0;
    }

    // Genera geometría procedural del animal basada en seed
    private void generateFromSeed(long seed){
        Random r = new Random(seed); // Crea generador aleatorio con seed específica
        this.voxelSize = 18 + r.nextInt(16); // Tamaño aleatorio entre 18 y 33 píxeles
        
        // === GENERACIÓN DE COLOR ===
        int rcol = 80 + r.nextInt(176); // Componente rojo: 80-255 (evita colores muy oscuros)
        int gcol = 80 + r.nextInt(176); // Componente verde: 80-255
        int bcol = 80 + r.nextInt(176); // Componente azul: 80-255
        this.color = new Color(rcol, gcol, bcol); // Crea color con componentes RGB
        this.speed = 0.5 + r.nextDouble()*2.0; // Velocidad aleatoria entre 0.5 y 2.5

        // === GENERACIÓN DE FORMA ===
        // Crea una huella 2D (footprint) y la puebla con voxels conectados
        int w = 1 + r.nextInt(3); // Ancho de la grilla: 1-3
        int h = 1 + r.nextInt(3); // Alto de la grilla: 1-3
        boolean[][] grid = new boolean[w][h]; // Grilla booleana para marcar posiciones ocupadas
        int attempts = 0; // Contador de intentos para colocar voxels
        
        // Semilla inicial en el centro
        grid[w/2][h/2] = true; // Marca centro como ocupado
        int placed = 1; // Contador de voxels colocados (empieza en 1)
        int target = 3 + r.nextInt(6); // Objetivo: 3-8 voxels total
        
        // Loop para colocar voxels asegurando conectividad
        while(placed < target && attempts < 50){ // Continúa hasta alcanzar target o 50 intentos
            int x = r.nextInt(w); // Posición X aleatoria en grilla
            int y = r.nextInt(h); // Posición Y aleatoria en grilla
            
            if(grid[x][y]) { attempts++; continue; } // Si ya está ocupada, incrementa intentos y continúa
            
            // Verifica si tiene vecino adyacente (asegura conectividad)
            boolean neigh = false; // Flag para indicar si hay vecino
            if(x>0 && grid[x-1][y]) neigh = true; // Vecino izquierdo
            if(x<w-1 && grid[x+1][y]) neigh = true; // Vecino derecho
            if(y>0 && grid[x][y-1]) neigh = true; // Vecino arriba
            if(y<h-1 && grid[x][y+1]) neigh = true; // Vecino abajo
            
            if(neigh){ // Si tiene vecino
                grid[x][y] = true; // Marca posición como ocupada
                placed++; // Incrementa contador de voxels colocados
            }
            attempts++; // Incrementa intentos
        }

        // === CONVERSIÓN DE GRILLA A VOXELS 3D ===
        for(int x=0;x<w;x++){ // Itera sobre ancho de grilla
            for(int y=0;y<h;y++){ // Itera sobre alto de grilla
                if(grid[x][y]){ // Si la posición está ocupada
                    double ox = (x - w/2.0); // Offset X centrado (0 es centro)
                    double oy = 0; // Altura Y = 0 (todos en mismo nivel base)
                    double oz = (y - h/2.0); // Offset Z centrado (0 es centro)
                    voxels.add(new Vector3(ox, oy, oz)); // Agrega voxel a lista con offsets locales
                }
            }
        }

        // === AGREGAR VOXELS ELEVADOS (PATAS/CUELLO) ===
        if(r.nextDouble() < 0.6){ // 60% de probabilidad de tener voxels elevados
            if(!voxels.isEmpty()){ // Si hay al menos un voxel
                int idx = r.nextInt(voxels.size()); // Selecciona voxel base aleatorio
                Vector3 base = voxels.get(idx); // Obtiene voxel base
                voxels.add(new Vector3(base.x, 1.0, base.z)); // Agrega voxel elevado (Y=1) sobre la base
            }
        }
    }
    
    // Inicializa partículas para animación de spawn
    private void initializeSpawnAnimation() {
        Random r = new Random(seed + 999); // Generador aleatorio con seed diferente (seed + offset)
        
        // Crear partículas en espiral para cada voxel
        for (int i = 0; i < voxels.size(); i++) { // Itera sobre todos los voxels
            int particlesPerVoxel = 5; // 5 partículas por cada voxel
            
            for (int p = 0; p < particlesPerVoxel; p++) { // Crea múltiples partículas por voxel
                double angle = r.nextDouble() * Math.PI * 2; // Ángulo aleatorio (0 a 2π radianes = 360°)
                double height = -50 - r.nextDouble() * 30; // Altura inicial muy baja (-50 a -80)
                double radius = 20 + r.nextDouble() * 25; // Radio de espiral (20 a 45 unidades)
                double speed = 0.8 + r.nextDouble() * 0.4; // Velocidad de rotación (0.8 a 1.2)
                spawnParticles.add(new SpawnParticle(angle, height, radius, speed, i)); // Agrega partícula a lista
            }
        }
    }

    public void mutate(long newSeed){
        Random r = new Random(newSeed); // Generador aleatorio con nueva seed
        
        // === MUTACIÓN DE COLOR (RGB) ===
        int dr = r.nextInt(41)-20; // Delta rojo: -20 a +20
        int dg = r.nextInt(41)-20; // Delta verde: -20 a +20
        int db = r.nextInt(41)-20; // Delta azul: -20 a +20
        int nr = Math.max(10, Math.min(255, color.getRed() + dr)); // Nuevo rojo clampado [10, 255]
        int ng = Math.max(10, Math.min(255, color.getGreen() + dg)); // Nuevo verde clampado [10, 255]
        int nb = Math.max(10, Math.min(255, color.getBlue() + db)); // Nuevo azul clampado [10, 255]
        color = new Color(nr, ng, nb); // Crea nuevo color con valores mutados
        
        // === MUTACIÓN DE TAMAÑO (ESCALA) ===
        int ds = r.nextInt(7)-3; // Delta de tamaño: -3 a +3 píxeles
        voxelSize = Math.max(8, voxelSize + ds); // Nuevo tamaño clampado (mínimo 8px)
    }

    public math.Vector3 getAABBMin(){
        // Si no hay voxels, retorna posición del animal como punto
        if(voxels.isEmpty()) return new math.Vector3(posicion.x, posicion.y, posicion.z);
        
        // Inicializa valores mínimos con infinito positivo
        double minX = Double.POSITIVE_INFINITY; // Valor inicial para buscar mínimo X
        double minY = Double.POSITIVE_INFINITY; // Valor inicial para buscar mínimo Y
        double minZ = Double.POSITIVE_INFINITY; // Valor inicial para buscar mínimo Z
        
        for(Vector3 off : voxels){ // Itera sobre cada voxel del animal
            // === TRANSFORMACIÓN: Escala + Traslación ===
            // Convierte coordenadas locales del voxel a coordenadas mundiales
            double wx = posicion.x + off.x * voxelSize; // World X = Centro + (Offset * Escala)
            double wy = posicion.y + off.y * voxelSize; // World Y = Centro + (Offset * Escala)
            double wz = posicion.z + off.z * voxelSize; // World Z = Centro + (Offset * Escala)
            
            // === EXTENDER AABB ===
            // Cada voxel es un cubo, su radio es voxelSize/2
            minX = Math.min(minX, wx - voxelSize/2.0); // Actualiza mínimo X considerando radio
            minY = Math.min(minY, wy - voxelSize/2.0); // Actualiza mínimo Y considerando radio
            minZ = Math.min(minZ, wz - voxelSize/2.0); // Actualiza mínimo Z considerando radio
        }
        return new math.Vector3(minX, minY, minZ); // Retorna esquina mínima del AABB
    }

    /**
     * ========================================================================================
     * getAABBMax - Calcula esquina máxima del Axis-Aligned Bounding Box
     * ========================================================================================
     * 
     * CONCEPTOS: Igual que getAABBMin() pero calcula esquina opuesta (máxima)
     * 
     * @return Esquina máxima (x, y, z) del bounding box en world space
     */
    public math.Vector3 getAABBMax(){
        // Si no hay voxels, retorna posición del animal como punto
        if(voxels.isEmpty()) return new math.Vector3(posicion.x, posicion.y, posicion.z);
        
        // Inicializa valores máximos con infinito negativo
        double maxX = Double.NEGATIVE_INFINITY; // Valor inicial para buscar máximo X
        double maxY = Double.NEGATIVE_INFINITY; // Valor inicial para buscar máximo Y
        double maxZ = Double.NEGATIVE_INFINITY; // Valor inicial para buscar máximo Z
        
        for(Vector3 off : voxels){ // Itera sobre cada voxel del animal
            // === TRANSFORMACIÓN: Escala + Traslación ===
            double wx = posicion.x + off.x * voxelSize; // World X
            double wy = posicion.y + off.y * voxelSize; // World Y
            double wz = posicion.z + off.z * voxelSize; // World Z
            
            // === EXTENDER AABB ===
            maxX = Math.max(maxX, wx + voxelSize/2.0); // Actualiza máximo X considerando radio
            maxY = Math.max(maxY, wy + voxelSize/2.0); // Actualiza máximo Y considerando radio
            maxZ = Math.max(maxZ, wz + voxelSize/2.0); // Actualiza máximo Z considerando radio
        }
        return new math.Vector3(maxX, maxY, maxZ); // Retorna esquina máxima del AABB
    }

    // Método update llamado cada frame (~60 FPS)
    @Override
    public void update(){
        if (isSpawning) { // Si el animal está en proceso de spawning
            spawnProgress += 0.016 / SPAWN_DURATION; // Incrementa progreso (0.016s ≈ 1 frame a 60fps)
            if (spawnProgress >= 1.0) { // Si la animación terminó (100%)
                spawnProgress = 1.0; // Clampea a 1.0
                isSpawning = false; // Desactiva flag de spawning
                spawnParticles.clear(); // Libera memoria de partículas (ya no se necesitan)
            }
        }
    }

    // Método render llamado cada frame para dibujar el animal
    @Override
    public void render(SoftwareRenderer renderer, Camera cam){
        if (isSpawning) { // Si está spawneando
            renderSpawnAnimation(renderer, cam); // Dibuja animación de aparición
        } else { // Si ya spawneó completamente
            // === RENDER NORMAL ===
            for(Vector3 off : voxels){ // Itera sobre cada voxel del animal
                // Calcula posición mundial del voxel (Traslación + Escala)
                Vector3 world = new Vector3(
                    posicion.x + off.x * voxelSize, // X mundial
                    posicion.y + off.y * voxelSize, // Y mundial
                    posicion.z + off.z * voxelSize  // Z mundial
                );
                // Obtiene vértices del cubo en posición mundial
                Vector3[] verts = renderer.getCubeVertices(world, voxelSize, 0); // Rotación = 0
                // Dibuja cubo con color del animal
                renderer.drawCube(verts, cam, color);
            }
        }
    }
    
    // Renderiza la animación de spawn en 3 fases
    private void renderSpawnAnimation(SoftwareRenderer renderer, Camera cam) {
        // === FASE 1 (0.0 - 0.5): Partículas en espiral convergiendo ===
        if (spawnProgress < 0.5) { // Primera mitad de la animación (0% a 50%)
            double particlePhase = spawnProgress / 0.5; // Normaliza a rango [0, 1] para esta fase
            
            for (SpawnParticle p : spawnParticles) { // Itera sobre todas las partículas
                // Calcula posición objetivo (donde terminará la partícula)
                double targetY = voxels.get(p.voxelIndex).y * voxelSize + posicion.y; // Y del voxel objetivo
                
                // Interpola radio (de ancho a 0 mientras converge)
                double currentRadius = p.radius * (1.0 - particlePhase); // Radio decrece linealmente
                
                // Interpola altura (de abajo hacia voxel objetivo)
                double currentHeight = p.height + (targetY - p.height) * particlePhase; // Interpolación lineal
                
                // Calcula ángulo de espiral (gira mientras sube)
                double spiralAngle = p.angle + particlePhase * Math.PI * 6 * p.speed; // 3 vueltas (6π rad)
                
                // === POSICIÓN DE PARTÍCULA EN ESPIRAL ===
                double px = posicion.x + Math.cos(spiralAngle) * currentRadius; // X en círculo
                double py = currentHeight; // Y interpolada
                double pz = posicion.z + Math.sin(spiralAngle) * currentRadius; // Z en círculo
                
                // Tamaño de partícula crece con progreso (6 a 10 píxeles)
                int particleSize = 6 + (int)(4 * particlePhase);
                
                // Obtiene vértices de cubo para partícula
                Vector3[] particleVerts = renderer.getCubeVertices(new Vector3(px, py, pz), particleSize, 0);
                
                // Color brillante amarillo-blanco
                Color particleColor = new Color(255, 255, 150); // RGB amarillo brillante
                renderer.drawCube(particleVerts, cam, particleColor); // Dibuja partícula
            }
        }
        
        // === FASE 2 (0.4 - 1.0): Voxels aparecen con rotación y escala ===
        if (spawnProgress > 0.4) { // Última parte de la animación (40% a 100%)
            double voxelPhase = Math.min(1.0, (spawnProgress - 0.4) / 0.6); // Normaliza a [0, 1]
            
            for (int i = 0; i < voxels.size(); i++) { // Itera sobre cada voxel
                Vector3 off = voxels.get(i); // Obtiene offset del voxel
                
                // Cada voxel tiene un retraso secuencial (aparecen uno tras otro)
                double voxelDelay = (double)i / voxels.size() * 0.3; // Retraso proporcional al índice
                
                // Calcula progreso individual de este voxel con retraso
                double individualProgress = Math.max(0, Math.min(1.0, (voxelPhase - voxelDelay) / 0.7));
                
                if (individualProgress > 0) { // Si este voxel ya empezó a aparecer
                    // === ESCALA CON EFECTO BOUNCE ===
                    double scale = individualProgress < 0.8  // Si está en primera parte (0% a 80%)
                        ? individualProgress / 0.8  // Crece linealmente de 0 a 1
                        : 1.0 + Math.sin((individualProgress - 0.8) / 0.2 * Math.PI) * 0.3; // Bounce final (overshoot)
                    
                    // === ROTACIÓN DURANTE APARICIÓN ===
                    double spinRotation = (1.0 - individualProgress) * Math.PI * 4; // 2 vueltas completas (8π/2)
                    
                    // === POSICIÓN MUNDIAL CON OFFSET VERTICAL ===
                    Vector3 world = new Vector3(
                        posicion.x + off.x * voxelSize, // X mundial
                        posicion.y + off.y * voxelSize + (1.0 - individualProgress) * 15, // Y + offset descendente
                        posicion.z + off.z * voxelSize  // Z mundial
                    );
                    
                    // Calcula tamaño escalado (mínimo 1 píxel)
                    int scaledSize = Math.max(1, (int)(voxelSize * scale));
                    
                    // Obtiene vértices con rotación de spin
                    Vector3[] verts = renderer.getCubeVertices(world, scaledSize, spinRotation);
                    
                    // === COLOR CON DESTELLO (GLOW) ===
                    float glow = (float)(Math.max(0, 1.5 - individualProgress * 1.5)); // Glow decrece de 1.5 a 0
                    
                    // Crea color con componente de brillo adicional
                    Color voxelColor = new Color(
                        Math.min(1.0f, color.getRed() / 255f + glow * 0.5f),   // R + glow
                        Math.min(1.0f, color.getGreen() / 255f + glow * 0.5f), // G + glow
                        Math.min(1.0f, color.getBlue() / 255f + glow * 0.5f)   // B + glow
                    );
                    renderer.drawCube(verts, cam, voxelColor); // Dibuja voxel con glow
                }
            }
            
            // === FASE 3 (0.85 - 1.0): Destello final expansivo ===
            if (spawnProgress > 0.85) { // Últimos 15% de la animación
                double burstPhase = (spawnProgress - 0.85) / 0.15; // Normaliza a [0, 1]
                int burstParticles = 16; // 16 partículas de explosión
                
                for (int i = 0; i < burstParticles; i++) { // Itera sobre partículas de burst
                    // Distribuye partículas uniformemente en círculo
                    double angle = (i / (double)burstParticles) * Math.PI * 2; // Ángulo uniforme
                    
                    double burstRadius = burstPhase * 30; // Radio crece hasta 30 unidades
                    double burstHeight = posicion.y + voxelSize * 2 + burstPhase * 15; // Sube 15 unidades
                    
                    // === POSICIÓN DE PARTÍCULA DE BURST ===
                    double bx = posicion.x + Math.cos(angle) * burstRadius; // X radial
                    double by = burstHeight; // Y ascendente
                    double bz = posicion.z + Math.sin(angle) * burstRadius; // Z radial
                    
                    // Tamaño decrece mientras se expande (de 6 a 0)
                    int burstSize = Math.max(1, (int)(6 * (1.0 - burstPhase)));
                    
                    // Obtiene vértices para partícula de burst
                    Vector3[] burstVerts = renderer.getCubeVertices(new Vector3(bx, by, bz), burstSize, 0);
                    
                    Color burstColor = new Color(255, 255, 100); // Amarillo brillante
                    renderer.drawCube(burstVerts, cam, burstColor); // Dibuja partícula de burst
                }
            }
        }
    }

    public Vector3 getPosicion() { return posicion; }
    public void setPosicion(Vector3 pos) { this.posicion = pos; }
}
