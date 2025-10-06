package math;

public class Camera {
    private Vector3 posicion;
    private double fov;

    public Camera(Vector3 posicion, double fov) {
        this.posicion = posicion;
        this.fov = fov;
    }

    public Vector3 getPosicion() {
        return posicion;
    }

    public double getFov() {
        return fov;
    }

    public void setPosicion(Vector3 posicion) {
        this.posicion = posicion;
    }

    public void setFov(double fov) {
        this.fov = fov;
    }
}
