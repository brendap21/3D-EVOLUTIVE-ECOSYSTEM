# 3D EVOLUTIVE ECOSYSTEM

## Descripción
3D EVOLUTIVE ECOSYSTEM es un proyecto de animación y visualización 3D desarrollado en Java, utilizando renderizado por software. Permite crear un ecosistema simple con cubos y cilindros, controlar la cámara con teclado y ver la escena en tiempo real mediante un panel de renderizado.

Este proyecto es parte de la materia Gráficas por Computadora 2D y 3D y sirve como base para experimentar con transformaciones, proyecciones, animación y manejo de hilos en Java.

## Características

- Renderizado 3D por software sin librerías externas.
- Cámara libre controlable con teclado (WASD/UP/DOWN).
- Objetos incluidos:
  - Cubos (con posición, tamaño y color configurable)
  - Cilindros (con radio, altura y color configurable)
- Renderizado con doble buffer (simulado) para evitar parpadeo.
- Estructura modular: entities, math, render, ui, main.
- Hilos separados para renderizado y actualización de controles.
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

## Estructura del Proyecto

- 3D-Evolutive-Ecosystem/
- │
- ├─ src/
- │  ├─ main/
- │  │  ├─ EcosistemaApp.java
- │  │  ├─ RenderPanel.java
- │  │  ├─ RenderThread.java
- │  │  └─ Renderable.java
- │  │
- │  ├─ entities/
- │  │  ├─ Cubo.java
- │  │  ├─ Cilindro.java
- │  │  └─ Curva.java
- │  │
- │  ├─ math/
- │  │  ├─ Camera.java
- │  │  ├─ Matrix4.java
- │  │  ├─ Transform.java
- │  │  └─ Vector3.java
- │  │
- │  ├─ render/
- │  │  └─ SoftwareRenderer.java
- │  │
- │  └─ ui/
- │     └─ Controles.java
- │
- └─ README.md


## Uso y Extensiones

- Puedes agregar nuevos objetos 3D implementando la interfaz Renderable.
- Experimentar con rotaciones, escalas y animaciones modificando el método update() de cada entidad.
- Ajustar el FOV de la cámara en Camera para cambiar la perspectiva.
- Implementar más formas geométricas (esferas, pirámides, etc.) usando SoftwareRenderer.

## Compilación y Ejecución

Desde la consola, en la raíz del proyecto:

    javac -d bin src/main/*.java src/entities/*.java src/math/*.java src/ui/*.java src/render/*.java
    java -cp bin main.EcosistemaApp

