package unsw.graphics.world;

import java.util.Random;

public class Particle {
    public float life; // how alive it iss
    public float r, g, b; // color
    public float x, y, z; // position
    public float speedX, speedY, speedZ; // speed in the direction

    private Random rand = new Random();

    // Constructor
    public Particle() {
        drop();
    }

    public void drop() {
        // Set the initial position
        z = 0.0f;
        y=rand.nextFloat();
        if(rand.nextFloat()>=0.5){
            y=-y;
        }
        x =rand.nextFloat()/2f;
        if(rand.nextFloat()>=0.5){
            x=-x;
        }
        speedX=0;
        speedY=-(0.01f+rand.nextFloat()*0.01f);
        speedZ=0;
        r = 1;
        g = 1;
        b = 1;
        life = 1.0f;
    }
}