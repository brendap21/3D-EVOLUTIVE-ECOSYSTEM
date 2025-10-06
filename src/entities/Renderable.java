package entities;

import render.SoftwareRenderer;
import math.Camera;

public interface Renderable {
    void render(SoftwareRenderer renderer, Camera cam);
}
