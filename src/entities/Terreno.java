package entities;

import main.Renderable;
import render.SoftwareRenderer;
import math.Vector3;
import math.Camera;
import java.awt.Color;
import java.util.Random;

/**
 * ============================================================================================
 * TERRENO - PRIMITIVA 3D: MALLA (GRID/MESH) CON HEIGHTMAP
 * ============================================================================================
 * 
 * Esta clase implementa el requisito de "TERRENO" como primitiva geométrica basada en MALLA.
 * 
 * DEFINICIÓN GEOMÉTRICA:
 * Un terreno es una SUPERFICIE 3D representada por una MALLA (grid) de vértices:
 * - Grid 2D: width × depth puntos de muestreo
 * - Heightmap: heights[x][z] = elevación Y en cada punto
 * - Triangulación: Cada celda se divide en 2 triángulos
 * 
 * ESTRUCTURA TOPOLÓGICA:
 * Para un grid de (width × depth) vértices:
 * - Número de vértices: width * depth
 * - Número de celdas: (width-1) * (depth-1)
 * - Número de triángulos: 2 * (width-1) * (depth-1)
 * 
 * Cada celda forma un quad dividido en 2 triángulos:
 *   (x,z) -------- (x+1,z)
 *     |    \          |
 *     |      \        |
 *     |        \      |
 *     |          \    |
 *   (x,z+1) ---- (x+1,z+1)
 * 
 * Triángulo 1: (x,z), (x+1,z+1), (x,z+1)
 * Triángulo 2: (x,z), (x+1,z), (x+1,z+1)
 * 
 * GENERACIÓN DE ALTURAS:
 * En este proyecto el terreno es PLANO (requisito específico):
 * - heights[x][z] = 0.0 para todo (x,z)
 * - No hay montes ni valles
 * - Esto simplifica colisiones (todos los animales caminan en Y=0)
 * 
 * RENDERIZADO:
 * - Grid ultra-denso: 50x50 celdas = 2500 quads = 5000 triángulos
 * - Área: 800x800 unidades
 * - Tamaño de celda: 16 unidades
 * - Se usa caché de proyección para optimizar (cada vértice se proyecta 1 sola vez)
 * 
 * CONCEPTOS IMPLEMENTADOS:
 * - PRIMITIVA 3D: Malla triangular (grid topology)
 * - HEIGHTMAP: Matriz 2D de elevaciones
 * - TRIANGULACIÓN: Conversión de quads a triángulos
 * - PROYECCIÓN: Transformación 3D → 2D
 * - CULLING: No se renderizan triángulos fuera de cámara
 * 
 * @author Sistema de Ecosistema 3D
 * @version 1.0
 */
public class Terreno implements Renderable, HeightProvider {
    private int width, depth;
    private double scale;
    private double[][] heights;
    private Color baseColor;
    private double terrainOffset = -0.01; // shared offset applied to rendered vertices and height queries

    public Terreno(int width, int depth, double scale, long seed, Color baseColor){
        this.width = Math.max(2, width);
        this.depth = Math.max(2, depth);
        this.scale = scale;
        this.baseColor = baseColor;
        this.heights = new double[this.width][this.depth];
        generateHeights(seed);
    }

    /**
     * ========================================================================================
     * generateHeights - Genera el heightmap del terreno
     * ========================================================================================
     * 
     * ALGORITMO: Terreno plano (requisito del proyecto)
     * - Todas las alturas son 0.0 (plano horizontal en Y=0)
     * - No hay variación topográfica (montes/valles)
     * 
     * ALTERNATIVAS COMUNES (no usadas aquí):
     * - Perlin Noise: Genera terreno orgánico con colinas suaves
     * - Diamond-Square: Algoritmo fractal para terreno procedural
     * - Heightmap texture: Cargar alturas desde imagen en escala de grises
     * 
     * @param seed Semilla para generación determinista (no usada en terreno plano)
     */
    private void generateHeights(long seed){
        // For this project requirement the terrain must be completely flat
        // (no montes ni valles). Set every sample to a constant elevation.
        double flatHeight = 0.0; // world Y coordinate for the flat plane
        
        // Inicializar heightmap: todos los puntos en Y=0
        for(int x=0;x<width;x++){
            for(int z=0;z<depth;z++){
                heights[x][z] = flatHeight; // TERRENO PLANO
            }
        }
    }

    /**
     * update - No requiere actualización (terreno estático)
     */
    @Override
    public void update(){ }

    /**
     * ========================================================================================
     * render - Renderiza la MALLA del terreno
     * ========================================================================================
     * 
     * ALGORITMO DE RENDERIZADO:
     * 1. Genera grid de 50×50 = 2500 celdas (5000 triángulos)
     * 2. OPTIMIZACIÓN: Pre-proyecta todos los vértices y los cachea
     * 3. Para cada celda: renderiza 2 triángulos
     * 
     * ESTRUCTURA DEL GRID:
     * - Área total: 800×800 unidades
     * - Tamaño de celda: 16 unidades
     * - Número de celdas: 50×50
     * - Vértices totales: 51×51 = 2601
     * - Triángulos totales: 2 × 50 × 50 = 5000
     * 
     * TOPOLOGÍA:
     * Cada celda (ix, iz) tiene 4 esquinas:
     *   (ix, iz) ----------- (ix+1, iz)
     *      |                     |
     *      |                     |
     *   (ix, iz+1) --------- (ix+1, iz+1)
     * 
     * Se divide en 2 triángulos compartiendo diagonal.
     * 
     * CACHÉ DE PROYECCIÓN:
     * - Evita proyectar el mismo vértice múltiples veces
     * - Key: "ix,iz" → Value: [sx, sy, cz] (screen x, screen y, camera z)
     * - Mejora rendimiento ~4x (cada vértice compartido por 2-6 triángulos)
     * 
     * @param renderer Motor de renderizado por software
     * @param cam Cámara para transformación de vista y proyección
     */
    @Override
    public void render(SoftwareRenderer renderer, Camera cam){
        double gridSize = 800;    // Dimensión total del grid
        double cellSize = 16;     // Tamaño de cada celda (16×16 unidades)
        int cells = 50;           // Número de celdas por dimensión (50×50)
        
        // Color oscurecido para líneas del grid
        Color col = new Color(
            Math.max(0, baseColor.getRed() - 20),
            Math.max(0, baseColor.getGreen() - 20),
            Math.max(0, baseColor.getBlue() - 20)
        );
        
        double y = 0.0; // Y constante (terreno plano)
        
        // OPTIMIZACIÓN: Caché de proyección (evita re-proyectar vértices compartidos)
        java.util.HashMap<String, double[]> projCache = new java.util.HashMap<>();
        
        // FASE 1: Pre-proyectar TODOS los vértices del grid
        for(int ix = 0; ix <= cells; ix++) {
            for(int iz = 0; iz <= cells; iz++) {
                double x = -gridSize/2 + ix*cellSize; // Centrar grid en origen
                double z = -gridSize/2 + iz*cellSize;
                String key = ix + "," + iz;
                Vector3 v = new Vector3(x, y, z);
                double[] proj = renderer.project(v, cam);
                projCache.put(key, proj);
            }
        }
        
        for(int ix = 0; ix < cells; ix++) {
            for(int iz = 0; iz < cells; iz++) {
                String k00 = ix + "," + iz;
                String k10 = (ix+1) + "," + iz;
                String k01 = ix + "," + (iz+1);
                String k11 = (ix+1) + "," + (iz+1);
                
                double[] p00 = projCache.get(k00);
                double[] p10 = projCache.get(k10);
                double[] p01 = projCache.get(k01);
                double[] p11 = projCache.get(k11);
                
                if (p00 != null && p10 != null && p01 != null && p11 != null) {
                    renderer.drawQuadScreen(p00, p10, p11, p01, col);
                }
            }
        }
    }

    @Override
    public double getHeightAt(double worldX, double worldZ){
        // Return height including the terrainOffset so physics/collisions match rendered surface.
        double fx = worldX / scale + width/2.0;
        double fz = worldZ / scale + depth/2.0;
        int x0 = (int)Math.floor(fx);
        int z0 = (int)Math.floor(fz);
        if(x0 < 0) x0 = 0; if(z0 < 0) z0 = 0;
        if(x0 >= width-1) x0 = width-2; if(z0 >= depth-1) z0 = depth-2;
        double sx = fx - x0;
        double sz = fz - z0;
        double h00 = heights[x0][z0];
        double h10 = heights[x0+1][z0];
        double h01 = heights[x0][z0+1];
        double h11 = heights[x0+1][z0+1];
        double hx0 = h00*(1-sx) + h10*sx;
        double hx1 = h01*(1-sx) + h11*sx;
        return hx0*(1-sz) + hx1*sz + terrainOffset;
    }
}
