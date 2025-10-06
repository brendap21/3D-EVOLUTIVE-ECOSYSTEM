package math;

public class Camera {
    private Vector3 posicion;
    private Vector3 forward;
    private Vector3 up;
    private double fov;

    public Camera(Vector3 posicion, double fov){
        this.posicion = posicion;
        this.fov = fov;
        this.forward = new Vector3(0,0,1);
        this.up = new Vector3(0,1,0);
    }

    public Vector3 getPosicion() { return posicion; }
    public void setPosicion(Vector3 p) { this.posicion = p; }
    public Vector3 getForward() { return forward; }
    public Vector3 getUp() { return up; }
    public double getFov() { return fov; }

    public Vector3 getRight() {
        return forward.cross(up).normalize();
    }

    public void moveForward(double amount){
        posicion = posicion.add(forward.scale(amount));
    }

    public void moveBackward(double amount){
        posicion = posicion.subtract(forward.scale(amount));
    }

    public void moveRight(double amount){
        posicion = posicion.add(getRight().scale(amount));
    }

    public void moveLeft(double amount){
        posicion = posicion.subtract(getRight().scale(amount));
    }
}
