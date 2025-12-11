# GUÍA DE ESTUDIO RÁPIDA (15 MINUTOS)

## LO MÁS IMPORTANTE QUE DEBES SABER

### 1. PIPELINE 3D (FLUJO COMPLETO)
```
Mundo 3D → Transformaciones → Proyección → Pantalla 2D

Detalles:
1. Vértices en coordenadas de objeto (local space)
2. Model Matrix: Rotar + Trasladar → World Space
3. View Matrix: Relativo a cámara → Camera Space
4. Projection: Dividir por Z → Screen Space
5. Rasterización: Llenar píxeles → Framebuffer
```

**Dónde está:** `Transform.project()` hace View + Projection

---

### 2. TRANSFORMACIONES (3 TIPOS)

#### TRASLACIÓN (mover)
```
Matriz:
[ 1  0  0  Tx ]
[ 0  1  0  Ty ]
[ 0  0  1  Tz ]
[ 0  0  0  1  ]
```
**Código:** `Matrix4.translation(x, y, z)`

#### ROTACIÓN (girar)
```
RotY(θ) = rotar alrededor eje Y:
[  cos  0  sin  0 ]
[   0   1   0   0 ]
[ -sin  0  cos  0 ]
[   0   0   0   1 ]
```
**Código:** `Matrix4.rotationY(angle)`
**Uso:** Girar animales hacia dirección de movimiento

#### ESCALA (cambiar tamaño)
```
Multiplicar vértices por factor:
v' = v * scale
```
**Código:** `voxelSize` en animales (crece con fases)

---

### 3. PROYECCIÓN PERSPECTIVA

**Fórmula clave:**
```java
x_screen = (x_camera * fov) / z + width/2
y_screen = height/2 - (y_camera * fov) / z
```

**Por qué se divide entre Z:**
- Z grande (lejos) → división hace número pequeño → cerca del centro
- Z pequeño (cerca) → división hace número grande → lejos del centro
- Simula cómo el ojo humano ve: cosas lejanas se ven pequeñas

**Dónde está:** `Transform.project()` línea 97-99

---

### 4. Z-BUFFER (Resolver qué está adelante)

**Algoritmo:**
```java
float[] zBuffer = new float[width * height];
Arrays.fill(zBuffer, Float.MAX_VALUE);

// Para cada píxel:
if (depth < zBuffer[y * width + x]) {
    setPixel(x, y, color);
    zBuffer[y * width + x] = depth;
}
```

**Por qué funciona:** Solo dibuja si está MÁS CERCA que lo ya dibujado

**Dónde está:** `SoftwareRenderer` (clase render)

---

### 5. DOBLE BUFFER (Evitar parpadeo)

**Problema:** Si dibujas directo en pantalla, usuario ve frame a medias
**Solución:** 
1. Dibujar TODO en buffer oculto (backBuffer)
2. Cuando termina: SWAP (copiar todo de golpe)
3. Usuario siempre ve frame completo

**Código:**
```java
BufferedImage backBuffer; // invisible
// ... dibujar en backBuffer ...
g.drawImage(backBuffer, 0, 0, null); // SWAP
```

**Dónde está:** `RenderPanel`

---

### 6. RASTERIZACIÓN DE TRIÁNGULOS

**Qué es:** Rellenar un triángulo con píxeles

**Algoritmo Scanline:**
1. Ordenar vértices por Y
2. Para cada línea horizontal:
   - Calcular X_inicio y X_fin
   - Rellenar píxeles entre ellos
   - Interpolar profundidad (Z)

**Dónde está:** `SoftwareRenderer.fillTriangle()`

---

### 7. BACKFACE CULLING (No dibujar caras traseras)

**Algoritmo:**
```java
Vector3 normal = (v1 - v0).cross(v2 - v0);
Vector3 toCamera = cameraPos - faceCenter;
if (normal.dot(toCamera) < 0) {
    return; // Cara mira hacia atrás, no dibujar
}
```

**Por qué:** Ahorra 50% de triángulos (solo dibujas caras visibles)

**Dónde está:** En el renderizador, antes de rasterizar

---

### 8. PRIMITIVAS 3D

#### CUBO
- 8 vértices: (±x, ±y, ±z)
- 6 caras × 2 triángulos = 12 triángulos
**Clase:** `entities.Cubo`

#### CILINDRO
- 2 círculos (N vértices cada uno)
- Conectar con triángulos
**Clase:** `entities.Cilindro`

#### TERRENO (Heightmap)
- Grid 2D donde cada celda tiene altura
- Altura = ruido Perlin (aleatorio suave)
**Clase:** `entities.Terreno`

---

### 9. CURVAS 3D

**Paramétrica:**
```java
x(t) = radio * cos(t)
y(t) = constante
z(t) = radio * sin(t)
// t de 0 a 2π genera círculo
```

**Dónde está:** `entities.Curva`

---

### 10. MULTI-THREADING

**3 hilos:**
1. **Main (AWT):** Eventos de UI
2. **RenderThread:** Dibuja ~143 FPS
3. **Simulador:** Evoluciona cada 1 segundo

**Thread-safety:** `synchronized` en Mundo para evitar crashes

---

### 11. PAUSA ABSOLUTA (TU FIX)

**Problema:** Simulador seguía corriendo aunque estuviera pausado

**Solución:**
```java
// En Simulador.run():
if (controles.isPaused() || controles.isAnimalPanelOpen()) {
    continue; // Saltar este tick
}
```

**Resultado:** NADA se mueve cuando pausado (cámara, animales, evolución)

---

## RESPUESTAS RÁPIDAS A PREGUNTAS

**P: ¿Por qué matrices 4x4?**
R: Coordenadas homogéneas (x,y,z,1) permiten hacer traslación con multiplicación

**P: ¿Qué es el producto cruz?**
R: Vector perpendicular a dos vectores. Uso: calcular normales

**P: ¿Cómo se evita división por cero en proyección?**
R: `if (cz == 0) cz = 0.0001;` en Transform.java

**P: ¿Qué es yaw y pitch?**
R: Yaw = rotar horizontal, Pitch = rotar vertical (ángulos de Euler)

**P: ¿Cómo funciona el terreno?**
R: Ruido Perlin genera alturas suaves, cada celda = 2 triángulos

**P: ¿Qué garantiza 3 minutos sin repetir?**
R: Seed fija genera secuencia determinista MUY larga

---

## CÓDIGO CRÍTICO PARA MOSTRAR

### 1. Proyección perspectiva:
```java
// Transform.java línea ~97
double x2d = (cx * fov) / cz + ancho / 2.0;
double y2d = alto / 2.0 - (cy * fov) / cz;
```

### 2. Rotación Y:
```java
// Matrix4.java línea ~27
r.m[0][0] = cos; r.m[0][2] = sin;
r.m[2][0] = -sin; r.m[2][2] = cos;
```

### 3. Z-buffer test:
```java
// SoftwareRenderer.java
if (depth < zBuffer[index]) {
    backBuffer.setRGB(x, y, color);
    zBuffer[index] = depth;
}
```

---

## CONCEPTOS POR ARCHIVO

| Archivo | Concepto Principal |
|---------|-------------------|
| `Transform.java` | Pipeline 3D completo, proyección |
| `Matrix4.java` | Traslación, rotación con matrices |
| `Vector3.java` | Álgebra vectorial, producto cruz/punto |
| `Camera.java` | Coordenadas esféricas, Euler angles |
| `SoftwareRenderer` | Z-buffer, rasterización, backface culling |
| `RenderThread` | Doble buffer, multi-threading |
| `Cubo` | Primitiva 3D simple |
| `Cilindro` | Primitiva 3D con círculos |
| `Terreno` | Superficie 3D, heightmap, Perlin |
| `Simulador` | Animación determinista, tu fix de pausa |

---

## PARA IMPRESIONAR A LA MAESTRA

Explica el pipeline completo:
1. "Los vértices empiezan en local space de cada objeto"
2. "La model matrix los rota y traslada a world space"
3. "La view matrix los transforma relativos a la cámara"
4. "La proyección perspectiva divide por Z"
5. "El z-buffer resuelve qué pixel está adelante"
6. "La rasterización rellena píxeles con interpolación"
7. "El doble buffer muestra todo sin parpadeo"

Menciona que TODO se hace píxel por píxel (setRGB), sin Graphics2D.

---

**ESTUDIA ESTO EN 15 MIN Y ESTARÁS LISTA** ✓
