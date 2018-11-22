package unsw.graphics.world;

import java.io.IOException;

import com.jogamp.opengl.GL3;

import unsw.graphics.CoordFrame3D;
import unsw.graphics.geometry.Point3D;
import unsw.graphics.geometry.TriangleMesh;

public class Bunny {

    private Point3D position;
    private TriangleMesh tt;

    public Bunny(float x, float y, float z) {
        position = new Point3D(x, y, z);

    }

    public void init(GL3 gl){
        try {
            tt = new TriangleMesh("res/models/bunny.ply",true,true);
            tt.init(gl);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Point3D getPosition() {
        return position;
    }

    public void draw(GL3 gl, CoordFrame3D frame) {
        tt.draw(gl, frame);
    }


}
