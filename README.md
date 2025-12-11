# 3D EVOLUTIVE ECOSYSTEM

## Descripción
3D EVOLUTIVE ECOSYSTEM es un  Simulador/mini-juego 3D donde animales voxel (cubos) evolucionan proceduralmente (color, tamaño, velocidad, comportamiento). El jugador puede interactuar (añadir / alimentar / eliminar), ajustar parámetros en tiempo real, guardar/cargar partidas y recorrer el mundo con cámara libre. Todo renderizado por software — solo usa operaciones por píxel (BufferedImage.setRGB/putPixel) y un JPanel para mostrar el BufferedImage.

## Características

- Renderizado 3D por software sin librerías externas (BufferedImage.setRGB píxel a píxel).
- Cámara libre controlable con teclado (WASD/UP/DOWN).
- Objetos incluidos:
  - Cubos (posibilidad de rotación y escala vía voxelSize)
  - Cilindros procedimentales (troncos/tallos) generados en SoftwareRenderer
  - Terreno como malla 3D (grid triangular)
- Renderizado con doble buffer para evitar parpadeo.
- Animación determinista (semillas fijas) y evolución en más de 3 minutos.
- Estructura modular: entities, math, render, ui, main, simulation.
- Hilos separados para renderizado (RenderThread) y simulación (Simulador).
- Fácil de ampliar con nuevos objetos y animaciones.

## Controles

Tecla      | Acción
---------- | -----------------------------
W          | Mover cámara hacia adelante
S          | Mover cámara hacia atrás
A          | Mover cámara hacia la izquierda
D          | Mover cámara hacia la derecha
UP         | Subir cámara (eje Y negativo)
DOWN       | Bajar cámara (eje Y positivo)
Y          | Abrir menú spawn animal/depredador
ESC        | Abrir menú de pausa

## Uso y Extensiones

- Puedes agregar nuevos objetos 3D implementando la interfaz Renderable.
- Experimentar con rotaciones, escalas y animaciones modificando el método update() de cada entidad.
- Ajustar el FOV de la cámara en Camera para cambiar la perspectiva.

## Interacción con entidades:
-	Click derecho: Displaya un menú lateral con la información del animal/depredador (id, especie, tiempo de generación, etapa de evolucion, tiempo restante para próxima evolución y controladorees que permiten avanzar directamente a una evolución o retroceder si lo permite, además de un botón que permit eliminar dicha entidad).

## Guardado/Carga: 
- Dialog que permite elegir la ubicación del archivo, nombrarlo y seleccionarlo para guardar o cargar la partida. El archivo genera una lista de entidades con todos sus atributos y RNG seed, parámetros del mundo y tiempo transcurrido (para continuar exactamente como se dejó).

## Compilación y Ejecución

Desde la consola, en la raíz del proyecto:

    javac -d bin -sourcepath src src\main\EcosistemaApp.java
    java -cp bin main.EcosistemaApp

