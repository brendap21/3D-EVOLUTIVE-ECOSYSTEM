package ui;

import javax.swing.*;
import java.io.File;

/**
 * SaveGameDialog: diálogo para guardar una partida con nombre personalizado
 * en una ubicación seleccionada por el usuario.
 */
public class SaveGameDialog {
    
    /**
     * Muestra un diálogo para que el usuario seleccione carpeta y nombre de archivo.
     * Retorna el File si el usuario confirma, null si cancela.
     */
    public static File showSaveDialog(JFrame parentFrame) {
        // Paso 1: Seleccionar carpeta
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setDialogTitle("Selecciona carpeta para guardar la partida");
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        folderChooser.setAcceptAllFileFilterUsed(false);
        
        int folderResult = folderChooser.showSaveDialog(parentFrame);
        if (folderResult != JFileChooser.APPROVE_OPTION) {
            return null; // Usuario canceló
        }
        
        File selectedFolder = folderChooser.getSelectedFile();
        
        // Paso 2: Pedir nombre de archivo
        String fileName = JOptionPane.showInputDialog(
            parentFrame,
            "Nombre del archivo de guardado:",
            "Partida 1"
        );
        
        if (fileName == null || fileName.trim().isEmpty()) {
            return null; // Usuario canceló o ingresó nombre vacío
        }
        
        // Asegurar extensión .save
        if (!fileName.endsWith(".save")) {
            fileName = fileName + ".save";
        }
        
        // Crear archivo en la carpeta seleccionada
        File gameFile = new File(selectedFolder, fileName);
        
        // Si el archivo ya existe, pedir confirmación de sobrescritura
        if (gameFile.exists()) {
            int overwriteOption = JOptionPane.showConfirmDialog(
                parentFrame,
                "El archivo ya existe. ¿Deseas sobrescribirlo?",
                "Archivo existente",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            
            if (overwriteOption != JOptionPane.YES_OPTION) {
                return null; // Usuario canceló la sobrescritura
            }
        }
        
        return gameFile;
    }
}
