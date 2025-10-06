package math;

public class Camera {
    private Vector3 posicion;

    public Camera(Vector3 posicionInicial) {
        this.posicion = posicionInicial;
    }

    public Vector3 getPosicion() {
        return posicion;
    }

    public void setPosicion(Vector3 pos) {
        this.posicion = pos;
    }
}
