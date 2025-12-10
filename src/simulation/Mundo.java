package simulation;

import java.util.ArrayList;
import java.util.List;
import main.Renderable;
import entities.Animal;

/**
 * Mundo: contenedor de entidades y animales. Provee API para añadir/quitar
 * y acceder a la lista que usa el render thread.
 */
public class Mundo {
    private final List<Renderable> entidades = new ArrayList<>();
    private final List<Animal> animales = new ArrayList<>();
    private Animal selectedAnimal = null;
    private boolean waitingForSpawnPosition = false;
    private int selectedAnimalType = -1; // tipo de animal a generar (-1=random, 0-9=tipo específico)
    
    // Environmental growth system
    private long lastGrowthUpdate = 0;
    private static final long GROWTH_UPDATE_INTERVAL = 1000L; // Update every 10 seconds
    private static final long NEW_ENTITY_SPAWN_INTERVAL = 5000L; // Spawn new entities every 5 seconds

    public Mundo(){ }

    public synchronized void addEntity(Renderable e){ entidades.add(e); }
    public synchronized void removeEntity(Renderable e){ entidades.remove(e); }
    // Return a defensive copy to avoid concurrent modification during iteration
    public synchronized List<Renderable> getEntities(){ return new ArrayList<>(entidades); }
    // Explicit snapshot helper used by the renderer to iterate safely across threads
    public synchronized List<Renderable> snapshotEntities(){ return new ArrayList<>(entidades); }

    public synchronized void addAnimal(Animal a){ animales.add(a); entidades.add(a); }
    public synchronized void removeAnimal(Animal a){ animales.remove(a); entidades.remove(a); }
    public synchronized List<Animal> getAnimals(){ return new ArrayList<>(animales); }

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
    public synchronized void setSelectedAnimal(Animal a){ this.selectedAnimal = a; }
    public synchronized Animal getSelectedAnimal(){ return selectedAnimal; }
    
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
    }
}
