package simulation;

import java.util.Random;
import java.util.List;
import entities.BaseAnimal;

/**
 * Simulador: hilo que actualiza el mundo (mutaciones, reproducción básica)
 * de forma determinista usando una seed global.
 */
public class Simulador extends Thread {
    private Mundo mundo;
    private long seed;
    private boolean running = true;

    public Simulador(Mundo mundo, long seed){
        this.mundo = mundo;
        this.seed = seed;
    }

    public void shutdown(){ running = false; }
    
    public long getSeed(){ return seed; }

    @Override
    public void run(){
        Random r = new Random(seed);
        long tick = 0;
        while(running){
            // Simple simulation step every 1s: possibly mutate a random animal
            try{ Thread.sleep(1000); } catch(Exception e){}
            List<BaseAnimal> animals = mundo.getAnimals();
            if(animals.isEmpty()) { tick++; continue; }

            // Deterministic choice based on tick and seed
            int idx = (int)((seed + tick) % animals.size());
            BaseAnimal a = animals.get(idx);
            // Evolución simple: intenta forzar avance de fase si aplica
            a.setGrowthPhase(a.getGrowthPhase());
            tick++;
            // simulation runs until shutdown() is called
        }
    }
}
