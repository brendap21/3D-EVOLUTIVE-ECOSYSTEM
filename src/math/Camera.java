package math;

public class Camera {
    private Vector3 posicion;
    private Vector3 forward;
    private Vector3 up;
    private double fov;
    private boolean orthographic = false;
    private double yaw;   // rotation around Y axis
    private double pitch; // rotation around X axis

    public Camera(Vector3 posicion, double fov){
        this.posicion = posicion;
        this.fov = fov;
        this.forward = new Vector3(0,0,1);
        this.up = new Vector3(0,1,0);
        this.yaw = 0.0;
        this.pitch = 0.0;
    }

    public Vector3 getPosicion() { return posicion; }
    public void setPosicion(Vector3 p) { this.posicion = p; }
    public Vector3 getForward() { return forward; }
    public Vector3 getUp() { return up; }
    public double getFov() { return fov; }

    public boolean isOrthographic() { return orthographic; }
    public void setOrthographic(boolean o) { this.orthographic = o; }

    public Vector3 getRight() {
        // Use world-up cross forward to obtain the camera right vector
        // forward.cross(up) points to the LEFT in our coordinate convention,
        // so use up.cross(forward) to get a right-handed right vector.
        return up.cross(forward).normalize();
    }

    // Rotate camera by yaw (radians) and pitch (radians). Pitch is clamped to avoid gimbal lock.
    public void rotate(double deltaYaw, double deltaPitch){
        yaw += deltaYaw;
        pitch += deltaPitch;
        double limit = Math.toRadians(89.0);
        if(pitch > limit) pitch = limit;
        if(pitch < -limit) pitch = -limit;

        // Spherical coordinates -> forward vector
        double cosPitch = Math.cos(pitch);
        double fx = Math.sin(yaw) * cosPitch;
        double fy = Math.sin(pitch);
        double fz = Math.cos(yaw) * cosPitch;

        this.forward = new Vector3(fx, fy, fz).normalize();
        // keep up as world up for simplicity
        this.up = new Vector3(0,1,0);
    }

    public double getYaw(){ return yaw; }
    public double getPitch(){ return pitch; }

    // Set absolute yaw and pitch (radians). Pitch will be clamped to avoid gimbal lock.
    public void setOrientation(double newYaw, double newPitch){
        this.yaw = newYaw;
        double limit = Math.toRadians(89.0);
        if(newPitch > limit) newPitch = limit;
        if(newPitch < -limit) newPitch = -limit;
        this.pitch = newPitch;

        // Recompute forward vector from yaw/pitch
        double cosPitch = Math.cos(pitch);
        double fx = Math.sin(yaw) * cosPitch;
        double fy = Math.sin(pitch);
        double fz = Math.cos(yaw) * cosPitch;

        this.forward = new Vector3(fx, fy, fz).normalize();
        this.up = new Vector3(0,1,0);
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
