package unsw.graphics.world;



import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.jogamp.opengl.GL3;

import com.jogamp.opengl.util.texture.TextureCoords;
import org.omg.Messaging.SYNC_WITH_TRANSPORT;
import unsw.graphics.CoordFrame3D;
import unsw.graphics.Shader;
import unsw.graphics.Vector3;
import unsw.graphics.geometry.*;

import java.lang.Math;
import com.jogamp.opengl.util.texture.Texture;



/**
 * COMMENT: Comment HeightMap 
 *
 * @author malcolmr
 */
public class Terrain {

    private int width;
    private int depth;
    private float[][] altitudes;
    private List<Tree> trees;
    private List<Road> roads;
    private Vector3 sunlight;
    private TriangleMesh mesh;
    private Bunny player;

    /**
     * Create a new terrain
     *
     * @param width The number of vertices in the x-direction
     * @param depth The number of vertices in the z-direction
     */

    public Terrain(int width, int depth, Vector3 sunlight) {
        this.width = width;
        this.depth = depth;
        altitudes = new float[width][depth];
        trees = new ArrayList<Tree>();
        roads = new ArrayList<Road>();
        this.sunlight = sunlight;
    }

    public List<Tree> trees() {
        return trees;
    }
    public Bunny getPlayer(){return player;}

    public List<Road> roads() {
        return roads;
    }

    public Vector3 getSunlight() {
        return sunlight;
    }

    /**
     * Set the sunlight direction. 
     * 
     * Note: the sun should be treated as a directional light, without a position
     * 
     * @param dx
     * @param dy
     * @param dz
     */
    public void setSunlightDir(float dx, float dy, float dz) {
        sunlight = new Vector3(dx, dy, dz);      
    }

    /**
     * Get the altitude at a grid point
     * 
     * @param x
     * @param z
     * @return
     */
    public double getGridAltitude(int x, int z) {
        return altitudes[x][z];
    }

    /**
     * Set the altitude at a grid point
     * 
     * @param x
     * @param z
     * @return
     */
    public void setGridAltitude(int x, int z, float h) {
        altitudes[x][z] = h;
    }

    /**
     * Get the altitude at an arbitrary point. 
     * Non-integer points should be interpolated from neighbouring grid points
     * 
     * @param x
     * @param z
     * @return
     */

    float sign (Point2D p1, Point2D p2, Point2D p3)
    {
        return (p1.getX() - p3.getX()) * (p2.getY() - p3.getY()) - (p2.getX() - p3.getX()) * (p1.getY() - p3.getY());
    }

    boolean ifPoint2Dequal(Point2D p1,Point2D p2){
        if(p1.getX()==p2.getX() && p1.getY()==p2.getY()){
            return true;
        }else{
            return false;
        }
    }

    public boolean checkPointInTriangele(Point2D v1,Point2D v2,Point2D v3,Point2D p){
        boolean b1, b2, b3;

        b1 = sign(p, v1, v2) < 0.0f;
        b2 = sign(p, v2, v3) < 0.0f;
        b3 = sign(p, v3, v1) < 0.0f;
        return ((b1 == b2) && (b2 == b3));
    }

    public List<Float> Barycentric(Point2D v1,Point2D v2, Point2D v3,Point2D p){
        float demonimator=(v2.getY()-v3.getY())*(v1.getX()-v3.getX())  +  (v3.getX()-v2.getX())*(v1.getY()-v3.getY());
        float w1=( (v2.getY()-v3.getY())*(p.getX()-v3.getX())  +  (v3.getX()-v2.getX())*(p.getY()-v3.getY()) )/demonimator;
        float w2=( (v3.getY()- v1.getY())*(p.getX()-v3.getX()) +  (v1.getX()-v3.getX())*(p.getY()-v3.getY()))/demonimator;

        float w3=1.0f-w1-w2;
        return new ArrayList<Float>(Arrays.asList(w1,w2,w3));
    }

    public float altitude(float x, float z) {
        float altitude = 0;
        float left_depth;
        float right_depth;

        // TODO: Implement this
        int top_x = (int)Math.floor(x);
        int bottom_x = (int)Math.floor(x)+1;
        int left_z = (int)Math.floor(z);
        int right_z = (int)Math.floor(z)+1;
//        if (top_x == bottom_x) {
//        		left_depth = (float)getGridAltitude(top_x,left_z);
//        		right_depth = (float)getGridAltitude(top_x,right_z);
//        }
//        else {
//	        	left_depth = (float)getGridAltitude(top_x,left_z)*(bottom_x - x) + (float)getGridAltitude(bottom_x,left_z)*(x - top_x);
//	    		right_depth = (float)getGridAltitude(top_x,right_z)*(bottom_x - x) + (float)getGridAltitude(bottom_x,right_z)*(x - top_x);
//        }
//        if(left_z==right_z) {
//        		altitude = left_depth;
//        }
//        else {
//        		altitude = (z-left_z)*right_depth + (right_z-z)*left_depth;
//        }
        Point2D topleft=new Point2D(top_x,left_z);
        Point2D bottomleft=new Point2D(bottom_x,left_z);
        Point2D topright=new Point2D(top_x,right_z);
        Point2D bottomright=new Point2D(bottom_x,right_z);
        List<Point2D> listofCorners=new ArrayList<>(Arrays.asList(topleft,bottomleft,topright,bottomright));
        Point2D target=new Point2D(x,z);

        for(Point2D each:listofCorners){
            if(ifPoint2Dequal(each,target)){
                altitude=(float) (getGridAltitude((int)each.getX(),(int)each.getY()));
                return altitude;
            }
        }

        if(x==width-1){
            altitude=(float)( (right_z-z)*getGridAltitude(width-1,left_z )+
                    (z-left_z)*getGridAltitude(width-1,right_z));
            return altitude;
        }

        if(z==depth-1){
            altitude=(float)( (bottom_x-x)*getGridAltitude(top_x,depth-1)+
                    (x-top_x)*getGridAltitude( bottom_x,depth-1 ) );
            return altitude;
        }

        if(checkPointInTriangele(topleft,bottomleft,topright,target)){
            List<Float> weights=Barycentric(topleft,bottomleft,topright,target);
            altitude=(float) (getGridAltitude((int)topleft.getX(),(int)topleft.getY())*weights.get(0)+
                    getGridAltitude((int)bottomleft.getX(),(int)bottomleft.getY())*weights.get(1)+
                    getGridAltitude((int)topright.getX(),(int)topright.getY())*weights.get(2)
            );
        }else{
            List<Float> weights=Barycentric(bottomleft,topright,bottomright,target);
            altitude=(float)( getGridAltitude((int)bottomleft.getX(),(int)bottomleft.getY())*weights.get(0)+
                 getGridAltitude((int)topright.getX(),(int)topright.getY())*weights.get(1)+
                 getGridAltitude((int)bottomright.getX(),(int)bottomright.getY())*weights.get(2)
            );
        }
        	        
        return altitude;
    }

    public float getRealWorldAltitude(float x, float z){
        return altitude(x,getDepth()-z);
    }

    /**
     * Add a tree at the specified (x,z) point. 
     * The tree's y coordinate is calculated from the altitude of the terrain at that point.
     * 
     * @param x
     * @param z
     */
    public void addTree(float x, float z) {
        float y = altitude(x, z);
        Tree tree = new Tree(x, y, z);
        trees.add(tree);
    }

    public void setPlayer(float x, float z){
        float y = altitude(x, this.getDepth()-z)+0.3f;
        player = new Bunny(x, y, z);

    }


//    /**
//     * Add a road.
//     *
//     * @param x
//     * @param z
//     */
    public void addRoad(float width, List<Point2D> spine) {
        Road road = new Road(width, spine);
        roads.add(road);        
    }
    


    public void init(GL3 gl){
        List<Point3D> shape = new ArrayList<Point3D>();
        List<Integer> indicesBuffer = new ArrayList<Integer>();
        List<Point2D> textureCord=new ArrayList<Point2D>();
        for (int i=0;i<width;i++) {
            for(int j=0;j<depth;j++) {
                shape.add(new Point3D(i,altitudes[i][j],-j));
            }
        }


        for (int i=0;i<width-1;i++) {
            for(int j=0;j<depth-1;j++) {
//                indicesBuffer.add(depth*(i+1)+j);   //v0
//                indicesBuffer.add(depth*(i+1)+j+1); //v1
//                indicesBuffer.add(depth*i+j);       //v2
//
//
//                indicesBuffer.add(depth*(i+1)+j+1); //v1
//                indicesBuffer.add(depth*i+j+1);     //v3
//                indicesBuffer.add(depth*i+j);      //v2

                indicesBuffer.add(depth*(i+1)+j);   //v0
                indicesBuffer.add(depth*i+j+1);     //v3
                indicesBuffer.add(depth*i+j);       //v2


                indicesBuffer.add(depth*(i+1)+j+1); //v1
                indicesBuffer.add(depth*i+j+1);     //v3
                indicesBuffer.add(depth*(i+1)+j);   //v0
            }
        }
        float x_step=1.0f/getWidth();
        float y_step=1.0f/getDepth();

        for (int i=0;i<width;i++) {
            for(int j=0;j<depth;j++) {
                Point2D vTexcord=new Point2D(i*x_step,j*y_step);
                textureCord.add(vTexcord);
            }
        }

        mesh = new TriangleMesh(shape,indicesBuffer,true,textureCord);
        mesh.init(gl);
    }
	
    public void draw(GL3 gl, CoordFrame3D frame) {
        mesh.draw(gl,frame);
//        Shader.setInt(gl, "mode", 1);
//        Shader.setInt(gl, "mode", 0);

    }

    public int getWidth(){
        return width-1;
    }

    public int getDepth(){
        return depth-1;
    }

//    public static void main(String[] args) throws FileNotFoundException {
//        Terrain terrain = LevelIO.load(new File(args[0]));
//        System.out.println(terrain.altitude(5,5));
//    }


}
