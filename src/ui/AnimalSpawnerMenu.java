package ui;

/**
 * Gestiona el estado del menú de generación de animales.
 * Implementa la lógica para abrir/cerrar el menú, navegar entre opciones y seleccionar tipo de animal.
 * 
 * CONCEPTOS IMPLEMENTADOS:
 * - Máquina de estados (menú cerrado, menú abierto, esperando posición)
 * - Navegación con teclado (arrow keys para seleccionar opción)
 * - Validación de entrada
 */
public class AnimalSpawnerMenu {
    
    // Estado del menú
    public enum MenuState {
        CLOSED,              // Menú cerrado
        OPEN,                // Menú abierto, eligiendo tipo
        WAITING_FOR_POSITION // Tipo seleccionado, esperando click en terreno
    }
    
    // Opciones de animales
    public static final String[] ANIMAL_OPTIONS = {
        "Random",      // 0
        "Tipo 1",      // 1
        "Tipo 2",      // 2
        "Tipo 3",      // 3
        "Tipo 4",      // 4
        "Tipo 5",      // 5
        "Tipo 6",      // 6
        "Tipo 7",      // 7
        "Tipo 8",      // 8
        "Tipo 9",      // 9
        "Tipo 10",     // 10
        "Depredador"   // 11
    };
    
    private MenuState state = MenuState.CLOSED;
    private int selectedIndex = 0; // índice en ANIMAL_OPTIONS
    private int selectedAnimalType = -1; // tipo de animal a generar (-1=random, 0-9=tipo específico)
    
    /**
     * Abre el menú de selección de animales.
     */
    public void open() {
        this.state = MenuState.OPEN;
        this.selectedIndex = 0;
    }
    
    /**
     * Cierra el menú sin generar animal.
     */
    public void close() {
        this.state = MenuState.CLOSED;
        this.selectedIndex = 0;
        this.selectedAnimalType = -1;
    }
    
    /**
     * Selecciona la opción actual y transiciona a estado de espera de posición.
     * Retorna el tipo de animal seleccionado (-1 para random, 0-9 para tipo específico).
     */
    public int selectCurrentOption() {
        if (state != MenuState.OPEN) return -1;
        
        if (selectedIndex == 0) {
            // Random
            selectedAnimalType = -1;
        } else {
            // Tipo 1-10
            selectedAnimalType = selectedIndex - 1; // selectedIndex 1 -> tipo 0, etc.
        }
        
        this.state = MenuState.WAITING_FOR_POSITION;
        return selectedAnimalType;
    }
    
    /**
     * Navega hacia arriba en el menú.
     */
    public void navigateUp() {
        if (state != MenuState.OPEN) return;
        selectedIndex = (selectedIndex - 1 + ANIMAL_OPTIONS.length) % ANIMAL_OPTIONS.length;
    }
    
    /**
     * Navega hacia abajo en el menú.
     */
    public void navigateDown() {
        if (state != MenuState.OPEN) return;
        selectedIndex = (selectedIndex + 1) % ANIMAL_OPTIONS.length;
    }
    
    /**
     * Confirma la posición de spawn y retorna el tipo de animal seleccionado.
     */
    public int confirmSpawn() {
        if (state != MenuState.WAITING_FOR_POSITION) return -1;
        int result = selectedAnimalType;
        close();
        return result;
    }
    
    /**
     * Cancela la operación actual.
     */
    public void cancel() {
        close();
    }
    
    // Getters
    public MenuState getState() { return state; }
    public int getSelectedIndex() { return selectedIndex; }
    public int getSelectedAnimalType() { return selectedAnimalType; }
    public String getSelectedOptionText() { 
        if (selectedIndex >= 0 && selectedIndex < ANIMAL_OPTIONS.length) {
            return ANIMAL_OPTIONS[selectedIndex];
        }
        return "Error";
    }
    
    public boolean isOpen() { return state == MenuState.OPEN; }
    public boolean isWaitingForPosition() { return state == MenuState.WAITING_FOR_POSITION; }
    public boolean isClosed() { return state == MenuState.CLOSED; }
}
