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
 * Depredador: Animal especial que puede cazar a otros animales.
 * No evoluciona, tamaño fijo, diseño de monstruo terrorífico.
 */
public class Depredador implements Renderable, Collidable {
    protected Vector3 posicion;
    protected List<Vector3> voxels;
    protected int voxelSize;
    protected Color color;
    protected long seed;
    
    // Movement
    protected Vector3 velocity = new Vector3(0, 0, 0);
    protected double baseSpeed = 1.5; // Un poco más rápido que animales normales
    protected double wanderTimer = 0.0;
    protected static final double WANDER_CHANGE_INTERVAL = 3.0;
    protected static final double WORLD_BOUND = 320.0;
    protected static simulation.Mundo worldRef = null;
    protected double yaw = 0.0;
    protected boolean movementInitialized = false;
    
    // Selection and hover states
    private boolean isHovered = false;
    private boolean isSelected = false;
    private double hoverGlow = 0.0;
    private double selectionScale = 1.0;
    private static int nextDepredadorId = 10000; // IDs separados de animales normales
    private int depredadorId;
    
    // Tracking for UI
    private long spawnTime = 0L;
    
    // Spawn animation
    private boolean isSpawning = true;
    private double spawnProgress = 0.0;
    private static final double SPAWN_DURATION = 0.8;
    private List<SpawnParticle> spawnParticles = new ArrayList<>();
    
    // Animation - MEJORADO
    private double animationTime = 0.0;
    private double breathePulse = 0.0;
    private double legSwing = 0.0;      // Para animación de patas
    private double blinkTimer = 0.0;    // Para parpadeo
    private boolean eyesOpen = true;    // Estado de los ojos
    private double attackAnimation = 0.0; // Animación de ataque
    private boolean isAttacking = false;
    private double bodyBounce = 0.0;    // Rebote del cuerpo al caminar
    private double tailSwing = 0.0;     // Movimiento de cola
    private double hornSwing = 0.0;     // Movimiento de cuernos/orejas
    private double eyePulse = 0.0;      // Pulso de brillo de ojos
    private double attackFlash = 0.0;   // Destello breve al atacar
    
    // Movement control
    private boolean isPaused = false;   // Pausar movimiento cuando está seleccionado
    
    // Death flag
    private boolean markedForDeath = false;
    private boolean isDying = false;
    private double deathProgress = 0.0;
    private static final double DEATH_DURATION = 0.8;
    private List<DeathParticle> deathParticles = new ArrayList<>();
    
    private static class DeathParticle {
        Vector3 position;
        Vector3 velocity;
        Color color;
        double life;
        
        DeathParticle(Vector3 pos, Vector3 vel, Color col) {
            this.position = pos.copy();
            this.velocity = vel.copy();
            this.color = col;
            this.life = 1.0;
        }
    }
    
    private static class SpawnParticle {
        double angle;
        double height;
        double radius;
        double speed;
        int voxelIndex;
        
        SpawnParticle(double angle, double height, double radius, double speed, int voxelIndex) {
            this.angle = angle;
            this.height = height;
            this.radius = radius;
            this.speed = speed;
            this.voxelIndex = voxelIndex;
        }
    }
    
    public Depredador(Vector3 pos, long seed) {
        this.posicion = pos.copy();
        this.seed = seed;
        this.voxelSize = 3; // Reducido para ser más pequeño
        this.voxels = new ArrayList<>();
        
        // Colores de monstruo variados
        Random r = new Random(seed);
        int colorType = r.nextInt(3);
        if (colorType == 0) {
            // Verde tóxico
            this.color = new Color(50 + r.nextInt(30), 200 + r.nextInt(56), 50 + r.nextInt(30));
        } else if (colorType == 1) {
            // Morado oscuro
            this.color = new Color(120 + r.nextInt(60), 30 + r.nextInt(40), 150 + r.nextInt(60));
        } else {
            // Rojo sangre
            this.color = new Color(200 + r.nextInt(56), 30 + r.nextInt(40), 30 + r.nextInt(40));
        }
        
        generarFormaDepredador();
        initializeSpawnAnimation();
        initializeMovement();
    }
    
    private void generarFormaDepredador() {
        Random r = new Random(seed);
        voxels.clear();
        
        // CUERPO PRINCIPAL - más musculoso y proporcional
        // Torso bajo (segmento 0-2)
        for (int y = 0; y < 3; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -1; z <= 3; z++) {
                    int dist = Math.abs(x) + Math.abs(z - 1);
                    if (dist <= 4) {
                        voxels.add(new Vector3(x, y, z));
                    }
                }
            }
        }
        
        // Torso superior (segmento 3-4) - más estrecho
        for (int y = 3; y < 5; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -1; z <= 2; z++) {
                    int dist = Math.abs(x) + Math.abs(z);
                    if (dist <= 3) {
                        voxels.add(new Vector3(x, y, z));
                    }
                }
            }
        }
        
        // CUELLO (conecta cuerpo con cabeza)
        for (int x = -1; x <= 1; x++) {
            for (int z = -2; z <= 0; z++) {
                voxels.add(new Vector3(x, 5, z));
            }
        }
        
        // CABEZA GRANDE Y AMENAZANTE (segmento 6-9) - adelante en Z negativo
        int headY = 6;
        // Parte trasera de la cabeza
        for (int x = -3; x <= 3; x++) {
            for (int z = -2; z <= 0; z++) {
                if (Math.abs(x) <= 3 && Math.abs(z) <= 2) {
                    voxels.add(new Vector3(x, headY, z));
                    voxels.add(new Vector3(x, headY + 1, z));
                }
            }
        }
        
        // Hocico/Mandíbula superior (proyección frontal)
        for (int x = -2; x <= 2; x++) {
            for (int z = -5; z <= -3; z++) {
                if (Math.abs(x) <= 2) {
                    voxels.add(new Vector3(x, headY, z));
                    if (Math.abs(x) <= 1 && z >= -4) {
                        voxels.add(new Vector3(x, headY + 1, z));
                    }
                }
            }
        }
        
        // Mandíbula inferior (debajo del hocico)
        for (int x = -2; x <= 2; x++) {
            for (int z = -5; z <= -3; z++) {
                if (Math.abs(x) <= 1) {
                    voxels.add(new Vector3(x, headY - 1, z));
                }
            }
        }
        
        // DIENTES/COLMILLOS (sobresaliendo de la mandíbula)
        voxels.add(new Vector3(-2, headY - 2, -4));
        voxels.add(new Vector3(2, headY - 2, -4));
        voxels.add(new Vector3(-1, headY - 2, -5));
        voxels.add(new Vector3(1, headY - 2, -5));
        
        // OJOS GRANDES Y BRILLANTES (3x2 cada uno para máxima visibilidad)
        int eyeY = headY + 1;
        // Ojo izquierdo
        for (int ex = -3; ex <= -1; ex++) {
            for (int ey = 0; ey <= 1; ey++) {
                voxels.add(new Vector3(ex, eyeY + ey, -2));
            }
        }
        // Ojo derecho
        for (int ex = 1; ex <= 3; ex++) {
            for (int ey = 0; ey <= 1; ey++) {
                voxels.add(new Vector3(ex, eyeY + ey, -2));
            }
        }
        
        // CUERNOS/OREJAS DEMONÍACOS (largos y curvados)
        // Izquierdo
        voxels.add(new Vector3(-3, headY + 2, -1));
        voxels.add(new Vector3(-4, headY + 3, -1));
        voxels.add(new Vector3(-4, headY + 4, 0));
        voxels.add(new Vector3(-4, headY + 5, 0));
        // Derecho
        voxels.add(new Vector3(3, headY + 2, -1));
        voxels.add(new Vector3(4, headY + 3, -1));
        voxels.add(new Vector3(4, headY + 4, 0));
        voxels.add(new Vector3(4, headY + 5, 0));
        
        // PÚAS EN LA ESPALDA (espina dorsal)
        for (int i = 0; i < 4; i++) {
            voxels.add(new Vector3(0, 3 + i, 2));
            voxels.add(new Vector3(0, 4 + i, 2));
        }
        
        // PATAS TRASERAS (más gruesas y musculosas)
        // Izquierda trasera
        for (int y = -1; y >= -4; y--) {
            for (int x = -4; x <= -2; x++) {
                for (int z = 2; z <= 3; z++) {
                    voxels.add(new Vector3(x, y, z));
                }
            }
        }
        // Derecha trasera
        for (int y = -1; y >= -4; y--) {
            for (int x = 2; x <= 4; x++) {
                for (int z = 2; z <= 3; z++) {
                    voxels.add(new Vector3(x, y, z));
                }
            }
        }
        
        // PATAS DELANTERAS (con garras)
        // Izquierda delantera
        for (int y = -1; y >= -4; y--) {
            for (int x = -4; x <= -2; x++) {
                for (int z = -2; z <= -1; z++) {
                    voxels.add(new Vector3(x, y, z));
                }
            }
        }
        // Garras izquierdas
        voxels.add(new Vector3(-5, -4, -2));
        voxels.add(new Vector3(-5, -4, -1));
        voxels.add(new Vector3(-5, -5, -2));
        
        // Derecha delantera
        for (int y = -1; y >= -4; y--) {
            for (int x = 2; x <= 4; x++) {
                for (int z = -2; z <= -1; z++) {
                    voxels.add(new Vector3(x, y, z));
                }
            }
        }
        // Garras derechas
        voxels.add(new Vector3(5, -4, -2));
        voxels.add(new Vector3(5, -4, -1));
        voxels.add(new Vector3(5, -5, -2));
        
        // COLA LARGA CON PÚAS (más dramática)
        for (int i = 0; i < 6; i++) {
            int tailZ = 4 + i;
            int tailY = 2 - (i / 2);
            // Cuerpo de la cola (más grueso en la base)
            int thickness = Math.max(0, 2 - i/2);
            for (int x = -thickness; x <= thickness; x++) {
                voxels.add(new Vector3(x, tailY, tailZ));
                if (thickness > 0) {
                    voxels.add(new Vector3(x, tailY + 1, tailZ));
                }
            }
            // Púas en la cola (cada 2 segmentos)
            if (i % 2 == 0 && i < 4) {
                voxels.add(new Vector3(0, tailY + 2, tailZ));
                voxels.add(new Vector3(0, tailY + 3, tailZ));
            }
        }
    }
    
    protected void initializeSpawnAnimation() {
        this.depredadorId = nextDepredadorId++;
        this.spawnTime = System.currentTimeMillis();
        Random r = new Random(seed + 999);
        for (int i = 0; i < voxels.size(); i++) {
            int particlesPerVoxel = 3;
            for (int p = 0; p < particlesPerVoxel; p++) {
                double angle = r.nextDouble() * Math.PI * 2;
                double height = -15 - r.nextDouble() * 10;
                double radius = 5 + r.nextDouble() * 8;
                double speed = 0.8 + r.nextDouble() * 0.4;
                spawnParticles.add(new SpawnParticle(angle, height, radius, speed, i));
            }
        }
    }
    
    private void initializeMovement() {
        if (movementInitialized) return;
        Random r = new Random(seed + System.currentTimeMillis());
        yaw = r.nextDouble() * Math.PI * 2;
        // Inicializar velocidad correctamente hacia adelante
        velocity.x = Math.sin(yaw) * baseSpeed;
        velocity.z = -Math.cos(yaw) * baseSpeed;
        movementInitialized = true;
    }
    
    @Override
    public void update() {
        animationTime += 0.016;
        breathePulse = Math.sin(animationTime * 3.0) * 0.2; // Respiración visible
        
        // Animación de caminar EXAGERADA Y DRAMÁTICA
        if (!isPaused && !isDying) {
            // Patas con movimiento alternado muy pronunciado
            legSwing = Math.sin(animationTime * 12.0) * 1.8; // MUY rápido y amplio
            // Rebote corporal al caminar (como un depredador poderoso)
            bodyBounce = Math.abs(Math.sin(animationTime * 12.0)) * 1.2; // Gran rebote
            // Cola serpenteante y ondulante (movimiento exagerado)
            tailSwing = Math.sin(animationTime * 6.0) * 2.5; // Amplitud muy grande
            // Cuernos/orejas oscilando amenazantemente
            hornSwing = Math.sin(animationTime * 4.0) * 1.2; // Movimiento notable
            // Ojos con pulso intenso (efecto siniestro)
            eyePulse = 0.3 + 0.7 * (0.5 + 0.5 * Math.sin(animationTime * 7.0));
        } else {
            legSwing = 0;
            bodyBounce = 0;
            tailSwing = 0;
            hornSwing = 0;
            eyePulse = 0;
        }
        
        // Sistema de parpadeo más frecuente
        blinkTimer += 0.016;
        if (eyesOpen && blinkTimer > 2.0 + Math.random() * 1.5) {
            eyesOpen = false;
            blinkTimer = 0.0;
        } else if (!eyesOpen && blinkTimer > 0.15) {
            eyesOpen = true;
            blinkTimer = 0.0;
        }
        
        // Animación de ataque MUY DRAMÁTICA Y VISIBLE
        if (isAttacking) {
            attackAnimation += 0.016 / 1.2; // Duración de 1.2 segundos (MUCHO más largo)
            if (attackAnimation >= 1.0) {
                attackAnimation = 0.0;
                isAttacking = false;
            }
            // Mantener el flash durante toda la animación
            attackFlash = 1.0;
        }

        // Decaimiento del destello de ataque (más lento para que se note)
        if (attackFlash > 0.0 && !isAttacking) {
            attackFlash = Math.max(0.0, attackFlash - 0.03); // Muy lento
        }
        
        // Death animation
        if (isDying) {
            deathProgress += 0.016 / DEATH_DURATION;
            if (deathProgress >= 1.0) {
                deathProgress = 1.0;
            }
            
            // Update death particles
            for (DeathParticle p : deathParticles) {
                p.position.x += p.velocity.x;
                p.position.y += p.velocity.y;
                p.position.z += p.velocity.z;
                p.velocity.y -= 0.3; // Gravity
                p.life -= 0.016 / DEATH_DURATION;
            }
            return;
        }
        
        if (isSpawning) {
            spawnProgress += 0.016 / SPAWN_DURATION;
            if (spawnProgress >= 1.0) {
                spawnProgress = 1.0;
                isSpawning = false;
                spawnParticles.clear();
            }
            return;
        }
        
        // Update hover/selection animations
        if (isHovered) {
            hoverGlow = Math.min(1.0, hoverGlow + 0.1);
        } else {
            hoverGlow = Math.max(0.0, hoverGlow - 0.1);
        }
        
        if (isSelected) {
            selectionScale = 1.0 + Math.sin(animationTime * 4.0) * 0.05;
        } else {
            selectionScale = 1.0;
        }
        
        // Movement (solo si no está pausado)
        if (!isPaused) {
            updateMovement();
        }
        
        // Check for collisions with prey animals
        if (worldRef != null && !isPaused) {
            checkPreyCollisions();
        }
    }
    
    private void updateMovement() {
        wanderTimer += 0.016;
        
        if (wanderTimer >= WANDER_CHANGE_INTERVAL) {
            wanderTimer = 0.0;
            Random r = new Random(seed + System.currentTimeMillis());
            double targetYaw = yaw + (r.nextDouble() - 0.5) * Math.PI * 0.8;
            yaw = targetYaw;
            // Mover hacia adelante: Z negativo es adelante
            velocity.x = Math.sin(yaw) * baseSpeed;
            velocity.z = -Math.cos(yaw) * baseSpeed;
        }
        
        // Calculate next position
        double nextX = posicion.x + velocity.x;
        double nextZ = posicion.z + velocity.z;
        
        // Boundary check
        if (Math.abs(nextX) > WORLD_BOUND) {
            velocity.x = -velocity.x;
            yaw = Math.atan2(velocity.x, -velocity.z);
            nextX = posicion.x + velocity.x;
        }
        if (Math.abs(nextZ) > WORLD_BOUND) {
            velocity.z = -velocity.z;
            yaw = Math.atan2(velocity.x, -velocity.z);
            nextZ = posicion.z + velocity.z;
        }
        
        // Check collision with structures AND other depredadores
        boolean collisionDetected = false;
        if (worldRef != null) {
            double checkRadius = voxelSize * 4.0; // Radio de colisión
            java.util.List<Renderable> entities = worldRef.getEntities();
            for (Renderable entity : entities) {
                // Colisionar con rocas, árboles, flores, arbustos (NO con pasto)
                if (entity instanceof entities.Piedra || 
                    entity instanceof entities.Arbol || 
                    entity instanceof entities.Flor ||
                    entity instanceof entities.Arbusto) {
                    
                    entities.Collidable collidable = (entities.Collidable) entity;
                    // Usar AABB para obtener centro aproximado
                    Vector3 minBB = collidable.getAABBMin();
                    Vector3 maxBB = collidable.getAABBMax();
                    Vector3 entityPos = new Vector3(
                        (minBB.x + maxBB.x) * 0.5,
                        (minBB.y + maxBB.y) * 0.5,
                        (minBB.z + maxBB.z) * 0.5
                    );
                    
                    // Calcular distancia a la siguiente posición
                    double dx = nextX - entityPos.x;
                    double dz = nextZ - entityPos.z;
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    
                    // Obtener radio de la entidad
                    double entityRadius = 0;
                    if (entity instanceof entities.Piedra) {
                        entityRadius = 15.0;
                    } else if (entity instanceof entities.Arbol) {
                        entityRadius = 12.0;
                    } else if (entity instanceof entities.Flor) {
                        entityRadius = 8.0;
                    } else if (entity instanceof entities.Arbusto) {
                        entityRadius = 10.0;
                    }
                    
                    if (distance < checkRadius + entityRadius) {
                        collisionDetected = true;
                        break;
                    }
                }
                // Colisión con otros depredadores - NUEVO
                else if (entity instanceof entities.Depredador && entity != this) {
                    entities.Depredador otherDep = (entities.Depredador) entity;
                    Vector3 otherPos = otherDep.getPosition();
                    
                    double dx = nextX - otherPos.x;
                    double dz = nextZ - otherPos.z;
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    
                    double combinedRadius = checkRadius + otherDep.getCollisionRadius();
                    if (distance < combinedRadius) {
                        collisionDetected = true;
                        break;
                    }
                }
            }
        }
        
        // Si hay colisión, cambiar dirección
        if (collisionDetected) {
            Random r = new Random(seed + System.currentTimeMillis());
            yaw += Math.PI * 0.5 + r.nextDouble() * Math.PI;
            velocity.x = Math.sin(yaw) * baseSpeed;
            velocity.z = -Math.cos(yaw) * baseSpeed;
            wanderTimer = WANDER_CHANGE_INTERVAL; // Forzar nuevo cambio
            return; // No mover esta vez
        }
        
        posicion.x = nextX;
        posicion.z = nextZ;
        
        // Update Y position to match terrain
        if (worldRef != null) {
            double terrainHeight = worldRef.getHeightAt(posicion.x, posicion.z);
            double offset = voxelSize * 3.5;
            posicion.y = terrainHeight + offset;
        }
    }
    
    private void checkPreyCollisions() {
        List<Renderable> entities = worldRef.getEntities();
        for (Renderable entity : entities) {
            if (entity instanceof BaseAnimal) {
                BaseAnimal animal = (BaseAnimal) entity;
                // Check distance-based collision using AABB
                Vector3 animalPos = animal.getPosicion();
                double dx = posicion.x - animalPos.x;
                double dy = posicion.y - animalPos.y;
                double dz = posicion.z - animalPos.z;
                double distSq = dx*dx + dy*dy + dz*dz;
                
                // Use voxelSize as collision radius estimate
                double depRadius = voxelSize * 5.0;
                double animalRadius = 30.0; // Fixed estimate for animals
                double combinedRadius = depRadius + animalRadius;
                
                if (distSq < combinedRadius * combinedRadius) {
                    // Marcar animal para muerte
                    animal.markForDeath();
                    // Activar animación de ataque - NUEVO
                    isAttacking = true;
                    attackAnimation = 0.0;
                    attackFlash = 1.0; // Destello rojo al matar
                    System.out.println("[DEPREDADOR] ¡ATAQUE ACTIVADO! Matando animal ID: " + animal.getAnimalId());
                }
            }
        }
    }
    
    @Override
    public void render(SoftwareRenderer renderer, Camera camera) {
        if (isDying) {
            renderDeathAnimation(renderer, camera);
            return;
        }
        
        if (isSpawning) {
            renderSpawnAnimation(renderer, camera);
            return;
        }
        
        double scale = selectionScale * (1.0 + breathePulse);
        
        // Efecto de ataque MUY DRAMÁTICO Y VISIBLE: lunge exagerado, sacudida de mandíbula
        double attackPush = 0;
        double attackJaw = 0;
        double attackShake = 0;
        double attackLift = 0;
        if (isAttacking) {
            // Onda de ataque en 4 fases: preparación -> lunge -> mordida -> retroceso
            double t = attackAnimation;
            if (t < 0.25) {
                // Fase 1: Preparación (retroceso dramático)
                double prep = t / 0.25;
                attackPush = -prep * 10.0; // Retroceso GRANDE
                attackLift = prep * 5.0; // Levantar cuerpo MUY alto
                attackShake = Math.sin(prep * Math.PI * 4.0) * 0.5; // Temblor de anticipación
            } else if (t < 0.45) {
                // Fase 2: Lunge explosivo (adelante con mucha fuerza)
                double lung = (t - 0.25) / 0.2;
                attackPush = -10.0 + lung * 45.0; // De -10 a +35 (MUY lejos)
                attackJaw = Math.sin(lung * Math.PI) * 5.0; // Abrir mandíbula AMPLIAMENTE
                attackShake = Math.sin(lung * Math.PI * 12.0) * 2.0; // Sacudida rápida y violenta
                attackLift = 5.0 + lung * 3.0; // Máxima elevación
            } else if (t < 0.7) {
                // Fase 3: Mordida (mantener posición con sacudida)
                double bite = (t - 0.45) / 0.25;
                attackPush = 35.0; // Mantener extendido
                attackJaw = 5.0 - bite * 5.0; // Cerrar mandíbula gradualmente
                attackShake = Math.sin(bite * Math.PI * 20.0) * 1.5; // Sacudida violenta
                attackLift = 8.0 - bite * 2.0; // Empezar a bajar
            } else {
                // Fase 4: Retroceso y volver a posición
                double ret = (t - 0.7) / 0.3;
                attackPush = 35.0 * (1.0 - ret); // Volver suavemente
                attackJaw = 0; // Mandíbula cerrada
                attackLift = 6.0 * (1.0 - ret); // Bajar completamente
                attackShake = Math.sin(ret * Math.PI) * 0.5; // Sacudida residual
            }
        }
        
        for (int i = 0; i < voxels.size(); i++) {
            Vector3 v = voxels.get(i);
            
            // Aplicar animaciones EXAGERADAS Y DRAMÁTICAS
            Vector3 animatedV = v.copy();
            if (!isPaused && !isDying) {
                // PATAS con movimiento ALTERNADO muy visible
                if (v.y < 0) { // Es una pata
                    double lift = legSwing * 1.5; // Elevación dramática
                    double stride = legSwing * 1.0; // Zancada grande
                    double sway = Math.sin(animationTime * 6.0) * 0.5; // Balanceo extra
                    
                    if (v.z < 0) { // Patas delanteras
                        if (v.x < 0) { // Pata izquierda delantera
                            animatedV.y += lift;
                            animatedV.z += stride;
                            animatedV.x += sway;
                        } else { // Pata derecha delantera
                            animatedV.y -= lift;
                            animatedV.z -= stride;
                            animatedV.x -= sway;
                        }
                    } else { // Patas traseras
                        if (v.x < 0) { // Pata izquierda trasera
                            animatedV.y -= lift;
                            animatedV.z -= stride;
                            animatedV.x -= sway;
                        } else { // Pata derecha trasera
                            animatedV.y += lift;
                            animatedV.z += stride;
                            animatedV.x += sway;
                        }
                    }
                }
                
                // BRAZOS/HOMBROS con swing pronunciado
                if (v.y >= 0 && v.y <= 2 && Math.abs(v.x) >= 2 && v.z < 0) {
                    animatedV.x += Math.sin(animationTime * 8.0 + v.x) * 1.2; // Swing amplio
                    animatedV.z += Math.cos(animationTime * 8.0 + v.x) * 0.6;
                }
                
                // CUERPO con rebote vertical Y balanceo lateral EXAGERADO
                if (v.y >= 0 && v.y < 6) {
                    animatedV.y += bodyBounce; // Rebote vertical
                    // Balanceo lateral como depredador al acecho
                    animatedV.x += Math.sin(animationTime * 3.0) * 0.8;
                    // Rotación corporal sutil
                    double bodyTwist = Math.sin(animationTime * 2.5) * 0.3;
                    animatedV.z += bodyTwist * Math.abs(v.x) * 0.15;
                }
            }

            // COLA SERPENTINA con movimiento ondulante dramático
            if (v.z >= 4) {
                int tailSegment = (int)(v.z - 4);
                double tailPhase = tailSwing + tailSegment * 0.5; // Onda propagándose
                // Movimiento horizontal amplio (serpenteo)
                animatedV.x += Math.sin(tailPhase) * (2.0 + tailSegment * 0.3);
                // Movimiento vertical ondulante
                animatedV.y += Math.cos(tailPhase * 0.7) * 0.8;
                // Durante ataque: latigazo de cola
                if (isAttacking) {
                    double whipPhase = attackAnimation * Math.PI * 3.0;
                    animatedV.x += Math.sin(whipPhase + tailSegment) * 2.0;
                    animatedV.y += Math.abs(Math.sin(whipPhase)) * 1.5;
                }
            }

            // CUERNOS/OREJAS oscilando amenazantemente
            if (v.y >= 8 && Math.abs(v.x) >= 3) {
                double hornPhase = animationTime * 4.0 + Math.signum(v.x) * Math.PI;
                animatedV.x += Math.sin(hornPhase) * 0.8 + hornSwing * 0.5;
                animatedV.z += Math.cos(hornPhase) * 0.5;
                animatedV.y += Math.sin(hornPhase * 2.0) * 0.3; // Vibración
            }

            // CABEZA Y MANDÍBULA durante ataque (incluye hocico)
            if (isAttacking) {
                // Mandíbula inferior (abre)
                if (v.y == 5 && v.z <= -3) {
                    animatedV.z -= attackJaw * 1.2; // Apertura dramática
                    animatedV.y -= attackJaw * 0.8; // Baja la mandíbula
                }
                // Cabeza entera: lunge hacia adelante
                if (v.y >= 6 && v.y <= 8) {
                    animatedV.z -= attackPush * 0.15; // Se proyecta hacia la presa
                    animatedV.y += attackLift * 0.1; // Se eleva
                    animatedV.x += attackShake * Math.signum(v.x); // Sacudida lateral
                }
                // Dientes/colmillos se proyectan más
                if (v.y == 4 && v.z <= -4) {
                    animatedV.z -= attackJaw * 0.5;
                }
            }
            
            Vector3 rotatedVoxel = rotateVoxel(animatedV, yaw);
            
            // Aplicar offset de ataque hacia la dirección actual - NUEVO
            double attackOffsetX = 0;
            double attackOffsetZ = 0;
            if (isAttacking) {
                attackOffsetX = Math.sin(yaw) * attackPush;
                attackOffsetZ = -Math.cos(yaw) * attackPush;
            }
            
            Vector3 worldPos = new Vector3(
                posicion.x + rotatedVoxel.x * voxelSize * scale + attackOffsetX,
                posicion.y + rotatedVoxel.y * voxelSize * scale,
                posicion.z + rotatedVoxel.z * voxelSize * scale + attackOffsetZ
            );
            
            Color voxelColor = color;
            
            // Ojos SIEMPRE brillantes y grandes - MEJORADO para nueva geometría
            boolean isEye = false;
            if (v.z == -2 && v.y >= 7 && v.y <= 8) {
                if ((v.x >= -3 && v.x <= -1) || (v.x >= 1 && v.x <= 3)) {
                    isEye = true;
                }
            }
            
            if (isEye && eyesOpen) {
                // Color vibrante según el tipo de monstruo
                int colorType = (int)(seed % 3);
                if (colorType == 0) {
                    voxelColor = new Color(255, 255, 50); // Amarillo brillante para verde
                } else if (colorType == 1) {
                    voxelColor = new Color(255, 100, 255); // Rosa brillante para morado
                } else {
                    voxelColor = new Color(255, 255, 100); // Amarillo-naranja para rojo
                }

                // Pulso y destello al atacar
                double pulse = eyesOpen ? (1.0 + eyePulse) : 1.0;
                if (attackFlash > 0.0) {
                    pulse += attackFlash * 1.0;
                }
                int r = (int)Math.min(255, voxelColor.getRed() * pulse);
                int g = (int)Math.min(255, voxelColor.getGreen() * pulse * 0.9);
                int b = (int)Math.min(255, voxelColor.getBlue() * pulse * 0.9);
                voxelColor = new Color(r, g, b);
            }
            
            // Dientes/espinas más claras
            if ((v.y >= 6 && Math.abs(v.x) <= 2) || (v.y == 4 && Math.abs(v.z) >= 2)) {
                int r = Math.min(255, voxelColor.getRed() + 80);
                int g = Math.min(255, voxelColor.getGreen() + 80);
                int b = Math.min(255, voxelColor.getBlue() + 80);
                voxelColor = new Color(r, g, b);
            }

            // Destello general cuando ataca - MUY INTENSO Y VISIBLE
            if (attackFlash > 0.0) {
                // Durante el ataque, el CUERPO ENTERO se vuelve ROJO BRILLANTE
                int flashIntensity = (int)(255 * attackFlash);
                // Mezclar con rojo intenso
                int r = (int)Math.min(255, voxelColor.getRed() * (1.0 - attackFlash * 0.7) + flashIntensity);
                int g = (int)Math.min(255, voxelColor.getGreen() * (1.0 - attackFlash * 0.9));
                int b = (int)Math.min(255, voxelColor.getBlue() * (1.0 - attackFlash * 0.9));
                voxelColor = new Color(r, g, b);
            }
            
            // Glow effect when hovered
            if (hoverGlow > 0.0) {
                int r = (int) Math.min(255, voxelColor.getRed() + hoverGlow * 80);
                int g = (int) Math.min(255, voxelColor.getGreen() + hoverGlow * 80);
                int b = (int) Math.min(255, voxelColor.getBlue() + hoverGlow * 80);
                voxelColor = new Color(r, g, b);
            }
            
            Vector3[] verts = renderer.getCubeVertices(worldPos, voxelSize, yaw);
            renderer.drawCube(verts, camera, voxelColor);
        }
    }
    
    private void renderSpawnAnimation(SoftwareRenderer renderer, Camera camera) {
        double centerY = posicion.y;
        
        // Esfera de energía (roja oscura)
        double sphereRadius = 15.0 * (1.0 - spawnProgress);
        int numSphereParticles = 30;
        for (int i = 0; i < numSphereParticles; i++) {
            double angle = (i / (double) numSphereParticles) * Math.PI * 2;
            double heightOffset = Math.sin(angle * 3 + animationTime * 5) * 5;
            Vector3 particlePos = new Vector3(
                posicion.x + Math.cos(angle) * sphereRadius,
                centerY + heightOffset,
                posicion.z + Math.sin(angle) * sphereRadius
            );
            Color energyColor = new Color(180, 30, 30, (int)(200 * (1.0 - spawnProgress)));
            Vector3[] particleVerts = renderer.getCubeVertices(particlePos, 6, 0);
            renderer.drawCube(particleVerts, camera, energyColor);
        }
        
        // Partículas ascendentes
        for (SpawnParticle p : spawnParticles) {
            double progress = spawnProgress * p.speed;
            if (progress < 1.0) {
                double currentHeight = p.height + progress * (centerY - p.height);
                double currentRadius = p.radius * (1.0 - progress);
                Vector3 particlePos = new Vector3(
                    posicion.x + Math.cos(p.angle) * currentRadius,
                    currentHeight,
                    posicion.z + Math.sin(p.angle) * currentRadius
                );
                Color particleColor = new Color(200, 50, 50, (int)(255 * (1.0 - progress)));
                Vector3[] particleVerts = renderer.getCubeVertices(particlePos, 5, 0);
                renderer.drawCube(particleVerts, camera, particleColor);
            }
        }
        
        // Voxels del depredador apareciendo gradualmente
        double appearProgress = Math.max(0.0, (spawnProgress - 0.3) / 0.7);
        if (appearProgress > 0.0) {
            for (int i = 0; i < voxels.size(); i++) {
                if ((i / (double) voxels.size()) <= appearProgress) {
                    Vector3 v = voxels.get(i);
                    Vector3 worldPos = new Vector3(
                        posicion.x + v.x * voxelSize,
                        posicion.y + v.y * voxelSize,
                        posicion.z + v.z * voxelSize
                    );
                    Vector3[] verts = renderer.getCubeVertices(worldPos, voxelSize, 0);
                    renderer.drawCube(verts, camera, color);
                }
            }
        }
    }
    
    private void renderDeathAnimation(SoftwareRenderer renderer, Camera camera) {
        double fadeOut = 1.0 - deathProgress;
        
        // FASE 1: Colapso y oscurecimiento (0-0.4)
        // FASE 2: Explosión de energía roja (0.4-0.7)
        // FASE 3: Desvanecimiento de partículas (0.7-1.0)
        
        // Render fading voxels con efecto de colapso
        for (int i = 0; i < voxels.size(); i++) {
            Vector3 v = voxels.get(i);
            Vector3 rotatedVoxel = rotateVoxel(v, yaw);
            
            // Efecto de colapso: todo se mueve hacia el centro
            double collapseAmount = Math.min(1.0, deathProgress * 2.0); // 0-1 en los primeros 0.5s
            Vector3 collapsedVoxel = new Vector3(
                rotatedVoxel.x * (1.0 - collapseAmount * 0.8),
                rotatedVoxel.y * (1.0 - collapseAmount * 0.9) - deathProgress * 15, // Cae hacia abajo
                rotatedVoxel.z * (1.0 - collapseAmount * 0.8)
            );
            
            Vector3 worldPos = new Vector3(
                posicion.x + collapsedVoxel.x * voxelSize,
                posicion.y + collapsedVoxel.y * voxelSize,
                posicion.z + collapsedVoxel.z * voxelSize
            );
            
            // Cambiar color según fase
            int r, g, b;
            if (deathProgress < 0.4) {
                // Fase 1: Oscurecer gradualmente
                double darkFactor = 1.0 - (deathProgress / 0.4) * 0.7;
                r = (int)(color.getRed() * darkFactor * fadeOut);
                g = (int)(color.getGreen() * darkFactor * fadeOut);
                b = (int)(color.getBlue() * darkFactor * fadeOut);
            } else if (deathProgress < 0.7) {
                // Fase 2: Destello rojo intenso (explosión de energía)
                double explosionPhase = (deathProgress - 0.4) / 0.3;
                double redFlash = 1.0 - explosionPhase;
                r = (int)Math.min(255, 255 * redFlash + 50);
                g = (int)(50 * fadeOut);
                b = (int)(30 * fadeOut);
            } else {
                // Fase 3: Desvanecimiento
                r = (int)(50 * fadeOut);
                g = (int)(20 * fadeOut);
                b = (int)(20 * fadeOut);
            }
            
            Color deathColor = new Color(Math.max(0, r), Math.max(0, g), Math.max(0, b));
            Vector3[] verts = renderer.getCubeVertices(worldPos, voxelSize, 0);
            renderer.drawCube(verts, camera, deathColor);
        }
        
        // Render death particles - EXPLOSIONES DE FUEGO
        for (DeathParticle p : deathParticles) {
            if (p.life > 0) {
                // Partículas se expanden rápidamente
                double expansion = 1.0 - p.life;
                Vector3 expandedPos = new Vector3(
                    p.position.x + p.velocity.x * expansion * 2.0,
                    p.position.y + p.velocity.y * expansion * 1.5,
                    p.position.z + p.velocity.z * expansion * 2.0
                );
                
                int alpha = (int)(p.life * 255);
                // Cambiar color de las partículas: rojo -> naranja -> desvanecerse
                int pr = (int)Math.min(255, 255 * p.life + 100);
                int pg = (int)(100 * p.life);
                int pb = (int)(50 * p.life);
                
                Color particleColor = new Color(
                    Math.max(0, Math.min(255, pr)),
                    Math.max(0, Math.min(255, pg)),
                    Math.max(0, Math.min(255, pb)),
                    Math.max(0, Math.min(255, alpha))
                );
                Vector3[] verts = renderer.getCubeVertices(expandedPos, (int)(voxelSize * 0.6 * p.life), 0);
                renderer.drawCube(verts, camera, particleColor);
            }
        }
        
        // EFECTO VISUAL: Área de impacto/onda de choque (en la fase 2)
        if (deathProgress >= 0.4 && deathProgress < 0.7) {
            double explosionWave = (deathProgress - 0.4) / 0.3; // 0-1
            double waveRadius = explosionWave * 50.0; // Expande hasta 50 unidades
            int numWaveParticles = (int)(30 * explosionWave);
            
            for (int i = 0; i < numWaveParticles; i++) {
                double angle = (i / (double)numWaveParticles) * Math.PI * 2;
                double waveX = posicion.x + Math.cos(angle) * waveRadius;
                double waveZ = posicion.z + Math.sin(angle) * waveRadius;
                double waveY = posicion.y + 10 - explosionWave * 20; // Baja mientras se expande
                
                // Partículas de onda
                Vector3 wavePos = new Vector3(waveX, waveY, waveZ);
                Color waveColor = new Color(
                    (int)(255 * (1.0 - explosionWave)),
                    (int)(150 * (1.0 - explosionWave)),
                    50
                );
                Vector3[] waveVerts = renderer.getCubeVertices(wavePos, 4, 0);
                renderer.drawCube(waveVerts, camera, waveColor);
            }
        }
    }
    
    private Vector3 rotateVoxel(Vector3 v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vector3(
            v.x * cos - v.z * sin,
            v.y,
            v.x * sin + v.z * cos
        );
    }
    
    // Collidable implementation
    @Override
    public Vector3 getAABBMin() {
        double minX = posicion.x - voxelSize * 5.0;
        double minY = posicion.y - voxelSize * 5.0;
        double minZ = posicion.z - voxelSize * 5.0;
        return new Vector3(minX, minY, minZ);
    }
    
    @Override
    public Vector3 getAABBMax() {
        double maxX = posicion.x + voxelSize * 5.0;
        double maxY = posicion.y + voxelSize * 5.0;
        double maxZ = posicion.z + voxelSize * 5.0;
        return new Vector3(maxX, maxY, maxZ);
    }
    
    // Utility methods (not from interface)
    public Vector3 getPosition() {
        return posicion;
    }
    
    public double getCollisionRadius() {
        return voxelSize * 5.0;
    }
    
    // Selection methods
    public void setHovered(boolean hovered) {
        this.isHovered = hovered;
    }
    
    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }
    
    public boolean isHovered() {
        return isHovered;
    }
    
    public boolean isSelected() {
        return isSelected;
    }
    
    public int getDepredadorId() {
        return depredadorId;
    }
    
    public long getSpawnTime() {
        return spawnTime;
    }
    
    public double getSpeed() {
        return baseSpeed;
    }
    
    public boolean isMarkedForDeath() {
        return markedForDeath;
    }
    
    public void markForDeath() {
        this.markedForDeath = true;
        if (!isDying) {
            isDying = true;
            deathProgress = 0.0;
            initializeDeathAnimation();
        }
    }
    
    private void initializeDeathAnimation() {
        Random r = new Random(seed + System.currentTimeMillis());
        for (int i = 0; i < voxels.size(); i++) {
            Vector3 v = voxels.get(i);
            Vector3 rotatedVoxel = rotateVoxel(v, yaw);
            Vector3 worldPos = new Vector3(
                posicion.x + rotatedVoxel.x * voxelSize,
                posicion.y + rotatedVoxel.y * voxelSize,
                posicion.z + rotatedVoxel.z * voxelSize
            );
            
            // Create particles with random velocities
            int particlesPerVoxel = 2;
            for (int p = 0; p < particlesPerVoxel; p++) {
                Vector3 vel = new Vector3(
                    (r.nextDouble() - 0.5) * 3.0,
                    r.nextDouble() * 4.0 + 2.0,
                    (r.nextDouble() - 0.5) * 3.0
                );
                deathParticles.add(new DeathParticle(worldPos, vel, color));
            }
        }
    }
    
    public boolean isDying() {
        return isDying;
    }
    
    public double getDeathProgress() {
        return deathProgress;
    }
    
    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }
    
    public boolean isPaused() {
        return isPaused;
    }
    
    public static void setWorldReference(simulation.Mundo world) {
        worldRef = world;
    }
}
