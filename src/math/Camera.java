package math;

public class Camera {
    private Vector3 posicion;
    private double yaw = 0;
    private double pitch = 0;
    private double fov = 500; // FOV por defecto

    public Camera(Vector3 posicionInicial) {
        this.posicion = posicionInicial;
    }

    public Vector3 getPosicion() {
        return posicion;
    }

    public void setPosicion(Vector3 pos) {
        this.posicion = pos;
    }

    public double getYaw() {
        return yaw;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    public double getFov() {
        return fov;
    }

    public void setFov(double fov) {
        this.fov = fov;
    }
}
