package math;

public class Vector3 {
    public double x, y, z;

    public Vector3(double x, double y, double z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3 add(Vector3 v){
        return new Vector3(this.x + v.x, this.y + v.y, this.z + v.z);
    }

    public Vector3 subtract(Vector3 v){
        return new Vector3(this.x - v.x, this.y - v.y, this.z - v.z);
    }

    public Vector3 scale(double s){
        return new Vector3(this.x * s, this.y * s, this.z * s);
    }

    public Vector3 normalize(){
        double len = Math.sqrt(x*x + y*y + z*z);
        if(len == 0) return new Vector3(0,0,0);
        return new Vector3(x/len, y/len, z/len);
    }

    public Vector3 cross(Vector3 v){
        return new Vector3(
            this.y*v.z - this.z*v.y,
            this.z*v.x - this.x*v.z,
            this.x*v.y - this.y*v.x
        );
    }
}
