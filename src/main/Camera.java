package main;

import math.Vector3;
import math.Matrix4;

public class Camera {
    private Vector3 position;
    private float yaw;
    private float fov = 1.0f;

    public Camera(Vector3 position) {
        this.position = position;
        this.yaw = 0f;
    }

    public Vector3 transform(Vector3 point) {
        Matrix4 rotY = Matrix4.rotationY(-yaw);
        Vector3 translated = new Vector3(
            point.x - position.x,
            point.y - position.y,
            point.z - position.z
        );
        return rotY.multiply(translated);
    }

    public float getFov() {
        return fov;
    }

    public void rotate(float deltaYaw) {
        yaw += deltaYaw;
    }

    public Vector3 getPosition() {
        return position;
    }

    public void move(Vector3 delta) {
        position = new Vector3(
            position.x + delta.x,
            position.y + delta.y,
            position.z + delta.z
        );
    }
}
