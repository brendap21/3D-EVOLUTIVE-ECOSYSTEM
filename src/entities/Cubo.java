package entities;

import main.Renderable;
import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;

/**
 * ============================================================================================
 * CUBO - PRIMITIVA 3D GEOMÉTRICA (REQUISITO DEL PROYECTO)
 * ============================================================================================
 * 
 * Esta clase implementa el requisito de "CUBOS" como primitiva 3D fundamental.
 * 
 * DEFINICIÓN GEOMÉTRICA:
 * - Un cubo es un poliedro con 6 caras cuadradas
 * - Tiene 8 vértices (esquinas)
 * - Tiene 12 aristas
 * - Es un caso especial de paralelepípedo donde todas las caras son congruentes
 * 
 * REPRESENTACIÓN 3D:
 * Los 8 vértices se calculan a partir de:
 * - Centro (posicion): Vector3(x, y, z)
 * - Tamaño (tamano): Longitud de cada arista
 * - Semi-tamaño (s = tamano/2): Radio desde centro a cara
 * 
 * Los 8 vértices son (relativo al centro):
 *   v0 = (-s, -s, -s)  // Bottom-Front-Left
 *   v1 = (+s, -s, -s)  // Bottom-Front-Right
 *   v2 = (+s, +s, -s)  // Top-Front-Right
 *   v3 = (-s, +s, -s)  // Top-Front-Left
 *   v4 = (-s, -s, +s)  // Bottom-Back-Left
 *   v5 = (+s, -s, +s)  // Bottom-Back-Right
 *   v6 = (+s, +s, +s)  // Top-Back-Right
 *   v7 = (-s, +s, +s)  // Top-Back-Left
 * 
 * TRANSFORMACIONES APLICADAS:
 * 1. TRASLACIÓN: posicion desplaza el cubo en el mundo
 * 2. ROTACIÓN: rotY rota el cubo alrededor del eje Y
 * 3. ESCALA: tamano controla el tamaño del cubo
 * 
 * RENDERIZADO:
 * - Las 6 caras se renderizan como 12 triángulos (2 por cara)
 * - Se aplica backface culling (no se dibujan caras traseras)
 * - Se usa z-buffer para manejar oclusión
 * 
 * USOS EN EL PROYECTO:
 * - Rocas decorativas en el terreno
 * - Voxels de los animales (cada animal es un conjunto de cubos)
 * - Primitiva básica para construcción de objetos complejos
 * 
 * @author Sistema de Ecosistema 3D
 * @version 1.0
 */
public class Cubo implements Renderable, entities.Collidable {
    private Vector3 posicion;  // Centro del cubo en world space
    private int tamano;        // Longitud de cada arista
    private Color color;       // Color RGB del cubo
    private double rotY = 0;   // Rotación alrededor del eje Y (radianes)

    /**
     * Constructor del cubo
     * @param posicion Centro del cubo en coordenadas del mundo
     * @param tamano Longitud de cada arista (todas iguales en un cubo)
     * @param color Color RGB para renderizado
     */
    public Cubo(Vector3 posicion, int tamano, Color color){
        this.posicion = posicion;
        this.tamano = tamano;
        this.color = color;
    }

    /**
     * ========================================================================================
     * getAABBMin - Axis-Aligned Bounding Box (mínimo)
     * ========================================================================================
     * 
     * Calcula la esquina mínima del bounding box para detección de colisiones.
     * 
     * NOTA: AABB ignora rotación (axis-aligned = alineado a ejes), por lo que
     * el bounding box es siempre un cubo alineado a los ejes X/Y/Z.
     * 
     * @return Esquina mínima (x, y, z) del bounding box
     */
    public math.Vector3 getAABBMin(){
        double half = tamano/2.0;
        return new math.Vector3(posicion.x - half, posicion.y - half, posicion.z - half);
    }

    /**
     * getAABBMax - Axis-Aligned Bounding Box (máximo)
     * @return Esquina máxima (x, y, z) del bounding box
     */
    public math.Vector3 getAABBMax(){
        double half = tamano/2.0;
        return new math.Vector3(posicion.x + half, posicion.y + half, posicion.z + half);
    }

    /**
     * ========================================================================================
     * update - Actualiza el estado del cubo (ROTACIÓN AUTOMÁTICA)
     * ========================================================================================
     * 
     * TRANSFORMACIÓN: ROTACIÓN alrededor del eje Y
     * - Incrementa rotY en 0.01 radianes por frame (~143 FPS = ~1.43 rad/s)
     * - Completa una vuelta completa en ~4.4 segundos (2π / 1.43)
     * - Se normaliza a rango [0, 2π] para evitar overflow
     * 
     * Esta rotación hace que los cubos decorativos giren lentamente.
     */
    @Override
    public void update(){
        rotY += 0.01; // ROTACIÓN: incremento constante
        if(rotY > 2*Math.PI) rotY -= 2*Math.PI; // Normalización
    }

    /**
     * ========================================================================================
     * render - Renderiza el cubo en pantalla
     * ========================================================================================
     * 
     * PIPELINE DE RENDERIZADO:
     * 1. getCubeVertices(): Genera 8 vértices con TRASLACIÓN + ROTACIÓN
     * 2. drawCube(): Proyecta vértices y rasteriza 12 triángulos con z-buffer
     * 
     * CONCEPTOS IMPLEMENTADOS:
     * - PRIMITIVA 3D: Cubo geométrico con 8 vértices
     * - TRASLACIÓN: posicion desplaza el cubo
     * - ROTACIÓN: rotY rota alrededor de Y
     * - PROYECCIÓN: De 3D a 2D (dentro de drawCube)
     * - RASTERIZACIÓN: Convierte triángulos a píxeles
     * 
     * @param renderer Motor de renderizado por software
     * @param cam Cámara para transformación de vista
     */
    @Override
    public void render(SoftwareRenderer renderer, Camera cam){
        // 1. Generar vértices con transformaciones (Traslación + Rotación)
        Vector3[] vertices = renderer.getCubeVertices(posicion, tamano, rotY);
        
        // 2. Renderizar cubo (Proyección + Rasterización + Z-buffer)
        renderer.drawCube(vertices, cam, color);
    }
}
