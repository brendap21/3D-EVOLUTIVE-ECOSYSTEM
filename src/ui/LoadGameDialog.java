package ui;

import javax.swing.*;
import java.io.File;

public class LoadGameDialog {
    public static File showLoadDialog(JFrame parentFrame) {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setDialogTitle("Selecciona carpeta con archivos guardados");
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        folderChooser.setAcceptAllFileFilterUsed(false);
        
        int folderResult = folderChooser.showOpenDialog(parentFrame);
        if (folderResult != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        
        File selectedFolder = folderChooser.getSelectedFile();
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
            return null;
        }
        for (File f : saveFiles) {
            if (f.getName().equals(selected)) {
                return f;
            }
        }
        
        return null;
    }
}
