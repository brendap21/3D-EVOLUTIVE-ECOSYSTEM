package ui;

import javax.swing.*;
import java.io.File;

/**
 * LoadGameDialog: diálogo para seleccionar y cargar un archivo de partida guardada.
 */
public class LoadGameDialog {
    
    /**
     * Muestra un diálogo para seleccionar un archivo de partida guardada.
     * El usuario selecciona primero la carpeta, luego elige un archivo .save
     * Retorna el File si el usuario confirma, null si cancela.
     */
    public static File showLoadDialog(JFrame parentFrame) {
        // Paso 1: Seleccionar carpeta
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setDialogTitle("Selecciona carpeta con archivos guardados");
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        folderChooser.setAcceptAllFileFilterUsed(false);
        
        int folderResult = folderChooser.showOpenDialog(parentFrame);
        if (folderResult != JFileChooser.APPROVE_OPTION) {
            return null; // Usuario canceló
        }
        
        File selectedFolder = folderChooser.getSelectedFile();
        
        // Paso 2: Listar archivos .save en la carpeta
        File[] saveFiles = selectedFolder.listFiles((dir, name) -> name.endsWith(".save"));
        
        if (saveFiles == null || saveFiles.length == 0) {
            JOptionPane.showMessageDialog(
                parentFrame,
                "No hay archivos de partidas guardadas en esta carpeta.",
                "Sin partidas",
                JOptionPane.INFORMATION_MESSAGE
            );
            return null;
        }
        
        // Paso 3: Crear diálogo de selección
        String[] fileNames = new String[saveFiles.length];
        for (int i = 0; i < saveFiles.length; i++) {
            fileNames[i] = saveFiles[i].getName();
        }
        
        String selected = (String) JOptionPane.showInputDialog(
            parentFrame,
            "Selecciona una partida para cargar:",
            "Cargar partida",
            JOptionPane.PLAIN_MESSAGE,
            null,
            fileNames,
            fileNames[0]
        );
        
        if (selected == null) {
            return null; // Usuario canceló
        }
        
        // Encontrar el archivo seleccionado
        for (File f : saveFiles) {
            if (f.getName().equals(selected)) {
                return f;
            }
        }
        
        return null;
    }
}
