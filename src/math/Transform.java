package math;

public class Transform {

    public static Vector3 project(Vector3 punto, Camera cam, int ancho, int alto){
        // Transform point into camera (view) space using camera basis vectors
        Vector3 camPos = cam.getPosicion();
        Vector3 rel = new Vector3(punto.x - camPos.x, punto.y - camPos.y, punto.z - camPos.z);

    // Build an orthonormal camera basis from forward and world-up to avoid skew
    Vector3 forward = cam.getForward().normalize();
    Vector3 worldUp = new Vector3(0, 1, 0);
    Vector3 right = worldUp.cross(forward).normalize();
    Vector3 up = forward.cross(right).normalize();

    // Camera-space coordinates (dot with orthonormal basis)
    double cx = rel.x * right.x + rel.y * right.y + rel.z * right.z;
    double cy = rel.x * up.x    + rel.y * up.y    + rel.z * up.z;
    double cz = rel.x * forward.x + rel.y * forward.y + rel.z * forward.z;

        // Avoid division by zero and handle points behind the camera
        if (cz == 0) cz = 0.0001;

        double fov = cam.getFov();
        if (cam.isOrthographic()) {
            double x2d = cx * fov + ancho / 2.0;
            // screen Y grows downwards; subtract to map world-up to screen-up
            double y2d = alto / 2.0 - cy * fov;
            return new Vector3(x2d, y2d, cz);
        } else {
            double x2d = (cx * fov) / cz + ancho / 2.0;
            double y2d = alto / 2.0 - (cy * fov) / cz;
            return new Vector3(x2d, y2d, cz);
        }
    }
}
