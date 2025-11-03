package simulation;

import java.io.*;
import java.util.List;
import entities.Animal;

/**
 * Persistencia simple: guardado/recuperado en archivo de texto con una línea
 * por animal usando el formato definido en Animal.serialize().
 */
public class Persistencia {
    public static boolean saveAnimals(File file, List<Animal> animals){
        try(PrintWriter pw = new PrintWriter(new FileWriter(file))){
            for(Animal a : animals){
                pw.println(a.serialize());
            }
            return true;
        }catch(IOException ex){
            ex.printStackTrace();
            return false;
        }
    }

    public static java.util.List<Animal> loadAnimals(File file){
        java.util.List<Animal> out = new java.util.ArrayList<>();
        if(!file.exists()) return out;
        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            String line;
            while((line = br.readLine()) != null){
                if(line.trim().isEmpty()) continue;
                Animal a = Animal.deserialize(line);
                if(a != null) out.add(a);
            }
        }catch(IOException ex){ ex.printStackTrace(); }
        return out;
    }
}
