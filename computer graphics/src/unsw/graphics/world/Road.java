package unsw.graphics.world;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL3;
import unsw.graphics.CoordFrame3D;
import unsw.graphics.Shader;
import unsw.graphics.geometry.Line3D;
import unsw.graphics.geometry.Point2D;
import unsw.graphics.geometry.Point3D;
import unsw.graphics.geometry.TriangleMesh;

/**
 * COMMENT: Comment Road 
 *
 * @author malcolmr
 */
public class Road {

    private List<Point2D> points;
    private float width;
    private TriangleMesh  mesh;
    private List<Point3D> actual3DPoints;
    private List<Point3D> skelotonPoints;
    /**
     * Create a new road with the specified spine
     *
     * @param width
     * @param spine
     */
    public Road(float width, List<Point2D> spine) {
        this.width = width;
        this.points = spine;
    }

    /**
     * The width of the road.
     *
     * @return
     */
    public double width() {
        return width;
    }

    /**
     * Get the number of segments in the curve
     *
     * @return
     */
    public int size() {
        return points.size() / 3;
    }

    public float computerDistance(Point3D p1,Point3D p2){
        return (float) Math.sqrt((p1.getX()-p2.getX())*(p1.getX()-p2.getX())+(p1.getY()-p2.getY())*(p1.getY()-p2.getY())+(p1.getZ()-p2.getZ())*(p1.getZ()-p2.getZ()));
    }

    /**
     * Get the specified control point.
     *
     * @param i
     * @return
     */
    public Point2D controlPoint(int i) {
        return points.get(i);
    }

    /**
     * Get a point on the spine. The parameter t may vary from 0 to size().
     * Points on the kth segment take have parameters in the range (k, k+1).
     *
     * @param t
     * @return
     */
    public Point2D point(float t) {
        int i = (int)Math.floor(t);
        t = t - i;

        i *= 3;

        Point2D p0 = points.get(i++);
        Point2D p1 = points.get(i++);
        Point2D p2 = points.get(i++);
        Point2D p3 = points.get(i++);


        float x = b(0, t) * p0.getX() + b(1, t) * p1.getX() + b(2, t) * p2.getX() + b(3, t) * p3.getX();
        float y = b(0, t) * p0.getY() + b(1, t) * p1.getY() + b(2, t) * p2.getY() + b(3, t) * p3.getY();

        return new Point2D(x, y);
    }

    public Point2D tangent(float t) {
        int i = (int)Math.floor(t);
        t = t - i;

        i *= 3;

        Point2D p0 = points.get(i++);
        Point2D p1 = points.get(i++);
        Point2D p2 = points.get(i++);
        Point2D p3 = points.get(i++);



        float x = bt(0, t) * (p1.getX()-p0.getX()) + bt(1, t) * (p2.getX()-p1.getX()) + bt(2, t) * (p3.getX()-p2.getX());
        float y = bt(0, t) * (p1.getY()-p0.getY()) + bt(1, t) * (p2.getY()-p1.getY()) + bt(2, t) * (p3.getY()-p2.getY());
        return new Point2D(x, y);
    }




    /**
     * Calculate the Bezier coefficients
     *
     * @param i
     * @param t
     * @return
     */
    private float b(int i, float t) {

        switch(i) {

            case 0:
                return (1-t) * (1-t) * (1-t);

            case 1:
                return 3 * (1-t) * (1-t) * t;

            case 2:
                return 3 * (1-t) * t * t;

            case 3:
                return t * t * t;
        }

        // this should never happen
        throw new IllegalArgumentException("" + i);
    }


    private float bt(int i, float t) {

        switch(i) {

            case 0:
                return 3 * (1-t) * (1-t);

            case 1:
                return 6 * (1-t) * t;

            case 2:
                return 3 * t * t;
        }

        // this should never happen
        throw new IllegalArgumentException("" + i);
    }

    public void init(GL3 gl,Terrain terrain){
        int numberOfBands=3;
        skelotonPoints=new ArrayList<>();
        List<Point2D> skeloton2DPointNormals=new ArrayList<>();
        actual3DPoints=new ArrayList<>();
        List<Point2D> skeloton2DPoints=new ArrayList<>();
        float tall=0.01f;
        List<Integer> indicesBuffer = new ArrayList<>();
        for(float i=0;i<=size();i+=0.025){
            Point2D currentPoint=point(i);
            skelotonPoints.add(new Point3D(currentPoint.getX(),tall,-currentPoint.getY()));
            skeloton2DPoints.add(new Point2D(currentPoint.getX(),currentPoint.getY()));
            Point2D currentPointTangent=tangent(i);
            float dx=currentPointTangent.getX();
            float dy=currentPointTangent.getY();
            float modula_pre=(float) Math.sqrt(dx*dx+dy*dy);
            dx=dx/modula_pre;
            dy=dy/modula_pre;
            float nx=-dy;
            float ny=dx;
            skeloton2DPointNormals.add(new Point2D(nx,ny));
        }

        Point2D corner=skeloton2DPoints.get(0);
        float mx=Math.max(Math.min(corner.getX(),terrain.getWidth()),0);
        float mz=Math.max(Math.min(corner.getY(),terrain.getDepth()),0);
        float height=terrain.altitude(mx,mz)+tall;

//calcuate vertex coordinate
        float gap=width/(2*(numberOfBands-1));
        for(int b=numberOfBands-1;b>=0;b--){
            for(int i=0;i<skeloton2DPoints.size();i++){ //1-?0
                Point2D currentN=skeloton2DPointNormals.get(i);
                Point2D currentP=skeloton2DPoints.get(i);
                float currentX=currentP.getX()+b*currentN.getX()*gap;
                float currentY=currentP.getY()+b*currentN.getY()*gap;
//                float mx=Math.max(Math.min(currentX,terrain.getWidth()),0);
//                float mz=Math.max(Math.min(currentY,terrain.getDepth()),0);
//                float height=terrain.altitude(mx,mz)+tall;
                Point3D current=new Point3D(currentX,height,-currentY);
                actual3DPoints.add(current);
            }
        }

        for(int b=1;b<numberOfBands;b++){
            for(int i=0;i<skeloton2DPoints.size();i++){ //1-?0
                Point2D currentN=skeloton2DPointNormals.get(i);
                Point2D currentP=skeloton2DPoints.get(i);
                float currentX=currentP.getX()-b*currentN.getX()*gap;
                float currentY=currentP.getY()-b*currentN.getY()*gap;
//                float mx=Math.max(Math.min(currentX,terrain.getWidth()),0);
//                float mz=Math.max(Math.min(currentY,terrain.getDepth()),0);
//                float height=terrain.altitude(mx,mz)+tall;
                Point3D current=new Point3D(currentX,height,-currentY);
                actual3DPoints.add(current);
            }
        }

        int Length=skeloton2DPoints.size();
        for (int i=0;i<numberOfBands*2-2;i++) {
            for(int j=0;j<Length-1;j++) {
                indicesBuffer.add(Length*(i+1)+j);   //v0
                indicesBuffer.add(Length*(i+1)+j+1); //v1
                indicesBuffer.add(Length*i+j);       //v2
                indicesBuffer.add(Length*(i+1)+j+1); //v1
                indicesBuffer.add(Length*i+j+1);     //v3
                indicesBuffer.add(Length*i+j);      //v2
            }
        }

//calcuate texture coordinate
        List<Float> totalDistanceList=new ArrayList<>();
        List<ArrayList<Float>> segmentLengthList=new ArrayList<ArrayList<Float>>();
        for(int l=0;l<numberOfBands*2-1;l++){
            float distanceSum=0;
            ArrayList<Float> lengthList=new ArrayList<>();
            for(int i=1;i<Length;i++){
                float distance=computerDistance(actual3DPoints.get(l*Length+i),actual3DPoints.get(l*Length+i-1));
                lengthList.add(distance);
                distanceSum+=distance;
            }
            totalDistanceList.add(distanceSum);
//            System.out.println(distanceSum);
            segmentLengthList.add(lengthList);
        }


        List<Point2D> startPointList=new ArrayList<>();
        float stripHeight=1.0f/(numberOfBands*2-2);
        for(int i=0;i<numberOfBands*2-1;i++){
            startPointList.add(new Point2D(0,i*stripHeight));
        }

        List<Point2D> textureCord=new ArrayList<>();

        for(int l=0;l<numberOfBands*2-1;l++){
            textureCord.add(startPointList.get(l));
            for(int i=0;i<Length-1;i++) {
                Point2D vTexcordt=new Point2D(segmentLengthList.get(l).get(i)/totalDistanceList.get(l),l*stripHeight);
                textureCord.add(vTexcordt);
            }
        }

        mesh = new TriangleMesh(actual3DPoints,indicesBuffer,true,textureCord);
        mesh.init(gl);
    }

    public void draw(GL3 gl, CoordFrame3D frame){
        mesh.draw(gl,frame);
    }


}
