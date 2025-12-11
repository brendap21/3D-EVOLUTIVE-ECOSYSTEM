package entities;

import main.Renderable;
import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import entities.Collidable;

/**
 * Clase base para todos los animales con animación de spawn compartida.
 */
public abstract class BaseAnimal implements Renderable, Collidable {
    protected Vector3 posicion;
    protected List<Vector3> voxels;
    protected int voxelSize;
    protected Color color;
    protected long seed;
    protected Color originalColor;
    protected int baseVoxelSize;
    
    // Growth phases
    protected int growthPhase = 1; // 1, 2, 3
    protected double phaseTimer = 0.0;
    protected double transitionPulse = 0.0; // animation pulse when changing phase
    
    // Método abstracto para que cada especie defina su tiempo de evolución por fase (fase 1, 2, o 3)
    protected abstract double getPhaseDuration(int phase);
    
    // Método abstracto para que cada especie defina sus cambios visuales por fase
    protected abstract void applyPhaseVisuals();
    
    // Método abstracto para que cada especie devuelva su tipo (0-9)
    public abstract int getSpeciesType();
    
    // Movement / wandering
    protected Vector3 velocity = new Vector3(0, 0, 0);
    protected double baseSpeed = 1.2;
    protected double wanderTimer = 0.0;
    protected static final double WANDER_CHANGE_INTERVAL = 3.5;
    protected static final double WORLD_BOUND = 320.0;
    protected static simulation.Mundo worldRef = null; // shared reference set from app
    protected double yaw = 0.0; // rotación del animal hacia donde mira
    protected boolean movementInitialized = false; // flag para inicializar el movimiento una sola vez
    
    // Selection and hover states
    private boolean isHovered = false;
    private boolean isSelected = false;
    private double hoverGlow = 0.0;
    private double selectionScale = 1.0;
    private static int nextAnimalId = 1;
    private int animalId;
    
    // Tracking for UI display
    private long spawnTime = 0L; // System.currentTimeMillis() when animal spawned
    private long phaseStartTime = 0L; // System.currentTimeMillis() when current phase started
    
    // Reproduction system
    private double reproductionCooldown = 0.0; // Cooldown timer to prevent multiple spawns
    private static final double REPRODUCTION_COOLDOWN_DURATION = 10.0; // 10 seconds between reproductions
    private boolean isMating = false; // Flag para animación de apareamiento
    private double matingProgress = 0.0; // 0.0 a 1.0
    private static final double MATING_DURATION = 1.5; // 1.5 segundos de animación
    private BaseAnimal matingPartner = null; // Referencia al compañero
    private boolean offspringSpawned = false; // Flag para evitar crear dos crías
    
    // Death system
    private boolean markedForDeath = false;
    private boolean isDying = false;
    private double deathProgress = 0.0;
    private static final double DEATH_DURATION = 1.0; // 1 segundo de animación de muerte
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
    
    // Spawn animation
    private boolean isSpawning = true;
    private double spawnProgress = 0.0;
    private static final double SPAWN_DURATION = 0.8;
    private List<SpawnParticle> spawnParticles = new ArrayList<>();
    
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
    
    protected void initializeSpawnAnimation() {
        this.animalId = nextAnimalId++;
        this.spawnTime = System.currentTimeMillis();
        Random r = new Random(seed + 999);
        for (int i = 0; i < voxels.size(); i++) {
            int particlesPerVoxel = 3;
            for (int p = 0; p < particlesPerVoxel; p++) {
                double angle = r.nextDouble() * Math.PI * 2;
                double height = -15 - r.nextDouble() * 10;
                double radius = 8 + r.nextDouble() * 12;
                double speed = 1.2 + r.nextDouble() * 0.6;
                spawnParticles.add(new SpawnParticle(angle, height, radius, speed, i));
            }
        }
    }
    
    /** Set a shared world reference so animals can consult collisions/terrain. */
    public static void setWorld(simulation.Mundo world) {
        worldRef = world;
    }
    
    @Override
    public void update() {
        if (isSpawning) {
            spawnProgress += 0.016 / SPAWN_DURATION;
            if (spawnProgress >= 1.0) {
                spawnProgress = 1.0;
                isSpawning = false;
                spawnParticles.clear();
                System.out.println("Animal " + animalId + " terminó spawn animation");
            }
        }

        updateGrowthPhase();
        updateMovement();
        
        // Animate hover glow
        if (isHovered) {
            hoverGlow = Math.min(1.0, hoverGlow + 0.1);
        } else {
            hoverGlow = Math.max(0.0, hoverGlow - 0.1);
        }
        
        // Animate selection scale
        double targetScale = isSelected ? 1.15 : 1.0;
        if (selectionScale < targetScale) {
            selectionScale = Math.min(targetScale, selectionScale + 0.02);
        } else if (selectionScale > targetScale) {
            selectionScale = Math.max(targetScale, selectionScale - 0.02);
        }
    }
    
    @Override
    public void render(SoftwareRenderer renderer, Camera cam) {
        if (isDying) {
            renderDeathAnimation(renderer, cam);
            return;
        }
        
        if (isSpawning) {
            renderSpawnAnimation(renderer, cam);
        } else {
            renderNormal(renderer, cam);
            // Renderizar esfera de energía si está evolucionando
            if (transitionPulse > 0) {
                renderEvolutionSphere(renderer, cam);
            }
            // Renderizar animación de apareamiento si está en proceso
            if (isMating) {
                renderMatingAnimation(renderer, cam);
            }
        }
    }
    
    private void renderSpawnAnimation(SoftwareRenderer renderer, Camera cam) {
        // Fase 1 (0.0 - 0.35): Partículas en espiral
        if (spawnProgress < 0.35) {
            double particlePhase = spawnProgress / 0.35;
            for (SpawnParticle p : spawnParticles) {
                double targetY = voxels.get(p.voxelIndex).y * voxelSize + posicion.y;
                double currentRadius = p.radius * (1.0 - particlePhase);
                double currentHeight = p.height + (targetY - p.height) * particlePhase;
                double spiralAngle = p.angle + particlePhase * Math.PI * 4 * p.speed;
                
                double px = posicion.x + Math.cos(spiralAngle) * currentRadius;
                double py = currentHeight;
                double pz = posicion.z + Math.sin(spiralAngle) * currentRadius;
                
                int particleSize = 5 + (int)(3 * particlePhase);
                Vector3[] particleVerts = renderer.getCubeVertices(new Vector3(px, py, pz), particleSize, 0);
                Color particleColor = new Color(255, 255, 150);
                renderer.drawCube(particleVerts, cam, particleColor);
            }
        }
        
        // Fase 2 (0.25 - 1.0): Voxels aparecen
        if (spawnProgress > 0.25) {
            double voxelPhase = Math.min(1.0, (spawnProgress - 0.25) / 0.75);
            
            for (int i = 0; i < voxels.size(); i++) {
                Vector3 off = voxels.get(i);
                double voxelDelay = (double)i / voxels.size() * 0.2;
                double individualProgress = Math.max(0, Math.min(1.0, (voxelPhase - voxelDelay) / 0.8));
                
                if (individualProgress > 0) {
                    double scale = individualProgress < 0.7 
                        ? individualProgress / 0.7 
                        : 1.0 + Math.sin((individualProgress - 0.7) / 0.3 * Math.PI) * 0.2;
                    
                    double spinRotation = (1.0 - individualProgress) * Math.PI * 3;
                    
                    Vector3 world = new Vector3(
                        posicion.x + off.x * voxelSize,
                        posicion.y + off.y * voxelSize + (1.0 - individualProgress) * 10,
                        posicion.z + off.z * voxelSize
                    );
                    
                    int scaledSize = Math.max(1, (int)(voxelSize * scale));
                    Vector3[] verts = renderer.getCubeVertices(world, scaledSize, spinRotation);
                    
                    float glow = (float)(Math.max(0, 1.2 - individualProgress * 1.2));
                    Color voxelColor = new Color(
                        Math.min(1.0f, color.getRed() / 255f + glow * 0.4f),
                        Math.min(1.0f, color.getGreen() / 255f + glow * 0.4f),
                        Math.min(1.0f, color.getBlue() / 255f + glow * 0.4f)
                    );
                    renderer.drawCube(verts, cam, voxelColor);
                }
            }
            
            // Fase 3 (0.75 - 1.0): Destello final
            if (spawnProgress > 0.75) {
                double burstPhase = (spawnProgress - 0.75) / 0.25;
                int burstParticles = 12;
                
                for (int i = 0; i < burstParticles; i++) {
                    double angle = (i / (double)burstParticles) * Math.PI * 2;
                    double burstRadius = burstPhase * 25;
                    double burstHeight = posicion.y + voxelSize * 1.5 + burstPhase * 12;
                    
                    double bx = posicion.x + Math.cos(angle) * burstRadius;
                    double by = burstHeight;
                    double bz = posicion.z + Math.sin(angle) * burstRadius;
                    
                    int burstSize = Math.max(1, (int)(5 * (1.0 - burstPhase)));
                    Vector3[] burstVerts = renderer.getCubeVertices(new Vector3(bx, by, bz), burstSize, 0);
                    Color burstColor = new Color(255, 255, 100);
                    renderer.drawCube(burstVerts, cam, burstColor);
                }
            }
        }
    }
    
    /**
     * Renderiza la animación de muerte: partículas dispersándose.
     */
    protected void renderDeathAnimation(SoftwareRenderer renderer, Camera cam) {
        // Voxels del animal se vuelven más transparentes
        double fadeOut = 1.0 - deathProgress;
        
        // Renderizar voxels del animal desapareciendo
        if (deathProgress < 0.5) {
            for (Vector3 v : voxels) {
                Vector3 rotatedVoxel = rotateVoxel(v, yaw);
                Vector3 worldPos = new Vector3(
                    posicion.x + rotatedVoxel.x * voxelSize,
                    posicion.y + rotatedVoxel.y * voxelSize,
                    posicion.z + rotatedVoxel.z * voxelSize
                );
                
                // Color que se oscurece
                int alpha = (int)(255 * fadeOut * 2.0); // Desaparece en la primera mitad
                Color fadedColor = new Color(
                    Math.max(0, color.getRed() - (int)(color.getRed() * deathProgress)),
                    Math.max(0, color.getGreen() - (int)(color.getGreen() * deathProgress)),
                    Math.max(0, color.getBlue() - (int)(color.getBlue() * deathProgress))
                );
                
                Vector3[] verts = renderer.getCubeVertices(worldPos, voxelSize, yaw);
                renderer.drawCube(verts, cam, fadedColor);
            }
        }
        
        // Renderizar partículas de muerte
        for (DeathParticle p : deathParticles) {
            if (p.life > 0) {
                int alpha = (int)(255 * p.life);
                Color particleColor = new Color(
                    p.color.getRed(),
                    p.color.getGreen(),
                    p.color.getBlue(),
                    Math.min(255, Math.max(0, alpha))
                );
                
                int particleSize = Math.max(2, (int)(voxelSize * 0.6 * p.life));
                Vector3[] particleVerts = renderer.getCubeVertices(p.position, particleSize, 0);
                renderer.drawCube(particleVerts, cam, particleColor);
            }
        }
    }
    
    // Método abstracto que cada tipo de animal debe implementar
    protected abstract void renderNormal(SoftwareRenderer renderer, Camera cam);
    
    /**
     * Renderiza la animación de apareamiento: corazones flotantes entre los dos animales.
     */
    protected void renderMatingAnimation(SoftwareRenderer renderer, Camera cam) {
        if (matingPartner == null || matingProgress >= 1.0) return;
        
        // Posición entre los dos animales
        double midX = (posicion.x + matingPartner.posicion.x) / 2.0;
        double midY = (posicion.y + matingPartner.posicion.y) / 2.0;
        double midZ = (posicion.z + matingPartner.posicion.z) / 2.0;
        
        // Color rosa/rojo para los corazones
        Color heartColor = new Color(255, 100, 150);
        
        // Generar múltiples corazones que flotan hacia arriba
        int heartCount = 8;
        for (int i = 0; i < heartCount; i++) {
            double angle = (2 * Math.PI * i / heartCount) + matingProgress * Math.PI * 2;
            double radius = voxelSize * 3.0;
            double heightOffset = matingProgress * voxelSize * 8.0;
            
            // Movimiento en espiral
            double x = midX + Math.cos(angle) * radius * (1.0 - matingProgress * 0.5);
            double y = midY + heightOffset + Math.sin(matingProgress * Math.PI * 4) * voxelSize;
            double z = midZ + Math.sin(angle) * radius * (1.0 - matingProgress * 0.5);
            
            // Tamaño del corazón (voxel)
            int heartSize = Math.max(2, (int)(voxelSize * 0.8 * (1.0 - matingProgress * 0.5)));
            
            // Brillo pulsante
            float brightness = (float)(0.7 + 0.3 * Math.sin(matingProgress * Math.PI * 6 + i));
            Color pulseColor = new Color(
                heartColor.getRed(),
                Math.min(255, (int)(heartColor.getGreen() * brightness)),
                Math.min(255, (int)(heartColor.getBlue() * brightness))
            );
            
            Vector3[] heartVerts = renderer.getCubeVertices(new Vector3(x, y, z), heartSize, angle);
            renderer.drawCube(heartVerts, cam, pulseColor);
        }
        
        // Partículas brillantes adicionales
        if (matingProgress > 0.5) {
            int sparkCount = 12;
            for (int i = 0; i < sparkCount; i++) {
                double sparkAngle = (2 * Math.PI * i / sparkCount) + matingProgress * Math.PI * 4;
                double sparkRadius = voxelSize * 2.0;
                double sparkHeight = midY + (matingProgress - 0.5) * voxelSize * 4.0;
                
                double sx = midX + Math.cos(sparkAngle) * sparkRadius;
                double sz = midZ + Math.sin(sparkAngle) * sparkRadius;
                
                int sparkSize = Math.max(1, (int)(voxelSize * 0.4));
                Color sparkColor = new Color(255, 200, 220);
                
                Vector3[] sparkVerts = renderer.getCubeVertices(new Vector3(sx, sparkHeight, sz), sparkSize, 0);
                renderer.drawCube(sparkVerts, cam, sparkColor);
            }
        }
    }
    
    /**
     * Renderiza una esfera de energía durante la evolución.
     * Efecto visual: esfera brillante que envuelve al animal con partículas.
     * Se ajusta al tamaño actual del animal según su fase.
     */
    protected void renderEvolutionSphere(SoftwareRenderer renderer, Camera cam) {
        if (transitionPulse <= 0) return;
        
        double pulseIntensity = transitionPulse;
        
        // Obtener el tamaño real del animal basado en su AABB
        Vector3 minBounds = getAABBMin();
        Vector3 maxBounds = getAABBMax();
        double animalWidth = maxBounds.x - minBounds.x;
        double animalHeight = maxBounds.y - minBounds.y;
        double animalDepth = maxBounds.z - minBounds.z;
        
        // Radio base de la esfera ajustado al tamaño del animal (el más grande de las dimensiones)
        double baseRadius = Math.max(Math.max(animalWidth, animalHeight), animalDepth) * 0.6;
        
        // Centro del animal (punto medio del AABB)
        double centerX = (minBounds.x + maxBounds.x) / 2.0;
        double centerY = (minBounds.y + maxBounds.y) / 2.0;
        double centerZ = (minBounds.z + maxBounds.z) / 2.0;
        
        // Fase 1 (1.0 - 0.7): Esfera expandiéndose desde el centro
        // Fase 2 (0.7 - 0.3): Esfera brillante estable con ondulaciones
        // Fase 3 (0.3 - 0.0): Esfera contrayéndose con explosión de partículas
        
        double sphereRadius = baseRadius;
        if (pulseIntensity > 0.7) {
            // Expansión inicial: empieza muy pequeña y crece
            double expandPhase = (pulseIntensity - 0.7) / 0.3;
            sphereRadius = baseRadius * 0.2 * (1.0 - expandPhase) + baseRadius * 1.3 * expandPhase;
        } else if (pulseIntensity > 0.3) {
            // Fase estable con ondulación sutil
            double stablePhase = (pulseIntensity - 0.3) / 0.4;
            double oscillation = Math.sin(pulseIntensity * Math.PI * 8) * 0.1;
            sphereRadius = baseRadius * 1.3 * (1.0 + oscillation);
        } else {
            // Contracción final
            double contractPhase = pulseIntensity / 0.3;
            sphereRadius = baseRadius * 1.3 * contractPhase;
        }
        
        // Colores creativos que cambian con la fase de evolución
        Color energyColor;
        if (growthPhase == 2) {
            // Fase 1->2: Verde-cyan (crecimiento natural)
            energyColor = new Color(
                (int)(100 + pulseIntensity * 100),
                (int)(220 + pulseIntensity * 35),
                (int)(150 + pulseIntensity * 105)
            );
        } else if (growthPhase == 3) {
            // Fase 2->3: Dorado-naranja (madurez/poder)
            energyColor = new Color(
                (int)(220 + pulseIntensity * 35),
                (int)(180 + pulseIntensity * 75),
                (int)(50 + pulseIntensity * 100)
            );
        } else {
            // Por defecto: Cyan brillante
            energyColor = new Color(
                (int)(150 + pulseIntensity * 105),
                (int)(220 + pulseIntensity * 35),
                255
            );
        }
        
        // Renderizar esfera como múltiples capas de voxels en distribución esférica
        int layers = Math.max(8, (int)(baseRadius / voxelSize)); // Más capas para animales grandes
        int voxelsPerLayer = Math.max(12, (int)(baseRadius / voxelSize * 2)); // Más voxels por capa para animales grandes
        
        for (int layer = 0; layer < layers; layer++) {
            double phi = Math.PI * layer / (layers - 1); // ángulo vertical (0 a π)
            double layerRadius = sphereRadius * Math.sin(phi);
            double yOffset = sphereRadius * Math.cos(phi);
            
            int voxelsInThisLayer = Math.max(6, (int)(voxelsPerLayer * Math.sin(phi))); // Menos voxels en los polos
            
            for (int i = 0; i < voxelsInThisLayer; i++) {
                double theta = 2 * Math.PI * i / voxelsInThisLayer; // ángulo horizontal
                
                // Rotación animada para dar dinamismo
                double animRotation = pulseIntensity * Math.PI * 6;
                theta += animRotation;
                
                // Ondulación radial
                double radialWave = Math.sin(phi * 4 + pulseIntensity * Math.PI * 4) * 0.15;
                double effectiveRadius = layerRadius * (1.0 + radialWave);
                
                double x = centerX + effectiveRadius * Math.cos(theta);
                double y = centerY + yOffset;
                double z = centerZ + effectiveRadius * Math.sin(theta);
                
                // Tamaño de voxel proporcional al animal
                int size = (int)(voxelSize * 0.7 * (0.5 + 0.5 * pulseIntensity));
                if (size < 2) size = 2;
                
                // Brillo variable con ondas
                float brightness = (float)(0.6 + 0.4 * Math.sin(pulseIntensity * Math.PI * 3 + phi * 2));
                Color voxelColor = new Color(
                    Math.min(255, (int)(energyColor.getRed() * brightness)),
                    Math.min(255, (int)(energyColor.getGreen() * brightness)),
                    Math.min(255, (int)(energyColor.getBlue() * brightness))
                );
                
                Vector3[] verts = renderer.getCubeVertices(new Vector3(x, y, z), size, 0);
                renderer.drawCube(verts, cam, voxelColor);
            }
        }
        
        // Anillos orbitales creativos en fase media
        if (pulseIntensity > 0.3 && pulseIntensity < 0.8) {
            int ringCount = 3;
            for (int ring = 0; ring < ringCount; ring++) {
                double ringRadius = sphereRadius * (0.8 + ring * 0.3);
                int particlesInRing = 16;
                double ringRotation = pulseIntensity * Math.PI * (4 + ring);
                
                for (int i = 0; i < particlesInRing; i++) {
                    double angle = (2 * Math.PI * i / particlesInRing) + ringRotation;
                    
                    // Anillos en diferentes planos
                    double rx, ry, rz;
                    if (ring % 3 == 0) {
                        // Anillo horizontal
                        rx = centerX + ringRadius * Math.cos(angle);
                        ry = centerY;
                        rz = centerZ + ringRadius * Math.sin(angle);
                    } else if (ring % 3 == 1) {
                        // Anillo vertical (plano XY)
                        rx = centerX + ringRadius * Math.cos(angle);
                        ry = centerY + ringRadius * Math.sin(angle);
                        rz = centerZ;
                    } else {
                        // Anillo vertical (plano YZ)
                        rx = centerX;
                        ry = centerY + ringRadius * Math.sin(angle);
                        rz = centerZ + ringRadius * Math.cos(angle);
                    }
                    
                    int ringSize = (int)(voxelSize * 0.5);
                    if (ringSize < 2) ringSize = 2;
                    
                    Color ringColor = new Color(
                        Math.min(255, energyColor.getRed() + 30),
                        Math.min(255, energyColor.getGreen() + 30),
                        Math.min(255, energyColor.getBlue())
                    );
                    
                    Vector3[] ringVerts = renderer.getCubeVertices(new Vector3(rx, ry, rz), ringSize, angle);
                    renderer.drawCube(ringVerts, cam, ringColor);
                }
            }
        }
        
        // Partículas de explosión en fase final (pulseIntensity < 0.3)
        if (pulseIntensity < 0.3) {
            int particleCount = Math.max(24, (int)(baseRadius / voxelSize * 3)); // Más partículas para animales grandes
            double burstPhase = 1.0 - (pulseIntensity / 0.3);
            
            for (int i = 0; i < particleCount; i++) {
                // Distribución esférica de partículas
                double theta = 2 * Math.PI * i / particleCount;
                double phi = Math.acos(2.0 * (i % 8) / 8.0 - 1.0);
                
                double burstRadius = burstPhase * baseRadius * 3.0;
                
                double px = centerX + burstRadius * Math.sin(phi) * Math.cos(theta);
                double py = centerY + burstRadius * Math.cos(phi);
                double pz = centerZ + burstRadius * Math.sin(phi) * Math.sin(theta);
                
                // Añadir ondulación a las partículas
                double wave = Math.sin(burstPhase * Math.PI * 2 + i * 0.5) * baseRadius * 0.3;
                py += wave;
                
                int pSize = Math.max(2, (int)(voxelSize * 0.8 * (1.0 - burstPhase)));
                
                // Estela de partícula (trail effect)
                Color particleColor = new Color(
                    Math.min(255, energyColor.getRed() + (int)(55 * (1 - burstPhase))),
                    Math.min(255, energyColor.getGreen() + (int)(35 * (1 - burstPhase))),
                    Math.min(255, energyColor.getBlue())
                );
                
                Vector3[] pVerts = renderer.getCubeVertices(new Vector3(px, py, pz), pSize, burstPhase * Math.PI * 4);
                renderer.drawCube(pVerts, cam, particleColor);
            }
        }
        
        // Núcleo brillante central que pulsa
        if (pulseIntensity > 0.2) {
            double coreScale = 1.0 + Math.sin(pulseIntensity * Math.PI * 8) * 0.3;
            int coreSize = (int)(voxelSize * 2.0 * pulseIntensity * coreScale);
            if (coreSize < 3) coreSize = 3;
            
            Color coreColor = new Color(255, 255, 255);
            Vector3 corePos = new Vector3(centerX, centerY, centerZ);
            Vector3[] coreVerts = renderer.getCubeVertices(corePos, coreSize, pulseIntensity * Math.PI * 6);
            renderer.drawCube(coreVerts, cam, coreColor);
            
            // Destello adicional en el núcleo
            if (pulseIntensity > 0.5) {
                int flareSize = (int)(coreSize * 0.6);
                Color flareColor = new Color(
                    255,
                    Math.min(255, 200 + (int)(55 * pulseIntensity)),
                    Math.min(255, 150 + (int)(105 * pulseIntensity))
                );
                Vector3[] flareVerts = renderer.getCubeVertices(corePos, flareSize, -pulseIntensity * Math.PI * 4);
                renderer.drawCube(flareVerts, cam, flareColor);
            }
        }
    }
    
    // Helper methods for rendering with glow/scale effects
    protected Color applyGlowToColor(Color baseColor) {
        if (hoverGlow <= 0 && !isSelected) return baseColor;
        
        float glowAmount = (float)(hoverGlow * 0.3 + (isSelected ? 0.3 : 0));
        return new Color(
            Math.min(1.0f, baseColor.getRed() / 255f + glowAmount),
            Math.min(1.0f, baseColor.getGreen() / 255f + glowAmount),
            Math.min(1.0f, baseColor.getBlue() / 255f + glowAmount)
        );
    }
    
    protected int applyScaleToSize(int baseSize) {
        return (int)(baseSize * selectionScale);
    }
    
    protected Vector3 applyScaleToPosition(Vector3 basePos) {
        if (selectionScale == 1.0) return basePos;
        // Scale around center position
        double dx = basePos.x - posicion.x;
        double dy = basePos.y - posicion.y;
        double dz = basePos.z - posicion.z;
        return new Vector3(
            posicion.x + dx * selectionScale,
            posicion.y + dy * selectionScale,
            posicion.z + dz * selectionScale
        );
    }
    
    // Rotar un vector relativo alrededor del eje Y (yaw)
    protected Vector3 rotateY(Vector3 v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vector3(
            v.x * cos - v.z * sin,
            v.y,
            v.x * sin + v.z * cos
        );
    }
    
    // Aplicar rotación y posición a un punto relativo
    protected Vector3 applyTransform(Vector3 relativePos) {
        // Ajustar yaw con -π/2 porque los modelos miran hacia +Z, pero velocity apunta en ángulo desde +X
        Vector3 rotated = rotateY(relativePos, yaw - Math.PI / 2);
        return new Vector3(
            posicion.x + rotated.x,
            posicion.y + rotated.y,
            posicion.z + rotated.z
        );
    }

    // --- Growth & movement helpers ---
    protected double getPhaseScaleMultiplier() {
        switch (growthPhase) {
            case 2: return 3.0;   // fase 2 = 3x fase 1
            case 3: return 6.0;   // fase 3 = 2x fase 2
            default: return 1.0;
        }
    }

    protected double getPhaseSpeedMultiplier() {
        switch (growthPhase) {
            case 2: return 1.25;
            case 3: return 1.5;
            default: return 1.0;
        }
    }

    protected void updateGrowthPhase() {
        if (isSpawning) return; // don't change during spawn
        
        // Inicializar el timer si no está inicializado
        if (phaseStartTime == 0) {
            phaseStartTime = System.currentTimeMillis();
        }
        
        // Calcular el tiempo transcurrido en esta fase en segundos
        long currentTime = System.currentTimeMillis();
        double elapsedSeconds = (currentTime - phaseStartTime) / 1000.0;
        
        // Verificar si ha pasado el tiempo necesario para la siguiente fase
        if (elapsedSeconds >= getPhaseDuration(growthPhase) && growthPhase < 3) {
            System.out.println("¡EVOLUCIÓN! Animal " + animalId + " cambió de fase " + growthPhase + " a " + (growthPhase + 1));
            growthPhase++;
            phaseStartTime = System.currentTimeMillis(); // Reiniciar el contador para la nueva fase
            transitionPulse = 1.0; // trigger animation pulse
            applyPhaseVisuals(); // Aplicar cambios visuales específicos de la especie
            
            // Reajustar altura al terreno con el nuevo tamaño
            if (worldRef != null) {
                double terrainHeight = worldRef.getHeightAt(posicion.x, posicion.z);
                if (terrainHeight != Double.NEGATIVE_INFINITY) {
                    double baseOffset = voxelSize * 1.5;
                    double phaseScale = getPhaseScaleMultiplier();
                    double groundOffset = baseOffset * phaseScale;
                    posicion = new Vector3(posicion.x, terrainHeight + groundOffset, posicion.z);
                }
            }
        }
        if (transitionPulse > 0) {
            transitionPulse = Math.max(0, transitionPulse - 0.04);
        }
        selectionScale = selectionScale * 0.98 + getPhaseScaleMultiplier() * 0.02;
        
        // Update reproduction cooldown
        if (reproductionCooldown > 0) {
            reproductionCooldown -= 0.016; // ~60 FPS
        }
        
        // Update death animation
        if (isDying) {
            deathProgress += 0.016 / DEATH_DURATION;
            
            // Update death particles
            for (DeathParticle p : deathParticles) {
                p.position.x += p.velocity.x;
                p.position.y += p.velocity.y;
                p.position.z += p.velocity.z;
                p.velocity.y -= 0.2; // Gravity
                p.life -= 0.016 / DEATH_DURATION;
            }
            
            if (deathProgress >= 1.0) {
                markedForDeath = true; // Ready to be removed
            }
            return; // Don't process other animations while dying
        }
        
        // Update mating animation
        if (isMating) {
            matingProgress += 0.016 / MATING_DURATION;
            if (matingProgress >= 1.0) {
                // Animación completada, crear la cría (solo si este animal tiene ID menor para evitar duplicado)
                if (matingPartner != null && !offspringSpawned && this.animalId < matingPartner.animalId) {
                    spawnOffspring(matingPartner);
                    offspringSpawned = true;
                    // Marcar el partner para que no lo intente también
                    matingPartner.offspringSpawned = true;
                }
                isMating = false;
                matingProgress = 0.0;
                matingPartner = null;
            }
        }
    }

    protected void updateMovement() {
        if (worldRef == null) return;
        
        // No mover si está seleccionado
        if (isSelected) return;
        
        // Inicializar movimiento una sola vez después de spawn
        if (!movementInitialized && !isSpawning) {
            Random r = new Random(seed + System.nanoTime());
            double angle = r.nextDouble() * Math.PI * 2;
            double speed = baseSpeed * getPhaseSpeedMultiplier();
            velocity = new Vector3(Math.cos(angle) * speed, 0, Math.sin(angle) * speed);
            yaw = Math.atan2(velocity.z, velocity.x);
            movementInitialized = true;
        }
        
        if (!movementInitialized) return; // No mover durante spawn
        
        wanderTimer += 0.016;
        if (wanderTimer > WANDER_CHANGE_INTERVAL) {
            wanderTimer = 0.0;
            Random r = new Random(seed + System.nanoTime());
            double angle = r.nextDouble() * Math.PI * 2;
            double speed = baseSpeed * getPhaseSpeedMultiplier();
            velocity = new Vector3(Math.cos(angle) * speed, 0, Math.sin(angle) * speed);
            yaw = Math.atan2(velocity.z, velocity.x);
        }

        Vector3 oldPos = posicion;
        
        // Actualizar posición X y Z
        double newX = posicion.x + velocity.x * 0.5;
        double newZ = posicion.z + velocity.z * 0.5;
        
        // Obtener la altura del terreno en la nueva posición
        double terrainHeight = worldRef.getHeightAt(newX, newZ);
        
        // Ajustar Y para que el animal esté sobre el terreno
        // Añadir un offset que se escala con la fase del animal para compensar patas con Y negativa
        double baseOffset = voxelSize * 1.5; // Elevación base para compensar patas con Y negativa
        double phaseScale = getPhaseScaleMultiplier();
        double groundOffset = baseOffset * phaseScale; // El offset crece proporcionalmente con el animal
        double newY = terrainHeight != Double.NEGATIVE_INFINITY ? terrainHeight + groundOffset : posicion.y;
        
        posicion = new Vector3(newX, newY, newZ);
        
        // Keep inside world bounds
        if (Math.abs(posicion.x) > WORLD_BOUND || Math.abs(posicion.z) > WORLD_BOUND) {
            posicion = oldPos;
            // Invertir velocidad y yaw
            velocity = new Vector3(-velocity.x, 0, -velocity.z);
            yaw = Math.atan2(velocity.z, velocity.x);
            return;
        }

        // Collisions
        java.util.List<entities.Collidable> collidables = worldRef.getCollidables();
        for (entities.Collidable c : collidables) {
            if (c == this) continue;
            if (c instanceof entities.Pasto) continue; // ignore grass
            if (intersects(c)) {
                // Check for reproduction if both are animals of the same species
                if (c instanceof BaseAnimal) {
                    BaseAnimal other = (BaseAnimal) c;
                    // Only reproduce if:
                    // 1. Both are the same species
                    // 2. Both are adults (phase 3)
                    // 3. This animal's cooldown has expired
                    // 4. Neither is currently mating
                    boolean sameSpecies = this.getSpeciesType() == other.getSpeciesType();
                    boolean cooldownOk = this.reproductionCooldown <= 0 && other.reproductionCooldown <= 0;
                    boolean notMating = !this.isMating && !other.isMating;
                    
                    if (sameSpecies && cooldownOk && notMating) {
                        // Iniciar animación de apareamiento
                        startMatingAnimation(other);
                    }
                }
                
                posicion = oldPos;
                // Girar 90 grados
                double angle = Math.atan2(velocity.z, velocity.x) - Math.PI / 2;
                double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                velocity = new Vector3(Math.cos(angle) * speed, 0, Math.sin(angle) * speed);
                yaw = Math.atan2(velocity.z, velocity.x);
                break;
            }
        }
    }
    
    /**
     * Inicia la animación de apareamiento entre dos animales.
     */
    private void startMatingAnimation(BaseAnimal partner) {
        // Set cooldown for both parents
        this.reproductionCooldown = REPRODUCTION_COOLDOWN_DURATION;
        partner.reproductionCooldown = REPRODUCTION_COOLDOWN_DURATION;
        
        // Iniciar animación para ambos
        this.isMating = true;
        this.matingProgress = 0.0;
        this.matingPartner = partner;
        
        partner.isMating = true;
        partner.matingProgress = 0.0;
        partner.matingPartner = this;
        
        // Separar ligeramente a los animales para evitar que se queden atrapados
        Vector3 directionFromPartner = new Vector3(
            this.posicion.x - partner.posicion.x,
            0,
            this.posicion.z - partner.posicion.z
        );
        double distance = Math.sqrt(directionFromPartner.x * directionFromPartner.x + directionFromPartner.z * directionFromPartner.z);
        if (distance > 0.1) {
            directionFromPartner = new Vector3(
                directionFromPartner.x / distance,
                0,
                directionFromPartner.z / distance
            );
            double separationDistance = voxelSize * 3.0;
            this.posicion = new Vector3(
                this.posicion.x + directionFromPartner.x * separationDistance,
                this.posicion.y,
                this.posicion.z + directionFromPartner.z * separationDistance
            );
            
            Vector3 directionFromThis = new Vector3(-directionFromPartner.x, 0, -directionFromPartner.z);
            partner.posicion = new Vector3(
                partner.posicion.x + directionFromThis.x * separationDistance,
                partner.posicion.y,
                partner.posicion.z + directionFromThis.z * separationDistance
            );
        }
    }
    
    /**
     * Crea una nueva cría después de la animación de apareamiento.
     */
    private void spawnOffspring(BaseAnimal partner) {
        if (worldRef == null) return;
        
        // Calculate spawn position between the two parents
        double childX = (this.posicion.x + partner.posicion.x) / 2.0;
        double childZ = (this.posicion.z + partner.posicion.z) / 2.0;
        
        // Get terrain height at child position
        double terrainHeight = worldRef.getHeightAt(childX, childZ);
        if (terrainHeight == Double.NEGATIVE_INFINITY) terrainHeight = 0.0;
        
        double baseOffset = voxelSize * 1.5;
        double childY = terrainHeight + baseOffset; // Phase 1 uses scale 1.0
        
        Vector3 childPos = new Vector3(childX, childY, childZ);
        long childSeed = System.currentTimeMillis() + this.seed + partner.seed;
        
        // Create the new offspring using the static factory method
        try {
            int speciesType = this.getSpeciesType();
            main.Renderable offspring = main.EcosistemaApp.createAnimalOfType(speciesType, childPos, childSeed);
            
            if (offspring != null) {
                // Los animales heredan de BaseAnimal que implementa Renderable
                if (offspring instanceof BaseAnimal) {
                    BaseAnimal babyAnimal = (BaseAnimal) offspring;
                    // El bebé tiene un cooldown de reproducción para evitar reproducirse inmediatamente
                    babyAnimal.reproductionCooldown = REPRODUCTION_COOLDOWN_DURATION * 2; // El bebé no puede reproducirse por 20 segundos
                    worldRef.addEntity(offspring);
                    System.out.println("¡REPRODUCCIÓN EXITOSA! Animal " + this.animalId + " y " + partner.animalId + 
                                     " tuvieron una cría (Especie " + speciesType + ")");
                } else {
                    System.err.println("El offspring no es un BaseAnimal");
                }
            } else {
                System.err.println("offspring es null");
            }
        } catch (Exception e) {
            System.err.println("Error en reproducción: " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected boolean intersects(entities.Collidable other) {
        Vector3 minA = getAABBMin();
        Vector3 maxA = getAABBMax();
        Vector3 minB = other.getAABBMin();
        Vector3 maxB = other.getAABBMax();
        return (minA.x <= maxB.x && maxA.x >= minB.x &&
                minA.y <= maxB.y && maxA.y >= minB.y &&
                minA.z <= maxB.z && maxA.z >= minB.z);
    }
    
    public Vector3 getPosicion() { return posicion; }
    
    public void setHovered(boolean hovered) { this.isHovered = hovered; }
    public boolean isHovered() { return isHovered; }
    
    public void setSelected(boolean selected) { this.isSelected = selected; }
    public boolean isSelected() { return isSelected; }
    
    public double getHoverGlow() { return hoverGlow; }
    public double getSelectionScale() { return selectionScale; }
    
    public int getAnimalId() { return animalId; }
    
    public int getGrowthPhase() { return growthPhase; }
    public double getPhaseTimer() { 
        if (phaseStartTime == 0) return 0.0;
        long currentTime = System.currentTimeMillis();
        return (currentTime - phaseStartTime) / 1000.0;
    }
    public String getSpeciesName() { return getClass().getSimpleName().replace("AnimalType", "Especie "); }
    public double getBaseSpeed() { return baseSpeed; }
    public long getSpawnTime() { return spawnTime; }
    public double getTimeSinceSpawn() {
        return (System.currentTimeMillis() - spawnTime) / 1000.0; // Convert to seconds
    }
    public Color getOriginalColor() { return originalColor; }
    
    // Public getter for phase duration (delegates to protected method)
    public double getPhaseDurationPublic() {
        return getPhaseDuration(growthPhase);
    }
    
    // Calculate time remaining until next phase evolution
    public double getTimeToNextPhase() {
        if (growthPhase >= 3) return 0.0;
        if (phaseStartTime == 0) return getPhaseDuration(growthPhase);
        
        long currentTime = System.currentTimeMillis();
        double elapsedSeconds = (currentTime - phaseStartTime) / 1000.0;
        double remainingSeconds = getPhaseDuration(growthPhase) - elapsedSeconds;
        return Math.max(0.0, remainingSeconds);
    }
    
    // Advance to next growth phase (if possible)
    public void advanceToNextPhase() {
        if (growthPhase < 3) {
            growthPhase++;
            phaseStartTime = System.currentTimeMillis();
            transitionPulse = 1.0;
            applyPhaseVisuals();
            // Reajustar altura al terreno con el nuevo tamaño
            if (worldRef != null) {
                double terrainHeight = worldRef.getHeightAt(posicion.x, posicion.z);
                if (terrainHeight != Double.NEGATIVE_INFINITY) {
                    double baseOffset = voxelSize * 1.5;
                    double phaseScale = getPhaseScaleMultiplier();
                    double groundOffset = baseOffset * phaseScale;
                    posicion = new Vector3(posicion.x, terrainHeight + groundOffset, posicion.z);
                }
            }
        }
    }
    
    // Revert to previous growth phase (if possible)
    public void revertToPreviousPhase() {
        if (growthPhase > 1) {
            growthPhase--;
            phaseStartTime = System.currentTimeMillis();
            transitionPulse = 1.0;
            applyPhaseVisuals();
            // Reajustar altura al terreno con el nuevo tamaño
            if (worldRef != null) {
                double terrainHeight = worldRef.getHeightAt(posicion.x, posicion.z);
                if (terrainHeight != Double.NEGATIVE_INFINITY) {
                    double baseOffset = voxelSize * 1.5;
                    double phaseScale = getPhaseScaleMultiplier();
                    double groundOffset = baseOffset * phaseScale;
                    posicion = new Vector3(posicion.x, terrainHeight + groundOffset, posicion.z);
                }
            }
        }
    }
    
    // Get AABB for collision detection and selection
    public Vector3 getAABBMin() {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        for (Vector3 v : voxels) {
            minX = Math.min(minX, posicion.x + v.x * voxelSize - voxelSize/2.0);
            minY = Math.min(minY, posicion.y + v.y * voxelSize - voxelSize/2.0);
            minZ = Math.min(minZ, posicion.z + v.z * voxelSize - voxelSize/2.0);
        }
        return new Vector3(minX, minY, minZ);
    }
    
    public Vector3 getAABBMax() {
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (Vector3 v : voxels) {
            maxX = Math.max(maxX, posicion.x + v.x * voxelSize + voxelSize/2.0);
            maxY = Math.max(maxY, posicion.y + v.y * voxelSize + voxelSize/2.0);
            maxZ = Math.max(maxZ, posicion.z + v.z * voxelSize + voxelSize/2.0);
        }
        return new Vector3(maxX, maxY, maxZ);
    }
    
    // Death system methods
    public void markForDeath() {
        if (!isDying) {
            isDying = true;
            deathProgress = 0.0;
            initializeDeathAnimation();
        }
    }
    
    private void initializeDeathAnimation() {
        Random r = new Random(seed + System.currentTimeMillis());
        deathParticles.clear();
        
        // Create particles from voxels
        for (Vector3 v : voxels) {
            Vector3 worldPos = new Vector3(
                posicion.x + v.x * voxelSize,
                posicion.y + v.y * voxelSize,
                posicion.z + v.z * voxelSize
            );
            
            // Multiple particles per voxel
            for (int i = 0; i < 3; i++) {
                Vector3 vel = new Vector3(
                    (r.nextDouble() - 0.5) * 4.0,
                    r.nextDouble() * 3.0 + 2.0,
                    (r.nextDouble() - 0.5) * 4.0
                );
                
                // Death colors: dark red, gray, black
                Color particleColor;
                int colorChoice = r.nextInt(3);
                if (colorChoice == 0) {
                    particleColor = new Color(80 + r.nextInt(40), 20, 20); // Dark red
                } else if (colorChoice == 1) {
                    particleColor = new Color(60 + r.nextInt(30), 60 + r.nextInt(30), 60 + r.nextInt(30)); // Gray
                } else {
                    particleColor = new Color(30 + r.nextInt(20), 30 + r.nextInt(20), 30 + r.nextInt(20)); // Dark gray
                }
                
                deathParticles.add(new DeathParticle(worldPos, vel, particleColor));
            }
        }
    }
    
    public boolean isMarkedForDeath() {
        return markedForDeath;
    }
    
    public boolean isDying() {
        return isDying;
    }
    
    protected Vector3 rotateVoxel(Vector3 v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vector3(
            v.x * cos - v.z * sin,
            v.y,
            v.x * sin + v.z * cos
        );
    }
}
