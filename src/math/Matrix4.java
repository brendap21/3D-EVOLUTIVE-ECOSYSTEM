package math;

public class Matrix4 {
    public float[][] m;

    public Matrix4(){
        m = new float[4][4];
    }

    public static Matrix4 rotationY(double angle){
        Matrix4 r = new Matrix4();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        r.m[0][0] = (float) cos; r.m[0][1] = 0; r.m[0][2] = (float) sin; r.m[0][3] = 0;
        r.m[1][0] = 0;           r.m[1][1] = 1; r.m[1][2] = 0;           r.m[1][3] = 0;
        r.m[2][0] = (float)-sin; r.m[2][1] = 0; r.m[2][2] = (float) cos; r.m[2][3] = 0;
        r.m[3][0] = 0;           r.m[3][1] = 0; r.m[3][2] = 0;           r.m[3][3] = 1;

        return r;
    }

    public static Matrix4 rotationX(double angle){
        Matrix4 r = new Matrix4();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        r.m[0][0] = 1; r.m[0][1] = 0;    r.m[0][2] = 0;    r.m[0][3] = 0;
        r.m[1][0] = 0; r.m[1][1] = (float) cos; r.m[1][2] = (float) -sin; r.m[1][3] = 0;
        r.m[2][0] = 0; r.m[2][1] = (float) sin; r.m[2][2] = (float) cos;  r.m[2][3] = 0;
        r.m[3][0] = 0; r.m[3][1] = 0;    r.m[3][2] = 0;    r.m[3][3] = 1;

        return r;
    }

    public static Matrix4 translation(double x, double y, double z){
        Matrix4 r = new Matrix4();
        r.m[0][0] = 1; r.m[0][1] = 0; r.m[0][2] = 0; r.m[0][3] = (float)x;
        r.m[1][0] = 0; r.m[1][1] = 1; r.m[1][2] = 0; r.m[1][3] = (float)y;
        r.m[2][0] = 0; r.m[2][1] = 0; r.m[2][2] = 1; r.m[2][3] = (float)z;
        r.m[3][0] = 0; r.m[3][1] = 0; r.m[3][2] = 0; r.m[3][3] = 1;
        return r;
    }

    public Vector3 multiply(Vector3 v){
        float[] p = {(float)v.x, (float)v.y, (float)v.z, 1};
        float[] res = new float[4];

        for(int i=0;i<4;i++){
            res[i] = 0;
            for(int j=0;j<4;j++){
                res[i] += m[i][j]*p[j];
            }
        }
        return new Vector3(res[0], res[1], res[2]);
    }
}
