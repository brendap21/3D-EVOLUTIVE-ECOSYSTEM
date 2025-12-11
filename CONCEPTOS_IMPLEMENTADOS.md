# CONCEPTOS DE GRÁFICAS POR COMPUTADORA IMPLEMENTADOS

## ÍNDICE RÁPIDO PARA REVISIÓN (1 HORA)
Este documento resume TODOS los conceptos implementados en el proyecto. Cada sección referencia las clases donde se implementa.

---

## 1. TRANSFORMACIONES 3D ✓

### TRASLACIÓN (Translation)
**Clase:** `math.Matrix4.translation()`, `math.Transform.java`
**Concepto:** Desplazamiento de puntos en el espacio 3D
**Fórmula:** P' = P + T donde T = (Tx, Ty, Tz)
**Implementación:** Matriz 4x4 con offset en última columna
**Uso:** Posicionar entidades en el mundo

### ROTACIÓN (Rotation)
**Clases:** `math.Matrix4.rotationY()`, `math.Matrix4.rotationX()`, `math.Camera.rotate()`
**Conceptos:**
- Rotación alrededor eje Y (Yaw): Girar animales horizontalmente
- Rotación alrededor eje X (Pitch): Inclinar cámara arriba/abajo
- Ángulos de Euler: Yaw (guiñada), Pitch (cabeceo)
- Coordenadas esféricas → cartesianas

**Fórmulas:**
```
RotY(θ):
[ cos(θ)   0   sin(θ) ]
[   0      1     0    ]
[-sin(θ)   0   cos(θ) ]

RotX(θ):
[ 1     0        0    ]
[ 0  cos(θ)  -sin(θ) ]
[ 0  sin(θ)   cos(θ) ]
```

**Uso:** 
- Rotar animales hacia dirección de movimiento
- Controles FPS de cámara (mouse look)

### ESCALA (Scale)
**Clase:** `entities.BaseAnimal` (voxelSize), entidades con tamaño variable
**Concepto:** Cambiar tamaño de objetos sin cambiar posición
**Implementación:** Multiplicar coordenadas de vértices por factor de escala
**Uso:** Crecimiento de animales (fases bebé/joven/adulto)

---

## 2. PROYECCIONES ✓

### PROYECCIÓN PERSPECTIVA (Perspective Projection)
**Clase:** `math.Transform.project()`
**Concepto:** Objetos lejanos se ven más pequeños (realista)
**Fórmula:** 
```
x_screen = (x_camera * fov) / z + width/2
y_screen = height/2 - (y_camera * fov) / z
```
**Explicación:** División por profundidad (z) causa que objetos lejanos sean pequeños
**Uso:** Modo por defecto del juego (realismo)

### PROYECCIÓN PARALELA (Orthographic)
**Clase:** `math.Transform.project()` con flag `cam.isOrthographic()`
**Concepto:** Distancia no afecta tamaño (proyección paralela)
**Fórmula:**
```
x_screen = x_camera * fov + width/2
y_screen = height/2 - y_camera * fov
```
**Explicación:** Sin división por z → objetos mantienen tamaño
**Uso:** Modo alternativo (cambiar con tecla en Controles si se implementó)

---

## 3. PRIMITIVAS 3D ✓

### CUBOS (Voxels)
**Clases:** `entities.Cubo`, todos los animales (hechos de cubos)
**Concepto:** Poliedro de 6 caras cuadradas (8 vértices, 12 aristas)
**Implementación:** 
- 8 vértices: combinaciones de ±size en X, Y, Z
- 6 caras: 2 triángulos por cara = 12 triángulos total
**Uso:** Base del diseño voxel (todo está hecho de cubos)

### CILINDROS
**Clases:** `entities.Cilindro`, `entities.Arbol` (tronco)
**Concepto:** Superficie de revolución con base circular
**Implementación:**
- Círculos superior e inferior (N vértices cada uno)
- Conectar vértices con triángulos (2N triángulos laterales + 2N en tapas)
**Parámetros:** radio, altura, segmentos (resolución)
**Uso:** Troncos de árboles, estructuras cilíndricas

### SUPERFICIES 3D (Terreno)
**Clase:** `entities.Terreno`
**Concepto:** Heightmap (mapa de alturas) - grid 2D donde cada celda tiene altura
**Implementación:**
- Grid MxN de puntos en plano XZ
- Altura Y calculada con ruido Perlin (aleatorio suave)
- Cada celda = 2 triángulos formando un quad
**Algoritmo Perlin Noise:** Genera alturas aleatorias pero continuas (sin saltos bruscos)
**Uso:** Terreno procedimental del mundo

---

## 4. CURVAS 3D ✓

### CURVAS PARAMÉTRICAS
**Clase:** `entities.Curva`
**Concepto:** Trayectoria 3D definida por función paramétrica P(t)
**Fórmula ejemplo (lemniscata):**
```
x(t) = radio * cos(t) / (1 + sin²(t))
y(t) = nivel_base
z(t) = radio * sin(t) * cos(t) / (1 + sin²(t))
```
**Implementación:** Evaluar P(t) para t ∈ [0, 2π] con muchos puntos
**Renderizado:** Líneas conectando puntos consecutivos
**Uso:** Animación de animales siguiendo trayectorias

---

## 5. DOBLE BUFFER (Double Buffering) ✓

**Clases:** `main.RenderPanel`, `main.RenderThread`
**Concepto:** Evitar parpadeo (flickering) dibujando en buffer oculto
**Algoritmo:**
1. Dibujar escena completa en backBuffer (BufferedImage oculto)
2. Cuando termina, hacer SWAP: copiar backBuffer a pantalla
3. Repetir cada frame

**Implementación:**
```java
// RenderPanel.java
BufferedImage backBuffer = new BufferedImage(width, height, TYPE_INT_RGB);
Graphics2D g2d = (Graphics2D) backBuffer.getGraphics();
// ... dibujar todo en g2d ...
getGraphics().drawImage(backBuffer, 0, 0, null); // SWAP
```

**Ventaja:** Usuario nunca ve frame parcial (sin parpadeo)
**Uso:** Todo el renderizado usa este patrón

---

## 6. Z-BUFFER (Depth Testing) ✓

**Clase:** `render.SoftwareRenderer`
**Concepto:** Resolver visibilidad (qué pixel está adelante)
**Algoritmo:**
1. Crear array `float[] zBuffer` del tamaño de la pantalla (width * height)
2. Inicializar con Float.MAX_VALUE (infinito)
3. Para cada pixel a dibujar:
   - Si su profundidad < zBuffer[x, y] → dibujar y actualizar zBuffer
   - Sino → descartar (hay algo más cerca)

**Fórmula:** `index = y * width + x` (convertir 2D a 1D)

**Implementación:**
```java
if (depth < zBuffer[screenY * width + screenX]) {
    backBuffer.setRGB(screenX, screenY, color);
    zBuffer[screenY * width + screenX] = (float)depth;
}
```

**Ventaja:** Dibuja correctamente sin ordenar triángulos
**Uso:** Resuelve solapamiento de objetos

---

## 7. RASTERIZACIÓN DE TRIÁNGULOS ✓

**Clase:** `render.SoftwareRenderer.fillTriangle()`
**Concepto:** Convertir triángulo (geometría) a píxeles (rasterización)
**Algoritmo Scanline:**
1. Ordenar vértices por Y: (x0,y0), (x1,y1), (x2,y2) donde y0 ≤ y1 ≤ y2
2. Dividir triángulo en 2 partes: superior (y0→y1) e inferior (y1→y2)
3. Para cada línea horizontal (scanline) en Y:
   - Interpolar X de borde izquierdo e X de borde derecho
   - Rellenar píxeles entre X_izq y X_der
   - Interpolar profundidad (z) para z-test

**Interpolación lineal:**
```
z(t) = z0 + t * (z1 - z0)  donde t ∈ [0, 1]
```

**Uso:** Rellenar todas las caras de cubos, cilindros, terreno

---

## 8. BACKFACE CULLING ✓

**Clase:** `render.SoftwareRenderer` (en método de render de caras)
**Concepto:** No dibujar caras que miran HACIA ATRÁS (no visibles)
**Algoritmo:**
1. Calcular normal de la cara: N = (V1-V0) × (V2-V0)
2. Calcular vector de vista: V = Posición_Cámara - Centro_Cara
3. Si dot(N, V) < 0 → cara mira hacia cámara (visible)
4. Si dot(N, V) > 0 → cara mira hacia atrás (descartar)

**Fórmula producto punto:**
```
dot(A, B) = Ax*Bx + Ay*By + Az*Bz
```

**Ventaja:** Reduce a la mitad los triángulos a dibujar (mejora rendimiento)
**Uso:** Automático en el renderizador

---

## 9. SOMBREADO (Shading) ✓

**Clase:** `render.SoftwareRenderer` (cálculo de iluminación)
**Concepto:** Simular iluminación para dar profundidad visual
**Modelo Lambert (Diffuse Lighting):**
```
Intensidad = max(0, dot(Normal, Luz))
```

**Explicación:**
- Si cara apunta hacia luz → brillante (dot = 1)
- Si cara perpendicular a luz → semibrillante (dot = 0)
- Si cara opuesta a luz → oscura (dot < 0 → clamp a 0)

**Implementación:**
```java
Vector3 lightDir = new Vector3(1, -1, 0.5).normalize();
double intensity = Math.max(0.2, dot(normal, lightDir));
Color shadedColor = new Color(
    (int)(baseColor.getRed() * intensity),
    (int)(baseColor.getGreen() * intensity),
    (int)(baseColor.getBlue() * intensity)
);
```

**Uso:** Todas las caras tienen iluminación Lambert

---

## 10. ANIMACIÓN DETERMINISTA (3 MINUTOS) ✓

**Clases:** `simulation.Simulador`, `entities.BaseAnimal`
**Concepto:** Animación reproducible usando seed fija
**Algoritmo:**
1. Simulador usa `Random(seed)` → secuencia predecible
2. Cada tick (1 segundo): seleccionar animal con índice = (seed + tick) % cantidad
3. Evolucionar animal (mutación, crecimiento)
4. Tick avanza → secuencia continúa

**Propiedades:**
- Misma seed → misma secuencia de eventos
- Animación corre >3 minutos sin repetir exacto
- Determinismo útil para debugging

**Uso:** Evolución de animales, crecimiento de plantas

---

## 11. MULTI-THREADING ✓

**Clases:** `main.RenderThread`, `simulation.Simulador`
**Concepto:** Separar lógica en hilos independientes
**Hilos implementados:**
1. **Event Dispatch Thread (AWT):** Manejo de UI (automático)
2. **RenderThread:** Dibuja escena ~143 FPS (sleep 7ms)
3. **Simulador:** Actualiza lógica cada 1 segundo

**Sincronización:**
```java
public synchronized void addEntity(Renderable e) {
    entidades.add(e);
}
```

**Thread-safety:**
- Métodos `synchronized` en Mundo
- Copias defensivas (snapshots) para iteración
- Evita ConcurrentModificationException

---

## 12. SISTEMA DE COLISIONES ✓

**Clases:** `entities.Collidable`, `ui.Controles.actualizar()`
**Concepto:** Detectar y resolver solapamiento
**Método:** AABB (Axis-Aligned Bounding Box) vs Esfera

**Algoritmo Sphere-AABB:**
1. Obtener AABB de objeto: min=(xMin,yMin,zMin), max=(xMax,yMax,zMax)
2. Encontrar punto más cercano del AABB a la esfera:
   ```
   closestX = clamp(sphereX, xMin, xMax)
   closestY = clamp(sphereY, yMin, yMax)
   closestZ = clamp(sphereZ, zMin, zMax)
   ```
3. Calcular distancia: dist² = (sphereX-closestX)² + ...
4. Si dist² < radius² → colisión

**Uso:** Cámara no atraviesa árboles/rocas

---

## 13. HEIGHTMAP QUERIES ✓

**Clases:** `entities.Terreno.getHeightAt()`, `simulation.Mundo.getHeightAt()`
**Concepto:** Consultar altura del terreno en coordenadas (X, Z)
**Algoritmo:**
1. Convertir (X, Z) mundial a coordenadas de grid
2. Obtener altura interpolada (bilinear interpolation)
3. Retornar Y del terreno

**Uso:** Animales caminan sobre terreno (no flotan ni se hunden)

---

## 14. PAUSA ABSOLUTA ✓

**Clases:** `ui.Controles`, `simulation.Simulador`, `main.RenderThread`
**Concepto:** Congelar completamente el juego
**Implementación:**
```java
// RenderThread
if (controles.isPaused() || controles.isAnimalPanelOpen()) {
    // NO actualizar controles
    // NO actualizar entidades
    // SÍ seguir dibujando (para mostrar menú)
}

// Simulador
if (controles.isPaused() || controles.isAnimalPanelOpen()) {
    continue; // Saltar tick sin modificar nada
}
```

**Resultado:** NADA se mueve cuando está pausado

---

## 15. PÍXEL POR PÍXEL (Software Rendering) ✓

**Clase:** `render.SoftwareRenderer`
**Concepto:** TODO se dibuja con `BufferedImage.setRGB(x, y, color)`
**Prohibido:** `Graphics.drawLine()`, `fillRect()`, etc.

**Implementado manualmente:**
- Líneas (algoritmo DDA o Bresenham)
- Triángulos (scanline fill)
- Texto (PixelFont con bitmap)

---

## FLUJO COMPLETO DE RENDERIZADO

```
1. RenderThread.run() [~143 FPS]
   ↓
2. Para cada entidad en Mundo.snapshotEntities():
   ↓
3. entity.render(renderer, cam)
   ↓
4. Para cada cubo/cilindro de la entidad:
   ↓
5. Transformar vértices:
   a) Model Matrix: Traslación + Rotación
   b) View Matrix: Relativo a cámara
   c) Projection Matrix: 3D → 2D
   ↓
6. Backface Culling (descartar caras traseras)
   ↓
7. Para cada triángulo visible:
   a) Calcular normal e iluminación
   b) Rasterizar con scanline
   c) Z-test para cada pixel
   d) setRGB si pasa z-test
   ↓
8. Dibujar HUD (texto, crosshair, menús)
   ↓
9. SWAP buffers (mostrar backBuffer)
```

---

## CÓMO LEER EL CÓDIGO (ORDEN SUGERIDO)

### Empezar aquí (fundamentos):
1. `math.Vector3` → Operaciones vectoriales básicas
2. `math.Matrix4` → Transformaciones con matrices
3. `math.Camera` → Sistema de coordenadas de cámara
4. `math.Transform` → Pipeline 3D completo

### Renderizado (núcleo visual):
5. `main.EcosistemaApp` → Inicialización
6. `main.RenderThread` → Bucle de render
7. `render.SoftwareRenderer` → Rasterización píxel por píxel
8. `entities.Cubo` → Ejemplo de primitiva simple

### Simulación (lógica):
9. `simulation.Mundo` → Contenedor de entidades
10. `simulation.Simulador` → Evolución determinista
11. `entities.BaseAnimal` → Comportamiento de animales

### Avanzado:
12. `ui.Controles` → Input handling (FPS controls)
13. `entities.Terreno` → Heightmap con Perlin noise
14. `entities.Cilindro` → Geometría procedural

---

## PREGUNTAS FRECUENTES DE LA MAESTRA

**P: ¿Cómo se implementó la proyección perspectiva?**
R: División por profundidad en `Transform.project()`: `x2d = (cx * fov) / cz`

**P: ¿Cómo funciona el Z-buffer?**
R: Array de profundidades, se compara y actualiza por cada pixel en `SoftwareRenderer`

**P: ¿Qué transformaciones implementaron?**
R: Traslación, rotación (X, Y), escala. Codificadas en matrices 4x4 con coordenadas homogéneas.

**P: ¿Cómo evitan parpadeo?**
R: Doble buffer: dibujar en backBuffer, luego swap atómico a pantalla.

**P: ¿Dónde está el backface culling?**
R: En `SoftwareRenderer`, calcula `dot(normal, viewVector)` y descarta si < 0.

**P: ¿Cómo se generan los cilindros?**
R: Dos círculos (N vértices) conectados con triángulos, ver `Cilindro.java`

**P: ¿Cómo funciona el terreno?**
R: Heightmap generada con ruido Perlin, grid 2D con alturas variables.

**P: ¿Por qué matrices 4x4 y no 3x3?**
R: Coordenadas homogéneas permiten representar traslación con multiplicación.

**P: ¿Cómo garantizan 3 minutos sin repetición?**
R: Seed fija + contador de ticks → secuencia determinista muy larga.

**P: ¿Qué es el producto cruz y dónde se usa?**
R: Vector perpendicular a dos vectores. Uso: calcular normales y base de cámara (right).

---

## GLOSARIO DE TÉRMINOS CLAVE

- **Voxel:** Cubo 3D (píxel volumétrico)
- **Rasterización:** Convertir geometría a píxeles
- **Pipeline:** Secuencia de transformaciones (Model → View → Projection → Screen)
- **Normal:** Vector perpendicular a una superficie
- **Homogéneas:** Coordenadas 4D (x,y,z,w) para representar traslación
- **Ortonormal:** Base de 3 vectores perpendiculares y unitarios
- **Heightmap:** Mapa 2D de alturas (terreno)
- **Interpolación:** Calcular valores intermedios entre dos puntos
- **Scanline:** Línea horizontal en rasterización de triángulos
- **Determinista:** Resultado predecible con misma seed

---

ESTE ARCHIVO RESUME TODO EL PROYECTO. CADA CONCEPTO ESTÁ IMPLEMENTADO Y DOCUMENTADO EN LAS CLASES REFERENCIADAS.
