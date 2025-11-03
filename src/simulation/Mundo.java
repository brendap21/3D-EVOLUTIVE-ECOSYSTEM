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

    public Mundo(){ }

    public synchronized void addEntity(Renderable e){ entidades.add(e); }
    public synchronized void removeEntity(Renderable e){ entidades.remove(e); }
    public synchronized List<Renderable> getEntities(){ return entidades; }

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
}
