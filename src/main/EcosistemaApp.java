package main;

import javax.swing.JFrame;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import math.Camera;
import math.Vector3;
import entities.*;
import java.awt.Color;
import ui.Controles;

/**
 * ============================================================================================
 * EcosistemaApp - Clase principal (punto de entrada) del simulador 3D
 * ============================================================================================
 * 
 * PROPÓSITO:
 * Esta clase inicializa y configura todos los componentes del ecosistema 3D:
 * - Ventana de visualización (JFrame + RenderPanel)
 * - Cámara 3D (Camera con proyección perspectiva)
 * - Mundo y entidades (animales, terreno, vegetación)
 * - Sistema de controles (teclado + mouse tipo FPS)
 * - Hilos de simulación y renderizado
 * 
 * CONCEPTOS IMPLEMENTADOS:
 * 1. ARQUITECTURA MULTI-HILO:
 *    - Hilo principal (UI/AWT Event Dispatch Thread)
 *    - RenderThread: Actualiza y dibuja escena ~143 FPS
 *    - Simulador: Evoluciona animales cada 1 segundo (determinista)
 * 
 * 2. INICIALIZACIÓN DE ESCENA 3D:
 *    - Terreno procedimental (superficie 3D con ruido Perlin)
 *    - Spawn aleatorio de animales voxel (10 tipos diferentes)
 *    - Spawn de entidades ambientales (árboles, rocas, pasto, flores)
 * 
 * 3. SISTEMAS DE COORDENADAS:
 *    - World Space: Sistema global donde viven todas las entidades
 *    - Camera Space: Espacio relativo a la cámara (tras aplicar view matrix)
 *    - Screen Space: Coordenadas 2D del framebuffer tras proyección
 * 
 * 4. PATRÓN OBSERVER:
 *    - Listeners de mouse/teclado conectados a Controles
 *    - Panel recibe eventos y actualiza estado de la cámara
 * 
 * ============================================================================================
 */
public class EcosistemaApp {
    // Referencia estática al mundo para que los animales puedan acceder a él
    private static simulation.Mundo mundoRef;
    
    /**
     * Método principal: construye la aplicación y arranca los hilos.
     * 
     * FLUJO DE INICIALIZACIÓN:
     * 1. Crear ventana Swing y panel de renderizado
     * 2. Crear cámara 3D con posición y distancia focal
     * 3. Poblar mundo con terreno, animales y vegetación
     * 4. Configurar controles (input handling estilo Minecraft)
     * 5. Iniciar hilo de render (actualiza visuals)
     * 6. Iniciar hilo de simulación (actualiza lógica/evolución)
     */
    public static void main(String[] args){
        // Dimensiones de la ventana de renderizado
        int ancho = 1000, alto = 700;

        // CREAR CÁMARA 3D
        // Posición inicial: (0, 80, -150) = 80 unidades arriba, 150 atrás del origen
        // Distancia focal: 500 (controla el campo de visión en la proyección perspectiva)
        // La cámara usa PROYECCIÓN PERSPECTIVA (objetos lejanos se ven más pequeños)
        Camera cam = new Camera(new Vector3(0,80,-150), 500);

        // CREAR PANEL DE RENDERIZADO
        // Este panel contiene el backBuffer donde se dibuja píxel por píxel
        // Implementa DOBLE BUFFER para evitar parpadeo (flickering)
        RenderPanel panel = new RenderPanel(ancho, alto);
        
        // CREAR PANEL DE VISUALIZACIÓN
        // DisplayPanel: Solo muestra la imagen renderizada, sin dibujar contenido
        DisplayPanel displayPanel = new DisplayPanel(ancho, alto);
        
        // CREAR VENTANA (JFrame)
        JFrame frame = new JFrame("3D EVOLUTIVE ECOSYSTEM");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(displayPanel);
        frame.pack(); // Ajusta tamaño al panel
        frame.setLocationRelativeTo(null); // Centra en pantalla
        frame.setVisible(true);

        // CREAR MUNDO (contenedor de todas las entidades)
        // Mundo maneja la lista sincronizada de entidades renderizables
        simulation.Mundo mundo = new simulation.Mundo();
        mundoRef = mundo; // Guardar referencia estática para animales
        
        // CONFIGURAR SEED PARA REPRODUCIBILIDAD
        // La seed determina la generación del ambiente (árboles, rocas, etc.)
        // Usar la misma seed genera exactamente el mismo mundo
        long envSeed = System.currentTimeMillis() + 12345;
        mundo.setEnvironmentSeed(envSeed);
        mundo.setEnvironmentCreatedAt(System.currentTimeMillis());
        
        // CREAR TERRENO PROCEDIMENTAL
        // Parámetros: 160x160 grid, escala 8.0 (cada celda = 8 unidades), seed 12345
        // El terreno es una SUPERFICIE 3D generada con ruido Perlin (alturas aleatorias suaves)
        // Total: 1280x1280 unidades de mundo
        // Color: Verde (60, 140, 60)
        mundo.addEntity(new Terreno(160, 160, 8.0, 12345L, new Color(60, 140, 60)));

        // SPAWN DE ANIMALES ALEATORIOS
        // Genera 5-7 animales de 10 tipos posibles, distribuidos sin solaparse
        spawnRandomAnimals(mundo);

        // SPAWN DE ENTIDADES AMBIENTALES
        // Árboles (cilindros), rocas (cubos), pasto, arbustos, flores
        spawnEnvironmentalEntities(mundo);
        
        // CREAR SISTEMA DE CONTROLES (input handling estilo FPS)
        // Usar displayPanel como componente de referencia para que las coordenadas
        // de mouse estén alineadas con el framebuffer mostrado.
        Controles controles = new Controles(cam, displayPanel);
        
        // CONECTAR CONTROLES CON EL MUNDO
        // setMundoAndCorrectPosition ajusta la posición de la cámara para que esté
        // sobre el terreno (evita que comience dentro del suelo)
        controles.setMundoAndCorrectPosition(mundo);
        panel.setMundo(mundo);
        panel.setCamera(cam);
        
        // REGISTRAR LISTENERS DE INPUT
        // MouseMotionListener: captura movimiento del mouse para rotar cámara (yaw/pitch)
        // KeyListener: captura WASD, espacio, ctrl para mover cámara
        // Listeners: mouse sobre el DisplayPanel para obtener coordenadas correctas
        displayPanel.addMouseMotionListener(controles);
        displayPanel.addKeyListener(controles);
        displayPanel.setFocusable(true); // Necesario para recibir eventos de teclado
        displayPanel.requestFocusInWindow();

        // LISTENER DE MOUSE CLICK
        // Cuando está pausado, detecta clicks en botones del menú de pausa
        // Cuando no está pausado, permite seleccionar animales haciendo click
        displayPanel.addMouseListener(new java.awt.event.MouseAdapter(){
            @Override
            public void mousePressed(java.awt.event.MouseEvent e){
                panel.handleMousePressed(e, controles);
            }
        });
        
        // LISTENER DE TECLAS (ESC para cerrar panel de animales)
        displayPanel.addKeyListener(new java.awt.event.KeyAdapter(){
            @Override
            public void keyPressed(java.awt.event.KeyEvent e){
                panel.handleKeyPressed(e, controles);
            }
        });

        // BLOQUEAR MOUSE (estilo FPS)
        // El cursor se oculta y se fija en el centro de la pantalla
        // Los movimientos del mouse se detectan como deltas (dx, dy) desde el centro
        // Esto permite rotación ilimitada sin que el cursor llegue al borde
        controles.lockMouse(true);

        // INICIAR HILO DE RENDERIZADO
        // Actualiza y dibuja la escena ~143 veces por segundo (7ms sleep)
        // Aplica transformaciones 3D, proyección, z-buffer, rasterización
        RenderThread hilo = new RenderThread(panel, mundo, cam, controles, displayPanel);
        hilo.start();

        // INICIAR HILO DE SIMULACIÓN
        // Actualiza lógica del mundo cada 1 segundo:
        // - Evolución de animales (mutaciones, crecimiento)
        // - Muerte por hambre/edad
        // - Reproducción (si se implementa)
        // IMPORTANTE: Usa seed fija (5555L) para que la evolución sea DETERMINISTA
        simulation.Simulador sim = new simulation.Simulador(mundo, 5555L);
        sim.setControles(controles); // Pasar controles para respetar la PAUSA ABSOLUTA
        panel.setSimulador(sim);
        sim.start();
    }

    /**
     * ========================================================================================
     * spawnRandomAnimals - Genera animales aleatorios en el mundo
     * ========================================================================================
     * 
     * ALGORITMO:
     * 1. Determinar cantidad aleatoria de animales (5-7)
     * 2. Para cada animal a generar:
     *    a. Elegir tipo aleatorio (0-9, hay 10 tipos)
     *    b. Generar posición aleatoria en área visible frente a la cámara
     *    c. Obtener altura del terreno en esa posición (evita flotar o hundirse)
     *    d. Verificar distancia mínima con otros animales (evita amontonamiento)
     *    e. Si la posición es válida, crear y agregar el animal al mundo
     * 
     * CONCEPTOS:
     * - Spawn distribution: Distribuye entidades con separación mínima
     * - Terrain heightmap query: Obtiene Y del terreno en coordenadas (X, Z)
     * - Seed generation: Cada animal tiene seed única para evolución determinista
     * 
     * @param mundo El mundo donde se agregarán los animales
     */
    private static void spawnRandomAnimals(simulation.Mundo mundo) {
        // Generador de números aleatorios con seed basada en tiempo actual
        Random r = new Random(System.currentTimeMillis());
        
        // Cantidad de animales a generar (5-7)
        int numAnimals = 5 + r.nextInt(3);
        
        // Lista de posiciones ya usadas (para validar distancia mínima)
        java.util.List<Vector3> usedPositions = new ArrayList<>();
        
        // Distancia mínima entre animales (50 unidades de mundo)
        // Esto evita que aparezcan amontonados
        double minDistance = 50;

        int attempts = 0; // Contador de intentos
        int spawned = 0;  // Contador de animales exitosamente generados
        
        // Intentar hasta 300 veces o hasta generar todos los animales
        while (spawned < numAnimals && attempts < 300) {
            // Elegir tipo de animal aleatorio (0-9 = 10 tipos diferentes)
            int animalType = r.nextInt(10);
            
            // Generar seed única para este animal (determina su evolución)
            long seed = System.currentTimeMillis() + spawned * 1000 + r.nextLong();

            // GENERAR POSICIÓN ALEATORIA
            // Área: Rectángulo frente a la cámara inicial (0, 80, -150)
            // X: [-150, 150] (300 unidades de ancho)
            // Z: [-100, 100] (200 unidades de profundidad)
            double x = -150 + r.nextDouble() * 300;
            double z = -100 + r.nextDouble() * 200;
            
            // OBTENER ALTURA DEL TERRENO
            // El terreno es una heightmap (mapa de alturas)
            // getHeightAt(x, z) devuelve la coordenada Y del terreno en ese punto
            double terrainHeight = mundo.getHeightAt(x, z);
            if (terrainHeight == Double.NEGATIVE_INFINITY) terrainHeight = 0.0;
            
            // OFFSET VERTICAL
            // Algunos animales tienen voxels en Y negativa (patas bajo el origen local)
            // Añadimos 5.0 unidades para que no se hundan en el suelo
            double y = terrainHeight + 5.0;
            
            Vector3 pos = new Vector3(x, y, z);

            // VERIFICAR DISTANCIA MÍNIMA CON OTROS ANIMALES
            // Si está muy cerca de otro animal, rechazar esta posición
            boolean tooClose = false;
            for (Vector3 used : usedPositions) {
                // Calcular distancia 2D (ignoramos Y porque el terreno puede tener desniveles)
                double dist = Math.sqrt((pos.x - used.x) * (pos.x - used.x) + 
                                       (pos.z - used.z) * (pos.z - used.z));
                if (dist < minDistance) {
                    tooClose = true;
                    break;
                }
            }

            // Si la posición es válida, crear el animal y agregarlo
            if (!tooClose) {
                usedPositions.add(pos);
                Renderable animal = createAnimalOfType(animalType, pos, seed);
                if (animal != null) {
                    if (animal instanceof entities.BaseAnimal) {
                        mundo.addAnimal((entities.BaseAnimal) animal);
                    } else {
                        mundo.addEntity(animal);
                    }
                    spawned++;
                }
            }
            attempts++;
        }
    }

    /**
     * ========================================================================================
     * createAnimalOfType - Factoría de animales (Factory Pattern)
     * ========================================================================================
     * 
     * PROPÓSITO:
     * Crea una instancia del tipo de animal especificado con posición y seed dadas.
     * 
     * TIPOS DE ANIMALES:
     * - Tipos 0-9: Animales herbívoros/omnívoros (diferentes colores y comportamientos)
     * - Tipo 10: Depredador (caza otros animales)
     * 
     * CONCEPTOS:
     * - Factory Pattern: Centraliza la creación de objetos
     * - Polymorphism: Todos heredan de BaseAnimal (interfaz común)
     * - World reference: Animales necesitan acceso al mundo para colisiones
     * 
     * @param type Tipo de animal (0-10)
     * @param pos Posición inicial en el mundo (Vector3)
     * @param seed Seed para evolución determinista
     * @return Instancia de Renderable (animal creado)
     */
    public static Renderable createAnimalOfType(int type, Vector3 pos, long seed) {
        // Configurar referencia al mundo para que animales puedan acceder a él
        // (necesario para colisiones, detección de terreno, etc.)
        entities.BaseAnimal.setWorld(mundoRef);
        entities.Depredador.setWorldReference(mundoRef);
        
        // Switch statement para instanciar el tipo correcto de animal
        // Cada tipo tiene diferentes características (color, tamaño, velocidad, comportamiento)
        switch (type) {
            case 0: return new AnimalType01(pos, seed);
            case 1: return new AnimalType02(pos, seed);
            case 2: return new AnimalType03(pos, seed);
            case 3: return new AnimalType04(pos, seed);
            case 4: return new AnimalType05(pos, seed);
            case 5: return new AnimalType06(pos, seed);
            case 6: return new AnimalType07(pos, seed);
            case 7: return new AnimalType08(pos, seed);
            case 8: return new AnimalType09(pos, seed);
            case 9: return new AnimalType10(pos, seed);
            case 10: return new entities.Depredador(pos, seed); // Depredador (caza otros animales)
            default: return null;
        }
    }

    /**
     * ========================================================================================
     * spawnEnvironmentalEntities - Genera entidades decorativas del ecosistema
     * ========================================================================================
     * 
     * PROPÓSITO:
     * Poblar el mundo con vegetación y elementos ambientales para dar vida al ecosistema.
     * 
     * ENTIDADES GENERADAS:
     * - Árboles (7-9): Cilindros con hojas esféricas
     * - Rocas (5-8): Estructuras de cubos apilados
     * - Pasto (80-120): Pequeñas matas de hierba
     * - Arbustos (4-6): Vegetación media
     * - Flores (40-60): Decoración colorida
     * 
     * CONCEPTOS:
     * - Procedural generation: Usa seeds para generar siempre el mismo ambiente
     * - Spatial hashing: Usa distancia mínima para distribuir entidades uniformemente
     * - Heightmap placement: Coloca entidades sobre el terreno
     * 
     * @param mundo El mundo donde se agregarán las entidades
     */
    private static void spawnEnvironmentalEntities(simulation.Mundo mundo) {
        // Generador aleatorio con seed fija (reproducible)
        Random r = new Random(System.currentTimeMillis() + 12345);
        
        // Lista compartida de posiciones usadas (para evitar solapamiento entre tipos)
        java.util.List<Vector3> usedPositions = new ArrayList<>();

        // Generar ÁRBOLES (7-9)
        // Distancia mínima: 180 unidades (árboles grandes necesitan más espacio)
        int numTrees = 7 + r.nextInt(3);
        spawnWithMinDistance(mundo, r, numTrees, 180, usedPositions, "tree");

        // Generar ROCAS (5-8)
        // Distancia mínima: 90 unidades
        int numRocks = 5 + r.nextInt(4);
        spawnWithMinDistance(mundo, r, numRocks, 90, usedPositions, "rock");

        // Generar PASTO (80-120)
        // Muchísimo pasto con separación mínima de 15 unidades
        // Esto crea un ecosistema denso y natural
        int numGrass = 80 + r.nextInt(41);
        spawnWithMinDistance(mundo, r, numGrass, 15, usedPositions, "grass");

        // Generar ARBUSTOS (4-6)
        // Distancia mínima: 85 unidades
        int numBushes = 4 + r.nextInt(3);
        spawnWithMinDistance(mundo, r, numBushes, 85, usedPositions, "bush");

        // Generar FLORES (40-60)
        // Flores pequeñas y coloridas con separación mínima de 20 unidades
        int numFlowers = 40 + r.nextInt(21);
        spawnWithMinDistance(mundo, r, numFlowers, 20, usedPositions, "flower");
    }

    /**
     * ========================================================================================
     * spawnWithMinDistance - Helper para spawn con distancia mínima
     * ========================================================================================
     * 
     * ALGORITMO:
     * 1. Intentar generar 'count' entidades del tipo especificado
     * 2. Para cada intento:
     *    a. Generar posición aleatoria en el mapa (-320 a 320 en X y Z)
     *    b. Verificar que esté a distancia mínima de TODAS las posiciones usadas
     *    c. Si es válida, crear la entidad y agregar posición a la lista
     * 3. Limitar intentos para evitar bucle infinito (count * 15 intentos máximo)
     * 
     * CONCEPTOS:
     * - Spatial distribution: Distribuye entidades uniformemente
     * - Rejection sampling: Rechaza posiciones inválidas y reintenta
     * - Shared position list: Previene solapamiento entre diferentes tipos
     * 
     * @param mundo Mundo donde agregar entidades
     * @param r Generador aleatorio
     * @param count Cantidad de entidades a generar
     * @param minDistance Distancia mínima entre entidades (unidades de mundo)
     * @param usedPositions Lista de posiciones ya usadas (modificada in-place)
     * @param type Tipo de entidad ("tree", "rock", "grass", "bush", "flower")
     */
    private static void spawnWithMinDistance(simulation.Mundo mundo, Random r, int count, 
                                            double minDistance, java.util.List<Vector3> usedPositions, String type) {
        int spawned = 0;   // Contador de entidades generadas exitosamente
        int attempts = 0;  // Contador de intentos totales
        int maxAttempts = count * 15; // Límite para evitar búsqueda infinita

        // Paleta de colores para flores (8 colores vibrantes)
        Color[] flowerColors = {
            new Color(255, 20, 147),   // Rosa profundo
            new Color(255, 105, 180),  // Rosa caliente
            new Color(255, 165, 0),    // Naranja
            new Color(255, 215, 0),    // Dorado
            new Color(144, 238, 144),  // Verde claro
            new Color(0, 206, 209),    // Turquesa oscuro
            new Color(186, 85, 211),   // Orquídea media
            new Color(255, 20, 147),   // Carmesí
        };

        // Bucle principal de spawn
        while (spawned < count && attempts < maxAttempts) {
            // Generar posición aleatoria en el mapa
            // Área: 640x640 unidades centrada en el origen
            double x = -320 + r.nextDouble() * 640;
            double z = -320 + r.nextDouble() * 640;
            Vector3 pos = new Vector3(x, 0, z); // Y=0 temporal (se ajusta al terreno después)

            // VALIDACIÓN DE DISTANCIA MÍNIMA
            // Verificar que la nueva posición no esté muy cerca de ninguna posición usada
            boolean tooClose = false;
            for (Vector3 used : usedPositions) {
                // Calcular distancia euclidiana 2D (solo X y Z)
                double dist = Math.sqrt((pos.x - used.x) * (pos.x - used.x) + 
                                       (pos.z - used.z) * (pos.z - used.z));
                if (dist < minDistance) {
                    tooClose = true;
                    break;
                }
            }

            // Si la posición es válida, crear la entidad correspondiente
            if (!tooClose) {
                usedPositions.add(pos); // Marcar posición como usada
                long seed = System.currentTimeMillis() + spawned * 100 + r.nextLong();
                
                // Switch para crear el tipo de entidad correcto
                switch (type) {
                    case "tree":
                        // Árbol: tronco (cilindro) + hojas (esfera de cubos)
                        // Parámetros: radio base, altura tronco, radio hojas, seed
                        Arbol arbol = new Arbol(pos, 12 + r.nextInt(8), 60 + r.nextInt(30), 28 + r.nextInt(15), seed);
                        mundo.addEntity(arbol);
                        break;
                    case "rock":
                        // Roca: estructura de cubos apilados irregularmente
                        Piedra piedra = new Piedra(pos, seed);
                        mundo.addEntity(piedra);
                        break;
                    case "grass":
                        // Pasto: pequeños voxels verdes que crecen con el tiempo
                        Pasto pasto = new Pasto(pos, seed);
                        mundo.addEntity(pasto);
                        break;
                    case "bush":
                        // Arbusto: vegetación media sin animación
                        Arbusto arbusto = new Arbusto(pos);
                        mundo.addEntity(arbusto);
                        break;
                    case "flower":
                        // Flor: tallo + pétalos de colores aleatorios
                        Color flowerColor = flowerColors[r.nextInt(flowerColors.length)];
                        Flor flor = new Flor(pos, flowerColor, seed);
                        mundo.addEntity(flor);
                        break;
                }
                spawned++;
            }
            attempts++;
        }
    }
}
