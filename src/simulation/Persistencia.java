package simulation;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import entities.BaseAnimal;
import math.Vector3;

/**
 * Persistencia: guardado/recuperado en archivo de texto con estado completo
 * incluyendo animales, posición de cámara, orientación y seed del simulador.
 */
public class Persistencia {
    
    // Estructura para guardar estado completo de una partida
    public static class GameState {
        public List<BaseAnimal> animals;
        public Vector3 cameraPos;
        public double cameraYaw;
        public double cameraPitch;
        public long simulatorSeed;
        public long environmentSeed; // Seed para recrear entorno exacto
        public long environmentCreatedAt; // Momento en que se creó el entorno (para crecimiento)
        
        public GameState() {
            this.animals = new ArrayList<>();
            this.cameraPos = new Vector3(0, 80, -150);
            this.cameraYaw = 0.0;
            this.cameraPitch = 0.0;
            this.simulatorSeed = 5555L;
            this.environmentSeed = 12345L;
            this.environmentCreatedAt = System.currentTimeMillis();
        }
    }
    
    // Guardar estado completo de la partida
    public static boolean saveGameState(File file, GameState state) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            // Guardar metadatos de cámara
            pw.println("CAMERA:" + state.cameraPos.x + "," + state.cameraPos.y + "," + state.cameraPos.z);
            pw.println("ROTATION:" + state.cameraYaw + "," + state.cameraPitch);
            pw.println("SEED:" + state.simulatorSeed);
            pw.println("ENV_SEED:" + state.environmentSeed);
            pw.println("ENV_CREATED_AT:" + state.environmentCreatedAt);
            pw.println("ANIMALS_START");
            
            // Guardar animales
            for (BaseAnimal a : state.animals) {
                pw.println(a.serializeState());
            }
            
            pw.println("ANIMALS_END");
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    // Cargar estado completo de la partida
    public static GameState loadGameState(File file) {
        GameState state = new GameState();
        
        if (!file.exists()) return state;
        
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean readingAnimals = false;
            
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                if (line.startsWith("CAMERA:")) {
                    String[] parts = line.substring(7).split(",");
                    if (parts.length == 3) {
                        double x = Double.parseDouble(parts[0]);
                        double y = Double.parseDouble(parts[1]);
                        double z = Double.parseDouble(parts[2]);
                        state.cameraPos = new Vector3(x, y, z);
                    }
                } else if (line.startsWith("ROTATION:")) {
                    String[] parts = line.substring(9).split(",");
                    if (parts.length == 2) {
                        state.cameraYaw = Double.parseDouble(parts[0]);
                        state.cameraPitch = Double.parseDouble(parts[1]);
                    }
                } else if (line.startsWith("SEED:")) {
                    state.simulatorSeed = Long.parseLong(line.substring(5));
                } else if (line.startsWith("ENV_SEED:")) {
                    state.environmentSeed = Long.parseLong(line.substring(9));
                } else if (line.startsWith("ENV_CREATED_AT:")) {
                    state.environmentCreatedAt = Long.parseLong(line.substring("ENV_CREATED_AT:".length()));
                } else if (line.equals("ANIMALS_START")) {
                    readingAnimals = true;
                } else if (line.equals("ANIMALS_END")) {
                    readingAnimals = false;
                } else if (readingAnimals) {
                    BaseAnimal a = BaseAnimal.deserializeState(line);
                    if (a != null) {
                        state.animals.add(a);
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        return state;
    }
}

