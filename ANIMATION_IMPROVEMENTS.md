# MEJORAS DE ANIMACIONES - 10 TIPOS DE ANIMALES

Documento de referencia para las nuevas animaciones implementadas en cada tipo de animal.
Cada animal ahora cuenta con un `AnimationController` para manejar animaciones secundarias complejas.

---

## ARQUITECTURA DE ANIMACIONES

### AnimationController
Clase centralizada que proporciona:
- **Blink** (Parpadeo): Ojos que se cierran naturalmente
- **Jaw Movement** (Movimiento de mandíbula): Abrir/cerrar boca
- **Tail Wag** (Meneo de cola): Movimiento ondulante
- **Wing Flap** (Bateo de alas): Movimiento alar coordinado
- **Head Turn** (Giro de cabeza): Movimiento lateral de cabeza
- **Ear Twitch** (Movimiento de orejas): Pequeños movimientos de orejas
- **Body Sway** (Balanceo de cuerpo): Movimiento natural corporal
- **Idle Bounce** (Rebote en reposo): Pequeño salto vertical

Cada método devuelve un valor numérico que se puede aplicar a posiciones/rotaciones para crear movimiento suave.

---

## ANIMAL TYPE 01 - FELINO ÁGIL (Cuadrúpedo)
**Características:** Ágil, cazador, felino
**Colores:** Teal, Menta, Azul verdoso
**Velocidad:** Rápida (1.6 - 2.2)

### ANIMACIONES IMPLEMENTADAS:
1. **Caminar Cuadrúpedo Realista**
   - Patas diagonales coordinadas: (FL, BR) y (FR, BL)
   - Movimiento alternado suave
   - Articulation effect: Estiramiento/compresión de rodillas
   - Diferencia de altura mínima durante el movimiento

2. **Movimiento de Cabeza**
   - Sigue la dirección del movimiento
   - Pequeño giro lateral

3. **Cola Ondulante**
   - Meneo lateral con desplazamiento horizontal
   - Movimiento vertical sincronizado
   - Amplitud aumenta con fases de crecimiento

4. **Parpadeo Natural**
   - Frecuencia: 2.5 Hz (~parpadeos normales)
   - Patrón: Cierre rápido, apertura lenta
   - Reduce pupila cuando se cierra

5. **Movimiento de Orejas**
   - Reaccionan al movimiento
   - Frecuencia: 4 Hz
   - Movimiento suave y sensible

### CÁLCULOS UTILIZADOS:
```
Articulation: sin(walkPhase * 2) * 0.2
Ear rotation: animController.getEarTwitch(earIndex)
Tail offset: sin(tailWagPhase + segmentIndex * 0.4) * amplitude
Blink amount: Pattern de sin() con fase especial
```

---

## ANIMAL TYPE 02 - BÍPEDO COMPACTO
**Características:** Vívido, expresivo, erguido
**Colores:** Naranja fuego, Rojo coral, Amarillo quemado
**Velocidad:** Media (1.3 - 1.8)

### ANIMACIONES IMPLEMENTADAS:
1. **Marcha Bípeda**
   - Patas alternas suave
   - Balanceo corporal
   - Efecto de articulación de rodillas

2. **Cresta Defensiva**
   - Se alza cuando camina
   - Movimiento ascendente rítmico
   - Amplitud: 0.3 * voxelSize

3. **Brazos/Alas Coordinados**
   - Oscilan opuestos a las patas para balance
   - Movimiento fluido arriba/abajo
   - Sincronización con marcha

4. **Pico Articulado**
   - Abre y cierra rítmicamente
   - Movimiento de mandíbula: `getJawOpen()`
   - Tamaño varía con apertura

5. **Parpadeo Expresivo**
   - Ojos se cierran completamente
   - Transmite expresión facial

### CÁLCULOS UTILIZADOS:
```
Body sway: sin(bodySwayPhase) * 0.15 * voxelSize
Crest rise: abs(sin(animPhase)) * 0.3 * voxelSize
Jaw opening: getJawOpen() con factor de 0.3
```

---

## ANIMAL TYPE 03 - CUADRÚPEDO PESADO (Herbívoro)
**Características:** Robusto, defensivo, pesado
**Colores:** Púrpura oscuro, Violeta, Azul profundo
**Velocidad:** Lenta (0.9 - 1.3)

### ANIMACIONES IMPLEMENTADAS:
1. **Marcha Pesada y Deliberada**
   - Movimiento lento y calculado
   - Amplitud de paso reducida
   - Aspecto de criatura deliberada

2. **Movimiento de Cabeza Defensiva**
   - Giro lateral para evaluar amenazas
   - Frecuencia baja (1.0 Hz)
   - Amplitud: 0.25 radianes

3. **Cola Gruesa Balanceándose**
   - Movimiento lento y pesado
   - Sincronizado con paso
   - Comunica poder

4. **Vibración de Cuernos**
   - Pequeños temblores defensivos
   - Se alternan durante el movimiento
   - Comunica agresividad potencial

5. **Movimiento Robusto**
   - Sin parpadeos frecuentes
   - Ojos abiertos vigilantes

### CÁLCULOS UTILIZADOS:
```
Leg bend: sin(animPhase * 2) * 0.15
Tail wag: Lower frequency for heaviness
Articulation: More pronounced for heavy movement
```

---

## ANIMAL TYPE 04 - SERPENTINO (Alargado)
**Características:** Fluido, misterioso, sinuoso
**Colores:** Amarillo lima, Verde limón, Chartreuse
**Velocidad:** Media-Alta (1.4 - 1.9)

### ANIMACIONES IMPLEMENTADAS:
1. **Ondulación Fluida de Segmentos**
   - Propagación de onda a lo largo del cuerpo
   - Cada segmento se desfasa ligeramente
   - Crea movimiento sinuoso natural

2. **Movimiento Horizontal y Vertical**
   - Offset lateral: `sin(phase + segmentIndex * 0.4)`
   - Offset vertical: `sin(phase + offset) * amplitude * 0.5`
   - Crea efecto de nado o deslizamiento

3. **Cambio de Color Rítmico**
   - Pulso de color sincronizado
   - Comunica estado vital

4. **Parpadeo Hipnótico**
   - Frecuencia variable
   - Ojos que se cierran lentamente

### CÁLCULOS UTILIZADOS:
```
Undulation: sin(undulatePhase + segmentIndex * 0.5) * amplitude
Vertical wave: sin(undulatePhase + segmentIndex * 0.3) * amplitude * 0.5
Color pulse: sin(undulatePhase) applied to RGB
```

---

## ANIMAL TYPE 05 - ESPINOSO DEFENSIVO (Cuadrúpedo)
**Características:** Defensivo, reactivo, púas
**Colores:** Rojo ladrillo, Carmesí oscuro, Burdeos
**Velocidad:** Lenta-Media (1.1 - 1.6)

### ANIMACIONES IMPLEMENTADAS:
1. **Vibración Defensiva de Espinas**
   - Temblores rápidos cuando se mueve
   - Comunica peligro
   - Patrón de vibración aleatorio

2. **Movimiento Reactivo de Patas**
   - Responde a cambios de velocidad
   - Postura defensiva

3. **Parpadeo Defensivo**
   - Ojos recelosos
   - Parpadeo menos frecuente que felino

4. **Púas que se Erizan**
   - Aumentan en tamaño durante movimiento
   - Efecto visual de defensa

### CÁLCULOS UTILIZADOS:
```
Spike vibration: High frequency tremor
Spine raise: Based on acceleration
Articulation: Defensive stance effect
```

---

## ANIMAL TYPE 06 - RADIANTE (Bípedo)
**Características:** Luminoso, articulado, extraño
**Colores:** Azul cielo, Cian brillante, Turquesa
**Velocidad:** Rápida (1.7 - 2.3)

### ANIMACIONES IMPLEMENTADAS:
1. **Pulso Luminoso Rítmico**
   - Color oscila entre claro y oscuro
   - Comunica bioluminiscencia
   - Sincronizado con movimiento

2. **Movimiento Fluido de Antenas**
   - Wobble suave
   - Cada antenna tiene fase propia
   - Exploración del ambiente

3. **Parpadeo Sincronizado**
   - Ambos ojos parpadean juntos
   - Patrón hipnótico

4. **Brazos Articulados Fluidos**
   - Movimiento pendular
   - Sincronizado con antenas

### CÁLCULOS UTILIZADOS:
```
Pulse: sin(pulsePhase * 2) * 0.2 + 0.8
Antenna wobble: sin(pulsePhase + antennaIndex * 0.5) * amplitude
Synchronization: All based on pulsePhase
```

---

## ANIMAL TYPE 07 - INSECTOIDE (6 Patas)
**Características:** Insectoide, mandíbulas, múltiples patas
**Colores:** Verde bosque, Esmeralda, Verde musgo
**Velocidad:** Media (1.3 - 1.8)

### ANIMACIONES IMPLEMENTADAS:
1. **Coordinación de 6 Patas (Tripod Gait)**
   - Patrón: (L1,R2,L3) vs (R1,L2,R3)
   - Simulación de insecto real
   - Movimiento muy realista

2. **Movimiento de Mandíbulas**
   - Abre/cierra sincrónizado con movimiento
   - Comunica intención de ataque

3. **Antenas Sinuosas**
   - Se mueven independientemente
   - Exploración del entorno
   - Frecuencia: 3.5 Hz

4. **Ojos Compuestos Parpadeantes**
   - Parpadeo ligeramente más rápido

### CÁLCULOS UTILIZADOS:
```
Tripod gait: walkPhase para L1,R2,L3 vs R1,L2,R3
Mandible: getJawOpen() con factor específico
Antenna: sin(walkPhase + antennaIndex * 0.7) * amplitude
```

---

## ANIMAL TYPE 08 - ACORAZADO (Pesado)
**Características:** Blindado, pesado, defensivo
**Colores:** Marrón tierra, Óxido, Café oscuro
**Velocidad:** Lenta (0.8 - 1.2)

### ANIMACIONES IMPLEMENTADAS:
1. **Movimiento Lento y Pesado**
   - Cada paso es deliberado
   - Poco movimiento vertical
   - Proyecta poder

2. **Rotación de Cabeza Defensiva**
   - Explora el ambiente lentamente
   - Frecuencia muy baja: 0.8 Hz
   - Amplitud controlada

3. **Vibración de Placas Protectoras**
   - Pequeños temblores en placas
   - Comunica solidez
   - Patrón sinuoso a lo largo del cuerpo

4. **Postura Protectora**
   - Cuerpo bajo
   - Movimiento mínimo

### CÁLCULOS UTILIZADOS:
```
Plate vibration: Frequency high, amplitude low
Head rotation: Very slow and deliberate
Body height: Minimal variation
```

---

## ANIMAL TYPE 09 - SALTADOR (Ágil)
**Características:** Saltador, ágil, expresivo
**Colores:** Rosa claro, Fucsia, Magenta
**Velocidad:** Rápida (1.6 - 2.2)

### ANIMACIONES IMPLEMENTADAS:
1. **Saltos Realistas**
   - Preparación (compresión)
   - Impulso (extensión rápida)
   - Aterrizaje (absorción)
   - Patrón sinuoso completo

2. **Movimiento de Orejas al Saltar**
   - Se mueven con cada salto
   - Comunican coordinación
   - Amplitud: 0.2 radianes

3. **Parpadeo de Sorpresa**
   - Ojos grandes que se cierran al saltar
   - Expresión de alegría

4. **Ojos Grandes y Tiernos**
   - Cambian tamaño con expresión
   - Comunican estado emocional

### CÁLCULOS UTILIZADOS:
```
Hop: abs(sin(hopPhase)) * 0.5 * voxelSize
Ear movement: Synced with hop phase
Eye size: Varies with hop intensity
Landing effect: Slight compression at bottom of hop
```

---

## ANIMAL TYPE 10 - VOLADOR (Alado)
**Características:** Nocturno, volador, ágil
**Colores:** Gris oscuro, Plata, Carbón
**Velocidad:** Media-Alta (1.5 - 2.2)

### ANIMACIONES IMPLEMENTADAS:
1. **Bateo de Alas Fluido**
   - Alas opuestas baten en fase inversa
   - Movimiento realista de vuelo
   - Frecuencia alta: 4 Hz

2. **Movimiento Corporal Sincronizado**
   - Cuerpo se mueve con alas
   - Simulación de vuelo real
   - Pequeños giros en cambios de dirección

3. **Parpadeo de Ojos Brillantes**
   - Ojos luminosos que parpadean
   - Comunican visión nocturna

4. **Postura Aérea**
   - Cuerpo inclinado hacia adelante
   - Proyecta movimiento

### CÁLCULOS UTILIZADOS:
```
Wing flap: sin(flapPhase + wingIndex * π) * amplitude
Body sway: Synchronized with flap
Eye glow: Pulse based on flapPhase
Elevation: Small bobbing motion
```

---

## TÉCNICAS DE PROGRAMACIÓN UTILIZADAS

### 1. Trigonometría
- **Funciones sinusoidales**: `sin()` y `cos()` para suavidad
- **Fases**: Desplazamiento de fase para coordinación
- **Amplitudes**: Control de rango de movimiento

### 2. Articulación
- **Desfase de segmentos**: Cada parte se mueve con pequeño retraso
- **Onda de propagación**: `phase + segmentIndex * factor`
- **Sincronización**: Todas basadas en fases principales

### 3. Animación Fluida
- **Interpolación**: Transiciones suaves entre estados
- **Easing**: Funciones que aceleran/desaceleran movimiento
- **Amortiguamiento**: Valores que disminuyen naturalmente

### 4. Expresión Facial
- **Parpadeo**: Patrón específico de cierre/apertura
- **Movimiento de boca**: Cambio de tamaño/posición
- **Ojos**: Tamaño y brillo variables

### 5. Coordinación del Cuerpo
- **Cuadrúpedos**: Patas diagonales alternas
- **Bípedos**: Balance con brazos/alas
- **Insectos**: Tripod gait (3 patas vs 3 patas)
- **Voladores**: Alas opuestas

---

## CONSTANTES DE FRECUENCIA

| Animación | Frecuencia (Hz) | Rango Típico |
|-----------|------------------|-------------|
| Blink (Parpadeo) | 2.5 | 1.0 - 3.0 |
| Jaw (Mandíbula) | 3.0 | 2.0 - 4.0 |
| Tail Wag (Cola) | 2.0 | 1.5 - 2.5 |
| Wing Flap (Alas) | 4.0 | 3.0 - 5.0 |
| Head Turn (Cabeza) | 1.0 | 0.5 - 1.5 |
| Ear Twitch (Orejas) | 4.0 | 3.0 - 5.0 |
| Body Sway (Cuerpo) | 0.8 | 0.5 - 1.2 |

---

## AMPLITUDES DE MOVIMIENTO

Todas las amplitudes se escalan con `voxelSize` del animal:

| Movimiento | Base Amplitude | Scale Factor |
|-----------|-----------------|---------------|
| Jaw Opening | 0.15 | voxelSize |
| Tail Wag | 0.3 | voxelSize |
| Wing Flap | 0.6 | voxelSize |
| Head Turn | 0.25 | radianes |
| Ear Twitch | 0.2 | radianes |
| Body Sway | 0.15 | radianes |
| Idle Bounce | 0.1 | voxelSize |

---

## NOTAS DE IMPLEMENTACIÓN

1. **AnimationController**: Se crea una instancia por animal. Se actualiza cada frame con `update(deltaTime)`.

2. **Render Order**: Siempre renderizar primero el cuerpo base, luego características móviles (patas, cola, alas, etc.)

3. **Performance**: Las funciones trigonométricas se calculan una sola vez por frame y se reutilizan.

4. **Coordinación**: Usar los mismos `animController.*Phase` para mantener sincronización temporal.

5. **Escalado**: Todos los movimientos deben escalar correctamente con `voxelSize` y `selectionScale`.

6. **Documentación en código**: Cada método render() incluye comentarios sobre qué animaciones se aplican.

---
