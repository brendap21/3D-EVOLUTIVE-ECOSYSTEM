package simulation;

import java.util.Random;
import java.util.List;
import entities.BaseAnimal;
import ui.Controles;

/**
 * ============================================================================================
 * Simulador - Hilo de simulación lógica del ecosistema
 * ============================================================================================
 * 
 * PROPÓSITO:
 * Ejecuta la lógica de evolución y mutación de los animales en un hilo separado.
 * Actualiza el estado del mundo cada 1 segundo de manera DETERMINISTA.
 * 
 * RESPONSABILIDADES:
 * 1. Evolucionar animales (cambios de fase de crecimiento)
 * 2. Aplicar mutaciones aleatorias (color, tamaño, velocidad)
 * 3. Gestionar muerte por hambre/edad
 * 4. Respetar el sistema de PAUSA ABSOLUTA
 * 
 * CONCEPTOS IMPLEMENTADOS:
 * 1. DETERMINISMO:
 *    - Usa seed fija para generar secuencia reproducible de eventos
 *    - Misma seed = misma secuencia de mutaciones
 *    - Útil para debugging y para animación de 3 minutos sin repetición
 *    - Random(seed) + tick counter = comportamiento predecible
 * 
 * 2. MULTI-THREADING:
 *    - Hilo separado del render (no bloquea dibujado)
 *    - Sleep de 1 segundo (frecuencia de simulación más lenta que render)
 *    - Permite evolución en "tiempo real" mientras el juego corre
 * 
 * 3. PAUSA ABSOLUTA:
 *    - Verifica controles.isPaused() y controles.isAnimalPanelOpen()
 *    - Si está pausado, continua al siguiente sleep SIN modificar nada
 *    - Garantiza congelamiento total (ni evolución, ni mutaciones, ni muerte)
 * 
 * 4. SELECCIÓN DETERMINISTA:
 *    - índice = (seed + tick) % cantidad_animales
 *    - Cada tick evoluciona un animal diferente
 *    - Secuencia predecible y uniforme (todos evolucionan eventualmente)
 * 
 * ============================================================================================
 */
public class Simulador extends Thread {
    private Mundo mundo;              // Referencia al mundo (lista de animales)
    private long seed;                // Seed para determinismo
    private boolean running = true;   // Flag para detener el hilo limpiamente
    private Controles controles;      // Referencia para verificar estado de pausa

    /**
     * Constructor: Inicializa el simulador con mundo y seed.
     * 
     * @param mundo Mundo que contiene los animales a evolucionar
     * @param seed Seed para generación determinista de eventos
     */
    public Simulador(Mundo mundo, long seed){
        this.mundo = mundo;
        this.seed = seed;
    }
    
    /**
     * Configura la referencia a Controles (necesaria para respetar la pausa).
     * Debe llamarse ANTES de iniciar el hilo.
     * 
     * @param controles Sistema de controles con estado de pausa
     */
    public void setControles(Controles controles){
        this.controles = controles;
    }

    /**
     * Detiene el hilo limpiamente (para cerrar la aplicación).
     */
    public void shutdown(){ 
        running = false; 
    }
    
    /**
     * Obtiene la seed del simulador (útil para guardar/cargar partidas).
     * 
     * @return Seed actual del simulador
     */
    public long getSeed(){ 
        return seed; 
    }

    /**
     * ========================================================================================
     * run - Bucle principal de simulación (ejecuta cada 1 segundo)
     * ========================================================================================
     * 
     * FLUJO DEL BUCLE:
     * 1. Dormir 1 segundo (frecuencia de simulación)
     * 2. Verificar estado de pausa
     * 3. Si NO está pausado:
     *    a. Obtener lista de animales vivos
     *    b. Seleccionar un animal de manera determinista
     *    c. Aplicar evolución/mutación al animal seleccionado
     *    d. Incrementar contador de tick
     * 4. Repetir hasta que shutdown() sea llamado
     * 
     * DETERMINISMO:
     * - Random(seed) genera siempre la misma secuencia
     * - índice = (seed + tick) % cantidad asegura distribución uniforme
     * - Resultado: animación reproducible de 3+ minutos
     */
    @Override
    public void run(){
        // Generador aleatorio con seed fija (determinista)
        Random r = new Random(seed);
        
        // Contador de ticks (usado para selección determinista)
        long tick = 0;
        
        // Bucle principal (corre hasta que shutdown() lo detenga)
        while(running){
            // Dormir 1 segundo (frecuencia de simulación)
            try{ Thread.sleep(1000); } catch(Exception e){}
            
            // ==================================================================================
            // PAUSA ABSOLUTA: Verificar estado de pausa
            // ==================================================================================
            // Si el juego está pausado (ESC presionado) o si hay un panel de info abierto,
            // NO ejecutar ninguna lógica de simulación.
            // Esto congela COMPLETAMENTE la evolución:
            // - No hay mutaciones
            // - No hay cambios de fase
            // - No hay muerte por hambre
            // - No hay reproducción
            // El tick no avanza, por lo que al despausar continúa donde quedó (determinista).
            if(controles != null && (controles.isPaused() || controles.isAnimalPanelOpen())){
                continue; // Saltar al siguiente sleep SIN modificar nada
            }
            
            // Obtener lista de animales vivos (copia thread-safe)
            List<BaseAnimal> animals = mundo.getAnimals();
            
            // Si no hay animales, no hacer nada (pero incrementar tick para mantener determinismo)
            if(animals.isEmpty()) { 
                tick++; 
                continue; 
            }

            // ==================================================================================
            // SELECCIÓN DETERMINISTA DE ANIMAL A EVOLUCIONAR
            // ==================================================================================
            // Algoritmo: índice = (seed + tick) % cantidad_animales
            // Esto asegura que:
            // 1. Siempre se selecciona el mismo animal en el mismo tick (reproducible)
            // 2. Todos los animales son seleccionados eventualmente (distribución uniforme)
            // 3. El orden es predecible (útil para debugging)
            int idx = (int)((seed + tick) % animals.size());
            BaseAnimal a = animals.get(idx);
            
            // APLICAR EVOLUCIÓN
            // setGrowthPhase dispara la lógica de evolución del animal:
            // - Cambio de color (mutación)
            // - Cambio de tamaño (crecimiento)
            // - Cambio de velocidad (mejora)
            // - Posible muerte si hambre = 0
            a.setGrowthPhase(a.getGrowthPhase());
            
            // Incrementar tick para próxima iteración
            tick++;
        }
    }
}
