package entities;

import math.Vector3;

/**
 * AnimationController: Gestor centralizado de animaciones secundarias para animales.
 * Proporciona métodos reutilizables para:
 * - Blinks (parpadeos): para abrir/cerrar ojos
 * - Jaw movement (movimiento de mandíbula): para abrir/cerrar boca
 * - Tail wag (meneo de cola): movimiento ondulante de cola
 * - Wing flap (bateo de alas): movimiento alar coordinado
 * - Head turn (giro de cabeza): movimiento de cabeza izquierda/derecha
 * - Ear twitch (movimiento de orejas): pequeños movimientos de orejas
 * - Body sway (balanceo de cuerpo): movimiento natural de cuerpo
 * 
 * Cada animal puede usar estos métodos en su render para agregar vida a las animaciones.
 */
public class AnimationController {
    
    private double blinkPhase = 0.0;
    private double jawPhase = 0.0;
    private double tailWagPhase = 0.0;
    private double wingFlapPhase = 0.0;
    private double headTurnPhase = 0.0;
    private double earTwitchPhase = 0.0;
    private double bodySwayPhase = 0.0;
    
    // Configuración de frecuencias (en Hz aproximadamente)
    private double blinkFrequency = 2.5;      // Parpadeos por segundo
    private double jawFrequency = 3.0;         // Movimientos de mandíbula por segundo
    private double tailWagFrequency = 2.0;     // Meneo de cola por segundo
    private double wingFlapFrequency = 4.0;    // Bateos de ala por segundo
    private double headTurnFrequency = 1.0;    // Giros de cabeza por segundo
    private double earTwitchFrequency = 3.5;   // Movimientos de orejas por segundo
    private double bodySwayFrequency = 0.8;    // Balanceo de cuerpo por segundo
    
    // Amplitudes (rango de movimiento)
    private double blinkAmplitude = 1.0;       // 0-1 para cierre de ojo
    private double jawAmplitude = 0.15;        // En unidades voxel
    private double tailWagAmplitude = 0.3;     // En unidades voxel
    private double wingFlapAmplitude = 0.6;    // En unidades voxel
    private double headTurnAmplitude = 0.25;   // En radianes
    private double earTwitchAmplitude = 0.2;   // En radianes
    private double bodySwayAmplitude = 0.15;   // En radianes
    
    /**
     * Actualizar todas las animaciones. Llamar en cada frame.
     * @param deltaTime Tiempo transcurrido desde el último frame en segundos
     */
    public void update(double deltaTime) {
        blinkPhase += deltaTime * blinkFrequency * Math.PI * 2;
        jawPhase += deltaTime * jawFrequency * Math.PI * 2;
        tailWagPhase += deltaTime * tailWagFrequency * Math.PI * 2;
        wingFlapPhase += deltaTime * wingFlapFrequency * Math.PI * 2;
        headTurnPhase += deltaTime * headTurnFrequency * Math.PI * 2;
        earTwitchPhase += deltaTime * earTwitchFrequency * Math.PI * 2;
        bodySwayPhase += deltaTime * bodySwayFrequency * Math.PI * 2;
        
        // Mantener fases en rango
        blinkPhase %= Math.PI * 2;
        jawPhase %= Math.PI * 2;
        tailWagPhase %= Math.PI * 2;
        wingFlapPhase %= Math.PI * 2;
        headTurnPhase %= Math.PI * 2;
        earTwitchPhase %= Math.PI * 2;
        bodySwayPhase %= Math.PI * 2;
    }
    
    /**
     * Obtener valor de parpadeo (0 = ojo abierto completamente, 1 = ojo cerrado).
     * Patrón: parpadeo corto, ojos abiertos más tiempo.
     * @return Valor entre 0 y 1
     */
    public double getBlinkAmount() {
        // Crear patrón de parpadeo: rápido cierre, lenta apertura
        double normalized = blinkPhase / (Math.PI * 2);
        if (normalized < 0.1) {
            // Cierre rápido (0-10% del ciclo)
            return Math.sin(normalized * Math.PI / 0.1) * blinkAmplitude;
        } else if (normalized < 0.15) {
            // Ojo cerrado (10-15%)
            return blinkAmplitude;
        } else if (normalized < 0.3) {
            // Apertura lenta (15-30%)
            return Math.cos((normalized - 0.15) * Math.PI / 0.15) * blinkAmplitude;
        }
        // Ojos abiertos resto del tiempo
        return 0.0;
    }
    
    /**
     * Obtener desplazamiento vertical de la mandíbula para simular apertura/cierre.
     * @return Desplazamiento en unidades voxel (negativo = abierto, positivo = cerrado)
     */
    public double getJawOpen() {
        return Math.sin(jawPhase) * jawAmplitude;
    }
    
    /**
     * Obtener desplazamiento lateral de la cola.
     * @param segmentIndex Índice del segmento de cola (0 es el más cercano al cuerpo)
     * @return Desplazamiento en unidades voxel
     */
    public double getTailWagOffset(int segmentIndex) {
        // Propagar onda a lo largo de la cola
        double phase = tailWagPhase + segmentIndex * 0.4;
        return Math.sin(phase) * tailWagAmplitude;
    }
    
    /**
     * Obtener desplazamiento vertical de la cola (para cola ondulante).
     * @param segmentIndex Índice del segmento de cola
     * @return Desplazamiento vertical en unidades voxel
     */
    public double getTailWagVertical(int segmentIndex) {
        double phase = tailWagPhase + segmentIndex * 0.3;
        return Math.sin(phase + Math.PI / 4) * tailWagAmplitude * 0.5;
    }
    
    /**
     * Obtener amplitud de bateo de ala.
     * @param wingIndex Índice del ala (0 = izquierda, 1 = derecha, etc.)
     * @return Desplazamiento en unidades voxel
     */
    public double getWingFlap(int wingIndex) {
        double phase = wingFlapPhase + (wingIndex % 2) * Math.PI; // Alas opuestas
        return Math.sin(phase) * wingFlapAmplitude;
    }
    
    /**
     * Obtener rotación de cabeza (giro izquierda/derecha).
     * @return Ángulo en radianes
     */
    public double getHeadTurn() {
        return Math.sin(headTurnPhase) * headTurnAmplitude;
    }
    
    /**
     * Obtener rotación de una oreja.
     * @param earIndex Índice de oreja (0 = izquierda, 1 = derecha)
     * @return Ángulo en radianes
     */
    public double getEarTwitch(int earIndex) {
        double phase = earTwitchPhase + earIndex * Math.PI;
        return Math.sin(phase) * earTwitchAmplitude;
    }
    
    /**
     * Obtener balanceo del cuerpo (para simular movimiento natural).
     * @return Ángulo en radianes
     */
    public double getBodySway() {
        return Math.sin(bodySwayPhase) * bodySwayAmplitude;
    }
    
    /**
     * Obtener pequeña altura de salto (para animación de rebote mientras está de pie).
     * @return Desplazamiento vertical en unidades voxel
     */
    public double getIdleBounce() {
        // Pequeño rebote en reposo
        return Math.abs(Math.sin(bodySwayPhase * 2)) * 0.1;
    }
    
    /**
     * Obtener rotación de mandíbula (para efectos adicionales).
     * @return Ángulo en radianes
     */
    public double getJawRotation() {
        return Math.sin(jawPhase) * 0.15;
    }
    
    /**
     * Resetear todas las animaciones.
     */
    public void reset() {
        blinkPhase = 0.0;
        jawPhase = 0.0;
        tailWagPhase = 0.0;
        wingFlapPhase = 0.0;
        headTurnPhase = 0.0;
        earTwitchPhase = 0.0;
        bodySwayPhase = 0.0;
    }
    
    // Getters y setters para configuración
    
    public void setBlinkFrequency(double freq) { this.blinkFrequency = freq; }
    public void setJawFrequency(double freq) { this.jawFrequency = freq; }
    public void setTailWagFrequency(double freq) { this.tailWagFrequency = freq; }
    public void setWingFlapFrequency(double freq) { this.wingFlapFrequency = freq; }
    public void setHeadTurnFrequency(double freq) { this.headTurnFrequency = freq; }
    public void setEarTwitchFrequency(double freq) { this.earTwitchFrequency = freq; }
    public void setBodySwayFrequency(double freq) { this.bodySwayFrequency = freq; }
    
    public void setBlinkAmplitude(double amp) { this.blinkAmplitude = amp; }
    public void setJawAmplitude(double amp) { this.jawAmplitude = amp; }
    public void setTailWagAmplitude(double amp) { this.tailWagAmplitude = amp; }
    public void setWingFlapAmplitude(double amp) { this.wingFlapAmplitude = amp; }
    public void setHeadTurnAmplitude(double amp) { this.headTurnAmplitude = amp; }
    public void setEarTwitchAmplitude(double amp) { this.earTwitchAmplitude = amp; }
    public void setBodySwayAmplitude(double amp) { this.bodySwayAmplitude = amp; }
    
    public double getBlinkPhase() { return blinkPhase; }
    public double getJawPhase() { return jawPhase; }
    public double getTailWagPhase() { return tailWagPhase; }
    public double getWingFlapPhase() { return wingFlapPhase; }
    public double getHeadTurnPhase() { return headTurnPhase; }
    public double getEarTwitchPhase() { return earTwitchPhase; }
    public double getBodySwayPhase() { return bodySwayPhase; }
}
