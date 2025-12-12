package main;

import render.SoftwareRenderer;
import math.Camera;

/** Interfaz para objetos que se actualizan y dibujan con el renderer. */
public interface Renderable {
    void render(SoftwareRenderer renderer, Camera cam);
    void update();
}
