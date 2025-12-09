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
 * Clase base para todos los animales con animación de spawn compartida.
 */
public abstract class BaseAnimal implements Renderable {
    protected Vector3 posicion;
    protected List<Vector3> voxels;
    protected int voxelSize;
    protected Color color;
    protected long seed;
    
    // Selection and hover states
    private boolean isHovered = false;
    private boolean isSelected = false;
    private double hoverGlow = 0.0;
    private double selectionScale = 1.0;
    private static int nextAnimalId = 1;
    private int animalId;
    
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
    
    @Override
    public void update() {
        if (isSpawning) {
            spawnProgress += 0.016 / SPAWN_DURATION;
            if (spawnProgress >= 1.0) {
                spawnProgress = 1.0;
                isSpawning = false;
                spawnParticles.clear();
            }
        }
        
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
        if (isSpawning) {
            renderSpawnAnimation(renderer, cam);
        } else {
            renderNormal(renderer, cam);
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
    
    // Método abstracto que cada tipo de animal debe implementar
    protected abstract void renderNormal(SoftwareRenderer renderer, Camera cam);
    
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
    
    public Vector3 getPosicion() { return posicion; }
    
    public void setHovered(boolean hovered) { this.isHovered = hovered; }
    public boolean isHovered() { return isHovered; }
    
    public void setSelected(boolean selected) { this.isSelected = selected; }
    public boolean isSelected() { return isSelected; }
    
    public double getHoverGlow() { return hoverGlow; }
    public double getSelectionScale() { return selectionScale; }
    
    public int getAnimalId() { return animalId; }
    
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
}
