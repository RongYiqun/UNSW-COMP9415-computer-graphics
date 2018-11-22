package unsw.graphics.world;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import unsw.graphics.*;
import unsw.graphics.geometry.Point3D;



/**
 * COMMENT: Comment Game
 * In the basic part:
 *      1.press key space to activate/disable the night mode
 *      2.press key shift to activate/disable the third person mode
 *
 *
 *In the extension part:
 * I implemented the following:
 *      1.Make the sun move and change colour according to the time of day ->press key m to enable or disable
 *      2.Add distance attenuation to the torch light
 *      3.Implement normal mapping on one of your models(terrain) ->press key n to enable or disable
 *

 */
public class World extends Application3D implements KeyListener,MouseListener {

    private Terrain terrain;
    private float tlr = 0;
    private float positionx = 0;
    private float positionz=0;
    private float lastX = 0, lastY = 0;
    private boolean firstMouse;
    private float yaw= 0;
    private float pitch= 0;
    private boolean disableMouse=false;
    private Texture grassTexture;
    private Texture bunnyTexture;
    private Texture treeTexture;
    private Texture roadTexture;
    private Texture normalMap;
//    private Texture rainTexture;
    private boolean firstPersonMode;
    private float dx = 0;
    private float dz = 1;
    private boolean dayMode;
    private boolean enableNormalMapping;
    private boolean sunMove;
    private float sunRotate;

    public World(Terrain terrain) {
        super("Assignment 2", 800, 600);
        this.terrain = terrain;
        this.setBackground(new Color(255, 255, 255));
        this.lastX=400;
        this.lastY=300;
        this.firstMouse=true;
        this.disableMouse=true; //for debug purpose, false to enable mouse or true to disable mouse, keys response differently in these two modes.
        this.positionx=0;
        this.positionz=0;
        this.firstPersonMode=true;
        this.dayMode=true;
        this.enableNormalMapping=false;
        this.sunMove=false;
        this.sunRotate=0;
    }

    /**
     * Load a level file and display it.
     *
     * @param args - The first argument is a level file in JSON format
     * @throws FileNotFoundException
     */
    public static void main(String[] args) throws FileNotFoundException {
        Terrain terrain = LevelIO.load(new File(args[0]));
        World world = new World(terrain);
        world.start();
    }




    @Override
    public void display(GL3 gl) {
        super.display(gl);
        gl.glEnable(GL.GL_DEPTH_TEST);

        CoordFrame3D model=CoordFrame3D.identity().translate(-0.5f*terrain.getWidth(),0 , 0.5f*terrain.getDepth());
        // after the about transformation, the terrain is centered at point (0,0) in world
        float tall=0.3f; //set the model height
        float ax=positionx+0.5f*(terrain.getWidth());  //get back the original coordinate in terrain model
        float az=positionz+0.5f*(terrain.getDepth());
        float mx=Math.max(Math.min(ax,terrain.getWidth()),0);
        float mz=Math.max(Math.min(az,terrain.getDepth()),0);
        float currenty=terrain.getRealWorldAltitude(mx,mz)+tall; //absolute height=altitude+tall



        CoordFrame3D camera;
        if(firstPersonMode){ //if player is in firstPersonMode
            camera = CoordFrame3D.identity().rotateX(-pitch).rotateY(tlr+yaw).translate(-positionx,-currenty,-positionz);
            Shader.setPoint3D(gl, "viewPos", new Point3D(positionx,currenty,positionz));
        }else{  //if player is in thirdPersonMode
            camera = CoordFrame3D.identity().rotateX(pitch).rotateY(-tlr-yaw).translate(-(positionx+dx),-currenty,-(positionz+dz));
            Shader.setPoint3D(gl, "viewPos", new Point3D(positionx+dx,currenty,positionz+dz));
        }
        //the camera stuff

        Shader.setPoint3D(gl,"torchPosition",new Point3D(positionx,currenty,positionz));
        Shader.setModelMatrix(gl,model.getMatrix());
        Shader.setViewMatrix(gl,camera.getMatrix());



        Shader.setColor(gl, "ambientIntensity", new Color(0.3f, 0.3f, 0.3f));
        Shader.setColor(gl, "ambientCoeff", Color.WHITE);
        Shader.setColor(gl, "diffuseCoeff", new Color(0.8f, 0.8f, 0.8f));
        Shader.setColor(gl, "specularCoeff", new Color(0.4f, 0.4f, 0.4f));
        Shader.setFloat(gl, "phongExp", 16f);
        Point3D sunDirection= terrain.getSunlight().asPoint3D();
        float daylightIntensity=1;
        if(sunMove){  // change the sunlight direction and change the color over time
            float radian=(float) Math.toRadians(sunRotate);
            float cos=(float) Math.cos(radian);
            float sin=(float) Math.sin(radian);
            sunDirection=new Point3D(cos,sin,0);
            sunRotate+=0.5;
            daylightIntensity=Math.max(sin,0);
            sunRotate=sunRotate%180;
        }

        Shader.setPoint3D(gl, "lightDirection", sunDirection);
        if(dayMode){
            Shader.setColor(gl, "daylightIntensity", new Color(daylightIntensity,daylightIntensity,daylightIntensity));
            Shader.setInt(gl,"mode",0);
        }else{
            Shader.setColor(gl, "nightlightIntensity", new Color(0.2f, 0.2f, 0.2f));
            Shader.setColor(gl,"torchlightIntensity",new Color(0.8f,0.8f,0.8f));
            Shader.setFloat(gl,"lightConstant",1.0f);
            Shader.setFloat(gl,"lightLinear",0.09f);
            Shader.setFloat(gl,"lightQuadratic",0.032f);
            Shader.setInt(gl,"mode",1);
            if(firstPersonMode){
                Shader.setPoint3D(gl, "torchDirection", new Point3D(dx,0,-dz));
            }else{
                Shader.setPoint3D(gl, "torchDirection", new Point3D(-dx,0,-dz));
            }
            Shader.setFloat(gl,"cutoff",0.8f);
        }


        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, grassTexture.getId());

        if(enableNormalMapping){  //if enable the normal mapping in terrain
            Shader.setInt(gl, "normalMap", 1);
            gl.glActiveTexture(GL.GL_TEXTURE1);
            gl.glBindTexture(GL.GL_TEXTURE_2D, normalMap.getId());
            Shader.setInt(gl,"enable",1);
            terrain.draw(gl,model);
            Shader.setInt(gl,"enable",0);
        }else{
            terrain.draw(gl,model);
        }

        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, treeTexture.getId());
        for(Tree each:terrain.trees()) {
            Point3D position=each.getPosition();
            Point3D newPosition=new Point3D(position.getX(),position.getY(),-position.getZ());
            each.draw(gl, model.translate(newPosition).scale(0.2f, 0.2f, 0.2f).translate(0,5,0));
        }

        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, treeTexture.getId());

        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, roadTexture.getId());
        for(Road each:terrain.roads()){
            each.draw(gl, model);
        }

        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, bunnyTexture.getId());

        if(firstPersonMode){  //if player is in firstPersonMode
            terrain.getPlayer().draw(gl,CoordFrame3D.identity().translate(positionx,currenty-tall,positionz).rotateX(pitch).rotateY(-tlr-yaw-90).scale(1.5f,1.5f,1.5f));
        }else{      //if player is in thirdPersonMode
            terrain.getPlayer().draw(gl,CoordFrame3D.identity().translate(positionx,currenty-tall,positionz).rotateX(-pitch).rotateY(tlr+yaw-90).scale(1.5f,1.5f,1.5f));
        } //make sure the bunny always face to player in face


    }



    @Override
    public void destroy(GL3 gl) {
        super.destroy(gl);

    }

    @Override
    public void init(GL3 gl) {
        super.init(gl);
        this.terrain.init(gl);
        for(Tree each:terrain.trees()) {
            each.init(gl);
        }

        for(Road each:terrain.roads()) {
            each.init(gl,terrain);
        }

        this.terrain.setPlayer(this.positionx,this.positionz);
        this.terrain.getPlayer().init(gl);
        getWindow().addKeyListener(this);
        getWindow().addMouseListener(this);
        grassTexture = new Texture(gl, "res/textures/grass.jpg", "jpg", true);
        treeTexture = new Texture(gl, "res/textures/tree.png", "png", false);
        bunnyTexture = new Texture(gl, "res/textures/BrightPurpleMarble.png", "png", false);
        roadTexture = new Texture(gl, "res/textures/road.jpg", "jpg", true);
        normalMap=new Texture(gl,"res/textures/normalMap4.png","png",false);
//        Shader shader = new Shader(gl,"shaders/current_vertex.glsl","shaders/current_fragment.glsl");
        Shader shader = new Shader(gl,"shaders/vertex_phong.glsl","shaders/fragment_phong.glsl");
        shader.use(gl);

    }
    @Override
    public  void keyPressed(KeyEvent e) {
        if(e.getKeyCode()==KeyEvent.VK_ESCAPE){
            System.exit(0);
        }else if(e.getKeyCode()==KeyEvent.VK_SHIFT){
            if(this.firstPersonMode){
                this.firstPersonMode=false;
                tlr=-tlr;
                yaw=-yaw;
                dx=(float)Math.sin(Math.toRadians(tlr));
                dz=(float)Math.cos(Math.toRadians(tlr));
            }else{
                this.firstPersonMode=true;
                tlr=-tlr;
                yaw=-yaw;
                dx=(float)Math.sin(Math.toRadians(tlr));
                dz=(float)Math.cos(Math.toRadians(tlr));
            }
        }else if(e.getKeyCode()==KeyEvent.VK_SPACE){
            if(dayMode){
                dayMode=false;
            }else{
                dayMode=true;
            }
        }else if(e.getKeyCode()==KeyEvent.VK_N){
            if(enableNormalMapping){
                enableNormalMapping=false;
            }else{
                enableNormalMapping=true;
            }
        }else if(e.getKeyCode()==KeyEvent.VK_M){
            if(sunMove){
                sunMove=false;
            }else{
                sunMove=true;
            }
        }


        if(disableMouse){  //normal operation done by keyboard
            float step=0.1f;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_A:
                    case KeyEvent.VK_LEFT:
                        if(firstPersonMode){
                            tlr-=5;
                        }else{
                            tlr+=5;
                        }
                        dx=(float)Math.sin(Math.toRadians(tlr));
                        dz=(float)Math.cos(Math.toRadians(tlr));
                        break;
                    case KeyEvent.VK_D:
                    case KeyEvent.VK_RIGHT:
                        if(firstPersonMode){
                            tlr+=5;
                        }else{
                            tlr-=5;
                        }
                        dx=(float)Math.sin(Math.toRadians(tlr));
                        dz=(float)Math.cos(Math.toRadians(tlr));
                        break;
                    case KeyEvent.VK_W:
                    case KeyEvent.VK_UP:
                        if(firstPersonMode){
                            positionx += dx*step;
                            positionz -= dz*step;
                        }else {
                            positionx -= dx*step;
                            positionz -= dz*step;
                        }
                        break;
                    case KeyEvent.VK_S:
                    case KeyEvent.VK_DOWN:
                        if(firstPersonMode){
                            positionx -= dx*step;
                            positionz += dz*step;
                        }else{
                            positionx += dx*step;
                            positionz += dz*step;
                        }
                        break;
                    default:
                        break;
            }
        }else{
            float step=0.1f;
            switch (e.getKeyCode()) {   //debug operation done by both keyboard and mouse
                case KeyEvent.VK_A:
                case KeyEvent.VK_LEFT:
                    positionx -= (float)Math.sin(Math.toRadians(90-yaw))*step;
                    positionz -= (float)Math.cos(Math.toRadians(90-yaw))*step;
                    break;
                case KeyEvent.VK_D:
                case KeyEvent.VK_RIGHT:
                    positionx += (float)Math.sin(Math.toRadians(90-yaw))*step;
                    positionz += (float)Math.cos(Math.toRadians(90-yaw))*step;
                    break;
                case KeyEvent.VK_W:
                case KeyEvent.VK_UP:
                    dx=(float)Math.sin(Math.toRadians(yaw));
                    dz=-((float)Math.cos(Math.toRadians(yaw)));
                    positionx += (float)Math.sin(Math.toRadians(yaw))*step;
                    positionz -= (float)Math.cos(Math.toRadians(yaw))*step;
                    break;
                case KeyEvent.VK_S:
                case KeyEvent.VK_DOWN:
                    dx=-(float)Math.sin(Math.toRadians(yaw));
                    dz=((float)Math.cos(Math.toRadians(yaw)));
                    positionx -= (float)Math.sin(Math.toRadians(yaw))*step;
                    positionz += (float)Math.cos(Math.toRadians(yaw))*step;
                    break;
                case KeyEvent.VK_ESCAPE:
                    System.exit(0);
                    break;
                case KeyEvent.VK_SPACE:
                    if(this.firstPersonMode){
                        this.firstPersonMode=false;
                    }else{
                        this.firstPersonMode=true;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
//w

    }


    @Override
    public void reshape(GL3 gl, int width, int height) {
        super.reshape(gl, width, height);
        Shader.setProjMatrix(gl, Matrix4.perspective(60, width/(float)height, 0.05f, 100));
    }


    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
//        System.out.println("mouseClicked");
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {
//        System.out.println("mouseEntered");
    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {
//        System.out.println("mouseExited");
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
//        System.out.println("mousePressed");
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
//        System.out.println("mouseReleased");
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {  //for debug purpose
        if(!disableMouse){
            float xpos=mouseEvent.getX();
            float ypos=mouseEvent.getY();
            if(firstMouse)
            {
                lastX = xpos;
                lastY = ypos;
                firstMouse = false;
            }
            float xoffset = mouseEvent.getX() - lastX;
            float yoffset = lastY - mouseEvent.getY(); // reversed since y-coordinates range from bottom to top
            lastX = xpos;
            lastY = ypos;
            float sensitivity = 0.08f;
            xoffset *= sensitivity;
            yoffset *= sensitivity;
            yaw   += xoffset;
            pitch += yoffset;
            if(pitch > 89.0f)
                pitch = 89.0f;
            if(pitch < -89.0f)
                pitch = -89.0f;
        }
    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
//        System.out.println("mouseDragged");
    }

    @Override
    public void mouseWheelMoved(MouseEvent mouseEvent) {
//        System.out.println("mouseWheelMoved");
    }
}
