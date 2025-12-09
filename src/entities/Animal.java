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
public class Animal implements Renderable {
    private Vector3 posicion;
    private List<Vector3> voxels; // offsets in voxel units
    private int voxelSize; // pixel size for each cube
    private Color color;
    private double rotY = 0.0;
    private double bob = 0.0;
    private double speed = 1.0; // affects animation speed
    private long seed;
    
    // Spawn animation
    private boolean isSpawning = true;
    private double spawnProgress = 0.0; // 0.0 to 1.0
    private static final double SPAWN_DURATION = 3.0; // seconds
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

    public Animal(Vector3 posicion, long seed){
        this.posicion = posicion;
        this.seed = seed;
        this.voxels = new ArrayList<>();
        generateFromSeed(seed);
        initializeSpawnAnimation();
        System.out.println("Animal creado - spawn animation iniciada. Particles: " + spawnParticles.size());
    }

    // Additional constructor used for deserialization
    public Animal(Vector3 posicion, long seed, Color color, int voxelSize, List<Vector3> voxels){
        this.posicion = posicion;
        this.seed = seed;
        this.color = color;
        this.voxelSize = voxelSize;
        this.voxels = new ArrayList<>(voxels);
        this.isSpawning = false; // Already spawned
        this.spawnProgress = 1.0;
    }

    private void generateFromSeed(long seed){
        Random r = new Random(seed);
        this.voxelSize = 18 + r.nextInt(16); // 18..33 pixels per cube
        // choose base color
        int rcol = 80 + r.nextInt(176);
        int gcol = 80 + r.nextInt(176);
        int bcol = 80 + r.nextInt(176);
        this.color = new Color(rcol, gcol, bcol);
        this.speed = 0.5 + r.nextDouble()*2.0;

        // Generate a small connected voxel blob. We'll create a 2D footprint
        // of size w x h and randomly populate it, ensuring connectivity.
        int w = 1 + r.nextInt(3); // 1..3
        int h = 1 + r.nextInt(3);
        boolean[][] grid = new boolean[w][h];
        int attempts = 0;
        // seed center
        grid[w/2][h/2] = true;
        int placed = 1;
        int target = 3 + r.nextInt(6); // 3..8 voxels
        while(placed < target && attempts < 50){
            int x = r.nextInt(w);
            int y = r.nextInt(h);
            if(grid[x][y]) { attempts++; continue; }
            // ensure neighbor exists to keep connected
            boolean neigh = false;
            if(x>0 && grid[x-1][y]) neigh = true;
            if(x<w-1 && grid[x+1][y]) neigh = true;
            if(y>0 && grid[x][y-1]) neigh = true;
            if(y<h-1 && grid[x][y+1]) neigh = true;
            if(neigh){ grid[x][y] = true; placed++; }
            attempts++;
        }

        // convert grid to voxel offsets (centered)
        for(int x=0;x<w;x++){
            for(int y=0;y<h;y++){
                if(grid[x][y]){
                    double ox = (x - w/2.0);
                    double oy = 0; // same base level for footprint
                    double oz = (y - h/2.0);
                    voxels.add(new Vector3(ox, oy, oz));
                }
            }
        }

        // randomly add a few elevated voxels to create legs/neck
        if(r.nextDouble() < 0.6){
            if(!voxels.isEmpty()){
                int idx = r.nextInt(voxels.size());
                Vector3 base = voxels.get(idx);
                voxels.add(new Vector3(base.x, 1.0, base.z));
            }
        }
    }
    
    private void initializeSpawnAnimation() {
        Random r = new Random(seed + 999);
        // Crear partículas en espiral para cada voxel
        for (int i = 0; i < voxels.size(); i++) {
            int particlesPerVoxel = 5; // Más partículas
            for (int p = 0; p < particlesPerVoxel; p++) {
                double angle = r.nextDouble() * Math.PI * 2;
                double height = -50 - r.nextDouble() * 30; // Start much lower
                double radius = 20 + r.nextDouble() * 25; // Wider spiral
                double speed = 0.8 + r.nextDouble() * 0.4;
                spawnParticles.add(new SpawnParticle(angle, height, radius, speed, i));
            }
        }
    }

    // Simple mutation: tweak color and size slightly using a new seed
    public void mutate(long newSeed){
        Random r = new Random(newSeed);
        int dr = r.nextInt(41)-20;
        int dg = r.nextInt(41)-20;
        int db = r.nextInt(41)-20;
        int nr = Math.max(10, Math.min(255, color.getRed() + dr));
        int ng = Math.max(10, Math.min(255, color.getGreen() + dg));
        int nb = Math.max(10, Math.min(255, color.getBlue() + db));
        color = new Color(nr, ng, nb);
        // size mutation
        int ds = r.nextInt(7)-3;
        voxelSize = Math.max(8, voxelSize + ds);
    }

    // Provide AABB covering all voxels (world-space)
    public math.Vector3 getAABBMin(){
        if(voxels.isEmpty()) return new math.Vector3(posicion.x, posicion.y, posicion.z);
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        for(Vector3 off : voxels){
            double wx = posicion.x + off.x * voxelSize;
            double wy = posicion.y + off.y * voxelSize;
            double wz = posicion.z + off.z * voxelSize;
            minX = Math.min(minX, wx - voxelSize/2.0);
            minY = Math.min(minY, wy - voxelSize/2.0);
            minZ = Math.min(minZ, wz - voxelSize/2.0);
        }
        return new math.Vector3(minX, minY, minZ);
    }

    public math.Vector3 getAABBMax(){
        if(voxels.isEmpty()) return new math.Vector3(posicion.x, posicion.y, posicion.z);
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for(Vector3 off : voxels){
            double wx = posicion.x + off.x * voxelSize;
            double wy = posicion.y + off.y * voxelSize;
            double wz = posicion.z + off.z * voxelSize;
            maxX = Math.max(maxX, wx + voxelSize/2.0);
            maxY = Math.max(maxY, wy + voxelSize/2.0);
            maxZ = Math.max(maxZ, wz + voxelSize/2.0);
        }
        return new math.Vector3(maxX, maxY, maxZ);
    }

    // Simple serialization format (line-based):
    // seed|x,y,z|r,g,b|voxelSize|ox:oy:oz,ox:oy:oz,...
    public String serialize(){
        StringBuilder sb = new StringBuilder();
        sb.append(seed).append('|');
        sb.append((int)posicion.x).append(',').append((int)posicion.y).append(',').append((int)posicion.z).append('|');
        sb.append(color.getRed()).append(',').append(color.getGreen()).append(',').append(color.getBlue()).append('|');
        sb.append(voxelSize).append('|');
        for(int i=0;i<voxels.size();i++){
            Vector3 v = voxels.get(i);
            sb.append((int)v.x).append(':').append((int)v.y).append(':').append((int)v.z);
            if(i<voxels.size()-1) sb.append(',');
        }
        return sb.toString();
    }

    // Parse the simple serialization format and return an Animal.
    public static Animal deserialize(String line){
        try{
            String[] parts = line.split("\\|");
            if(parts.length < 5) return null;
            long seed = Long.parseLong(parts[0]);
            String[] posParts = parts[1].split(",");
            Vector3 pos = new Vector3(Double.parseDouble(posParts[0]), Double.parseDouble(posParts[1]), Double.parseDouble(posParts[2]));
            String[] colParts = parts[2].split(",");
            Color col = new Color(Integer.parseInt(colParts[0]), Integer.parseInt(colParts[1]), Integer.parseInt(colParts[2]));
            int vsize = Integer.parseInt(parts[3]);
            List<Vector3> vox = new ArrayList<>();
            if(parts[4].length() > 0){
                String[] offs = parts[4].split(",");
                for(String o : offs){
                    String[] xyz = o.split(":");
                    vox.add(new Vector3(Double.parseDouble(xyz[0]), Double.parseDouble(xyz[1]), Double.parseDouble(xyz[2])));
                }
            }
            return new Animal(pos, seed, col, vsize, vox);
        }catch(Exception ex){
            return null;
        }
    }

    @Override
    public void update(){
        if (isSpawning) {
            spawnProgress += 0.016 / SPAWN_DURATION; // ~60 FPS
            if (spawnProgress >= 1.0) {
                spawnProgress = 1.0;
                isSpawning = false;
                spawnParticles.clear();
                System.out.println("Spawn animation completada!");
            }
        }
    }

    @Override
    public void render(SoftwareRenderer renderer, Camera cam){
        if (isSpawning) {
            renderSpawnAnimation(renderer, cam);
        } else {
            // Render normal - each voxel as a cube
            for(Vector3 off : voxels){
                Vector3 world = new Vector3(
                    posicion.x + off.x * voxelSize,
                    posicion.y + off.y * voxelSize,
                    posicion.z + off.z * voxelSize
                );
                Vector3[] verts = renderer.getCubeVertices(world, voxelSize, 0);
                renderer.drawCube(verts, cam, color);
            }
        }
    }
    
    private void renderSpawnAnimation(SoftwareRenderer renderer, Camera cam) {
        System.out.println("Rendering spawn animation - progress: " + spawnProgress);
        // Fase 1 (0.0 - 0.5): Partículas en espiral convergiendo
        if (spawnProgress < 0.5) {
            double particlePhase = spawnProgress / 0.5;
            for (SpawnParticle p : spawnParticles) {
                // Las partículas suben en espiral hacia su voxel objetivo
                double targetY = voxels.get(p.voxelIndex).y * voxelSize + posicion.y;
                double currentRadius = p.radius * (1.0 - particlePhase);
                double currentHeight = p.height + (targetY - p.height) * particlePhase;
                double spiralAngle = p.angle + particlePhase * Math.PI * 6 * p.speed;
                
                double px = posicion.x + Math.cos(spiralAngle) * currentRadius;
                double py = currentHeight;
                double pz = posicion.z + Math.sin(spiralAngle) * currentRadius;
                
                // Renderizar partícula como cubo brillante más grande
                int particleSize = 6 + (int)(4 * particlePhase);
                Vector3[] particleVerts = renderer.getCubeVertices(new Vector3(px, py, pz), particleSize, 0);
                
                // Color brillante que va hacia el color del animal
                Color particleColor = new Color(255, 255, 150);
                renderer.drawCube(particleVerts, cam, particleColor);
            }
        }
        
        // Fase 2 (0.4 - 1.0): Voxels aparecen con rotación y escala
        if (spawnProgress > 0.4) {
            double voxelPhase = Math.min(1.0, (spawnProgress - 0.4) / 0.6);
            
            for (int i = 0; i < voxels.size(); i++) {
                Vector3 off = voxels.get(i);
                
                // Cada voxel aparece secuencialmente
                double voxelDelay = (double)i / voxels.size() * 0.3;
                double individualProgress = Math.max(0, Math.min(1.0, (voxelPhase - voxelDelay) / 0.7));
                
                if (individualProgress > 0) {
                    // Escala desde 0 a tamaño completo con bounce
                    double scale = individualProgress < 0.8 
                        ? individualProgress / 0.8 
                        : 1.0 + Math.sin((individualProgress - 0.8) / 0.2 * Math.PI) * 0.3;
                    
                    // Rotación durante aparición
                    double spinRotation = (1.0 - individualProgress) * Math.PI * 4;
                    
                    Vector3 world = new Vector3(
                        posicion.x + off.x * voxelSize,
                        posicion.y + off.y * voxelSize + (1.0 - individualProgress) * 15, // Baja desde más arriba
                        posicion.z + off.z * voxelSize
                    );
                    
                    int scaledSize = Math.max(1, (int)(voxelSize * scale));
                    Vector3[] verts = renderer.getCubeVertices(world, scaledSize, spinRotation);
                    
                    // Color con destello
                    float glow = (float)(Math.max(0, 1.5 - individualProgress * 1.5));
                    Color voxelColor = new Color(
                        Math.min(1.0f, color.getRed() / 255f + glow * 0.5f),
                        Math.min(1.0f, color.getGreen() / 255f + glow * 0.5f),
                        Math.min(1.0f, color.getBlue() / 255f + glow * 0.5f)
                    );
                    renderer.drawCube(verts, cam, voxelColor);
                }
            }
            
            // Fase 3 (0.85 - 1.0): Destello final expansivo
            if (spawnProgress > 0.85) {
                double burstPhase = (spawnProgress - 0.85) / 0.15;
                int burstParticles = 16;
                
                for (int i = 0; i < burstParticles; i++) {
                    double angle = (i / (double)burstParticles) * Math.PI * 2;
                    double burstRadius = burstPhase * 30;
                    double burstHeight = posicion.y + voxelSize * 2 + burstPhase * 15;
                    
                    double bx = posicion.x + Math.cos(angle) * burstRadius;
                    double by = burstHeight;
                    double bz = posicion.z + Math.sin(angle) * burstRadius;
                    
                    int burstSize = Math.max(1, (int)(6 * (1.0 - burstPhase)));
                    Vector3[] burstVerts = renderer.getCubeVertices(new Vector3(bx, by, bz), burstSize, 0);
                    Color burstColor = new Color(255, 255, 100);
                    renderer.drawCube(burstVerts, cam, burstColor);
                }
            }
        }
    }

    public Vector3 getPosicion() { return posicion; }
}
