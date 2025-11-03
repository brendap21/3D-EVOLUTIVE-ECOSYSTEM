package math;

public class Transform {

    public static Vector3 project(Vector3 punto, Camera cam, int ancho, int alto){
        double xRel = punto.x - cam.getPosicion().x;
        double yRel = punto.y - cam.getPosicion().y;
        double zRel = punto.z - cam.getPosicion().z;

        if(zRel == 0) zRel = 0.0001;

        // Use camera's fov instead of a hardcoded value. Keep compatibility
        // with existing code by treating fov as a scale factor.
        double fov = cam.getFov();
        double x2d = (xRel * fov) / zRel + ancho / 2.0;
        double y2d = (yRel * fov) / zRel + alto / 2.0;

        return new Vector3(x2d, y2d, zRel);
    }
}
