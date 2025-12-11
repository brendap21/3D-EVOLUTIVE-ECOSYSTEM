package simulation;

import java.util.ArrayList;
import java.util.List;
import main.Renderable;
import entities.BaseAnimal;

/**
 * ============================================================================================
 * Mundo - Contenedor centralizado de todas las entidades del ecosistema
 * ============================================================================================
 * 
 * PROPÓSITO:
 * Gestiona la lista de todas las entidades (animales, terreno, vegetación) del mundo 3D.
 * Proporciona acceso thread-safe para que múltiples hilos puedan consultar/modificar.
 * 
 * RESPONSABILIDADES:
 * 1. Almacenar y gestionar entidades renderizables
 * 2. Proporcionar queries espaciales (heightmap, colisiones)
 * 3. Gestionar spawn y eliminación de entidades
 * 4. Mantener estado de selección de animales
 * 5. Garantizar reproducibilidad (seeds para mundo determinista)
 * 
 * CONCEPTOS IMPLEMENTADOS:
 * 1. THREAD-SAFETY:
 *    - Métodos synchronized para evitar race conditions
 *    - Copias defensivas (snapshots) para iteración segura
 *    - Evita ConcurrentModificationException
 * 
 * 2. SEPARACIÓN DE CONCERNS:
 *    - Lista general de Renderable (todo lo que se dibuja)
 *    - Lista específica de BaseAnimal (para lógica de simulación)
 *    - Permite tratar animales de forma especial sin casting constante
 * 
 * 3. SPATIAL QUERIES:
 *    - getHeightAt(x,z): Obtiene altura del terreno (heightmap query)
 *    - getCollidables(): Obtiene entidades con AABB (collision detection)
 * 
 * 4. DETERMINISMO:
 *    - environmentSeed: Seed para generación reproducible de vegetación
 *    - environmentCreatedAt: Timestamp para calcular edad de plantas
 *    - Permite guardar/cargar mundos idénticos
 * 
 * ============================================================================================
 */
public class Mundo {
    // LISTA PRINCIPAL DE ENTIDADES (todo lo que se renderiza)
    private final List<Renderable> entidades = new ArrayList<>();
    
    // LISTA ESPECÍFICA DE ANIMALES (para lógica de simulación)
    private final List<BaseAnimal> animales = new ArrayList<>();
    
    // ANIMAL SELECCIONADO (para mostrar panel de info)
    private BaseAnimal selectedAnimal = null;
    
    // ESTADO DE SPAWN (para menú de generación de animales)
    private boolean waitingForSpawnPosition = false;
    private int selectedAnimalType = -1; // -1=random, 0-9=tipo específico, 10=depredador
    
    // SISTEMA DE CRECIMIENTO AMBIENTAL
    private long lastGrowthUpdate = 0; // Último tick de actualización
    private static final long GROWTH_UPDATE_INTERVAL = 1000L; // Actualizar cada 1 segundo
    private static final long NEW_ENTITY_SPAWN_INTERVAL = 5000L; // Spawn nuevas plantas cada 5s
    
    // SEED PARA REPRODUCIBILIDAD DEL AMBIENTE
    // La misma seed genera exactamente el mismo mundo (árboles, rocas, pasto en mismas posiciones)
    private long environmentSeed = 12345L;
    
    // TIMESTAMP DE CREACIÓN (para calcular edad de plantas)
    private long environmentCreatedAt = 0L;

    public Mundo(){ }
    
    public long getEnvironmentSeed() { return environmentSeed; }
    public void setEnvironmentSeed(long seed) { this.environmentSeed = seed; }
    public long getEnvironmentCreatedAt() { return environmentCreatedAt; }
    public void setEnvironmentCreatedAt(long createdAt) { this.environmentCreatedAt = createdAt; }

    public synchronized void addEntity(Renderable e){ entidades.add(e); }
    public synchronized void removeEntity(Renderable e){ entidades.remove(e); }
    // Return a defensive copy to avoid concurrent modification during iteration
    public synchronized List<Renderable> getEntities(){ return new ArrayList<>(entidades); }
    // Explicit snapshot helper used by the renderer to iterate safely across threads
    public synchronized List<Renderable> snapshotEntities(){ return new ArrayList<>(entidades); }

    public synchronized void addAnimal(BaseAnimal a){ animales.add(a); entidades.add(a); }
    public synchronized void removeAnimal(BaseAnimal a){ animales.remove(a); entidades.remove(a); }
    public synchronized List<BaseAnimal> getAnimals(){ return new ArrayList<>(animales); }

    // Query the terrain height at world coordinates. If multiple height-providers
    // exist, return the maximum height among them (so objects rest on top).
    public synchronized double getHeightAt(double x, double z){
        double maxH = Double.NEGATIVE_INFINITY;
        for(Renderable r : entidades){
            if(r instanceof entities.HeightProvider){
                entities.HeightProvider s = (entities.HeightProvider)r;
                double h = s.getHeightAt(x, z);
                if(h > maxH) maxH = h;
            }
        }
        if(maxH == Double.NEGATIVE_INFINITY) return Double.NEGATIVE_INFINITY;
        return maxH;
    }

    // Return list of entities that expose AABB (Collidable)
    public synchronized java.util.List<entities.Collidable> getCollidables(){
        java.util.List<entities.Collidable> out = new java.util.ArrayList<>();
        for(Renderable r : entidades){
            if(r instanceof entities.Collidable) out.add((entities.Collidable)r);
        }
        return out;
    }

    // Animal selection
    public synchronized void setSelectedAnimal(BaseAnimal a){ this.selectedAnimal = a; }
    public synchronized BaseAnimal getSelectedAnimal(){ return selectedAnimal; }
    
    public synchronized void setWaitingForSpawn(boolean waiting){ this.waitingForSpawnPosition = waiting; }
    public synchronized boolean isWaitingForSpawn(){ return waitingForSpawnPosition; }
    
    // Animal type for spawning
    public synchronized void setSelectedAnimalType(int type){ this.selectedAnimalType = type; }
    public synchronized int getSelectedAnimalType(){ return selectedAnimalType; }
    
    // Update environmental entities (trees growing, new plants spawning)
    public synchronized void updateEnvironment() {
        long currentTime = System.currentTimeMillis();
        
        // Update all entities
        for (Renderable r : entidades) {
            r.update();
        }
        
        // Remove dead animals and depredadores
        List<Renderable> toRemove = new ArrayList<>();
        for (Renderable r : entidades) {
            if (r instanceof entities.BaseAnimal) {
                entities.BaseAnimal animal = (entities.BaseAnimal) r;
                if (animal.isMarkedForDeath()) {
                    toRemove.add(r);
                    if (selectedAnimal == r) {
                        selectedAnimal = null; // Deselect if was selected
                    }
                }
            } else if (r instanceof entities.Depredador) {
                entities.Depredador dep = (entities.Depredador) r;
                if (dep.isMarkedForDeath()) {
                    toRemove.add(r);
                }
            }
        }
        
        // Remove marked entities
        for (Renderable r : toRemove) {
            removeEntity(r);
            if (r instanceof BaseAnimal) {
                animales.remove(r);
            }
        }
    }
    
    // Limpiar completamente el mundo (para cargar nueva partida)
    public synchronized void clearWorld() {
        entidades.clear();
        animales.clear();
        selectedAnimal = null;
        waitingForSpawnPosition = false;
        selectedAnimalType = -1;
        environmentCreatedAt = 0L;
    }
    
    // Inicializar entorno con terreno y entidades ambientales usando la seed guardada
    public synchronized void initializeEnvironment() {
        // Crear terreno con seed fija
        entities.Terreno terreno = new entities.Terreno(160, 160, 8.0, 12345L, new java.awt.Color(60, 140, 60));
        addEntity(terreno);
        
        if (environmentCreatedAt == 0L) {
            environmentCreatedAt = System.currentTimeMillis();
        }

        // Recrear entidades ambientales usando la seed del mundo (determinista)
        java.util.Random r = new java.util.Random(environmentSeed);
        java.util.List<math.Vector3> usedPositions = new java.util.ArrayList<>();
        
        // Árboles (7-9)
        int numTrees = 7 + r.nextInt(3);
        spawnEnvironmentalEntities(r, numTrees, 180, usedPositions, "tree");
        
        // Rocas (5-8)
        int numRocks = 5 + r.nextInt(4);
        spawnEnvironmentalEntities(r, numRocks, 90, usedPositions, "rock");
        
        // Pasto (80-120)
        int numGrass = 80 + r.nextInt(41);
        spawnEnvironmentalEntities(r, numGrass, 15, usedPositions, "grass");
        
        // Arbustos (4-6)
        int numBushes = 4 + r.nextInt(3);
        spawnEnvironmentalEntities(r, numBushes, 85, usedPositions, "bush");
        
        // Flores (40-60)
        int numFlowers = 40 + r.nextInt(21);
        spawnEnvironmentalEntities(r, numFlowers, 20, usedPositions, "flower");

        applyEnvironmentCreationTime(environmentCreatedAt);
    }

    private void applyEnvironmentCreationTime(long createdAt) {
        for (Renderable r : entidades) {
            if (r instanceof entities.Arbol) {
                ((entities.Arbol) r).setCreationTime(createdAt);
            } else if (r instanceof entities.Pasto) {
                ((entities.Pasto) r).setCreationTime(createdAt);
            } else if (r instanceof entities.Flor) {
                ((entities.Flor) r).setCreationTime(createdAt);
            }
        }
    }
    
    private void spawnEnvironmentalEntities(java.util.Random r, int count, double minDistance, 
                                           java.util.List<math.Vector3> usedPositions, String type) {
        int spawned = 0;
        int attempts = 0;
        int maxAttempts = count * 15;
        
        java.awt.Color[] flowerColors = {
            new java.awt.Color(255, 20, 147),
            new java.awt.Color(255, 105, 180),
            new java.awt.Color(255, 165, 0),
            new java.awt.Color(255, 215, 0),
            new java.awt.Color(144, 238, 144),
            new java.awt.Color(0, 206, 209),
            new java.awt.Color(186, 85, 211),
            new java.awt.Color(255, 20, 147)
        };
        
        while (spawned < count && attempts < maxAttempts) {
            double x = -320 + r.nextDouble() * 640;
            double z = -320 + r.nextDouble() * 640;
            math.Vector3 pos = new math.Vector3(x, 0, z);
            
            boolean tooClose = false;
            for (math.Vector3 used : usedPositions) {
                double dist = Math.sqrt((pos.x - used.x) * (pos.x - used.x) + 
                                       (pos.z - used.z) * (pos.z - used.z));
                if (dist < minDistance) {
                    tooClose = true;
                    break;
                }
            }
            
            if (!tooClose) {
                usedPositions.add(pos);
                long seed = System.currentTimeMillis() + spawned * 100 + r.nextLong();
                
                switch (type) {
                    case "tree":
                        entities.Arbol arbol = new entities.Arbol(pos, 12 + r.nextInt(8), 60 + r.nextInt(30), 28 + r.nextInt(15), seed);
                        addEntity(arbol);
                        break;
                    case "rock":
                        entities.Piedra piedra = new entities.Piedra(pos, seed);
                        addEntity(piedra);
                        break;
                    case "grass":
                        entities.Pasto pasto = new entities.Pasto(pos, seed);
                        addEntity(pasto);
                        break;
                    case "bush":
                        entities.Arbusto arbusto = new entities.Arbusto(pos);
                        addEntity(arbusto);
                        break;
                    case "flower":
                        java.awt.Color flowerColor = flowerColors[r.nextInt(flowerColors.length)];
                        entities.Flor flor = new entities.Flor(pos, flowerColor, seed);
                        addEntity(flor);
                        break;
                }
                spawned++;
            }
            attempts++;
        }
    }
}
