package main;

import render.SoftwareRenderer;
import math.Camera;

/**
 * Renderable: interface para objetos que pueden actualizarse y dibujarse
 * usando el renderer por software. Debe residir en el paquete `main` porque
 * otras clases importan `main.Renderable`.
 */
public interface Renderable {
    void render(SoftwareRenderer renderer, Camera cam);
    void update();
}
