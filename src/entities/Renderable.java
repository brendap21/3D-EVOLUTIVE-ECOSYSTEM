package entities;

import math.Camera;
import render.SoftwareRenderer;

public interface Renderable {
    void render(SoftwareRenderer renderer, Camera cam);
    void update(); // Método para animación
}
