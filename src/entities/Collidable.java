package entities;

import math.Vector3;

/** Simple collidable interface returning world-space AABB */
public interface Collidable {
    Vector3 getAABBMin();
    Vector3 getAABBMax();
}
