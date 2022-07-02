package com.example.falsefriends;

import android.content.Context;
import android.view.View;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** parent class for ball (here UFO), obstacles (here BlackHole) and goal(here Planet) <br>
 * contains common variables like radius and coordinates <br>
 * and methods <br>
 * based on the template "AccelerometerPlay", but evolved and change by me
 * @see <a href="https://github.com/googlearchive/android-AccelerometerPlay/blob/master/app/src/main/java/com/example/android/accelerometerplay/AccelerometerPlayActivity.java">AccelerometerPlay - Activity</a>*/
public class Object extends View {

    public int radius;
    public int posX;
    public int posY;
    public float angle;
    public int horizontalBound;
    public int verticalBound;

    /** Default Constructor*/
    public Object(Context context) {
        super(context);
    }

    /** calculates the angle with needs to be added to the ufo if it is inside a blackhole or planet
     *
     * @param pointX x coordinate of the point that needs to be checked
     * @param pointY y coordinate of the point that needs to be checked
     * @return array of the angles which need to be further considered
     */
    public float[] getAngleAtPosition(int pointX, int pointY) {
        float[] angles = new float[2];

        // check if x angles need to be considered
        if (pointX > posX) angles[0] = angle;
        else if (pointX < posX) angles[0] = -angle;
        else angles[0] = 0;

        // check if x angles need to be considered
        if (pointY > posY) angles[1] = -angle;
        else if (pointY < posY) angles[1] = angle;
        else angles[1] = 0;

        return angles;
    }

    /** Sets the coordinates to new random positions inside the bounds*/
    public void resetPos(){
        //random Position between radius and horizontalBound - radius
        posX = ThreadLocalRandom.current().nextInt(radius, horizontalBound - radius);
        posY = ThreadLocalRandom.current().nextInt(radius, verticalBound - radius);
    }

    /** computes the border, so that the ball cant fall of, credits: AccelerometerPlay
     * collision is calculated with the Verlet integrator
     * Credits: taken over from the AccelerometerPlay App
     * @see <a href="https://github.com/googlearchive/android-AccelerometerPlay">AccelerometerPlay App</a>*/
    public void resolveCollisionWithBounds() {
        final int xmax = horizontalBound - radius;
        final int xmin = radius;

        final int ymax = verticalBound - radius*2;
        final int ymin = radius;

        final int x = posX;
        final int y = posY;
        if (x > xmax) {
            posX = xmax;
        } else if (x < xmin) {
            posX = xmin;
        }
        if (y > ymax) {
            posY = ymax;
        } else if (y < ymin) {
            posY = ymin;
        }
    }
}

/**
 * Obstacle (false friends) class, here called Blackholes, child class of Object
 */
class BlackHole extends Object {

    /** Constructor, set the Backgroundimage and adds it as a layer to the Activity */
    public BlackHole(Context context, int id) {
        super(context);
        this.setBackgroundResource(R.drawable.spacehole);
        this.setLayerType(LAYER_TYPE_HARDWARE, null);
        this.setId(id);
    }

    /** checks if a point is inside a circle (in this case the Blackhole)
     * Credits: Internet https://www.geeksforgeeks.org/find-if-a-point-lies-inside-or-on-circle/
     * @param pointX x coordinates of the point which is to be checked
     * @param pointY y coordinates of the point which is to be checked
     * @return whether the point is inside the circle (Blackhole) or not
     * @see <a href="https://www.geeksforgeeks.org/find-if-a-point-lies-inside-or-on-circle/">Template</a>*/
    public boolean isPointInsideCircle(int pointX, int pointY) {
        return ((pointX - this.posX) * (pointX - this.posX)
                + (pointY - this.posY) * (pointY - this.posY))
                < (this.radius * this.radius);
    }

}

/** creates the Ball (here UFO) with its physics, child class of Object <br>
 * template: AccelerometerPlay App, but changed and evolved by me
 * @see <a href="https://github.com/googlearchive/android-AccelerometerPlay">AccelerometerPlay App</a>*/
class UFO extends Object{

    private long lastTimeStamp;
    private float velX;
    private float velY;
    private final List<BlackHole> blackholes;
    private final Planet planet;
    private final GameView.LevelManager levelManager;


    /** Constructor, sets the Backgroundimage and adds it as a layer to the Activity, takes the giving objects and saves them itself for further processing
     * @param blackHoles list of existing blackHole objects
     * @param context current context
     * @param levelManager levelmanager object
     * @param planet existing planet object*/
    public UFO(Context context, List<BlackHole> blackHoles, Planet planet, GameView.LevelManager levelManager) {
        super(context);
        this.setBackgroundResource(R.drawable.ufo);
        this.setLayerType(LAYER_TYPE_HARDWARE, null);
        this.blackholes = blackHoles;
        this.planet = planet;
        this.levelManager = levelManager;
    }

    /** Overrides the object method, because the velocity needs to be set to zero on level restart*/
    public void resetPos(){
        //random Position between radius and horizontal Bound - radius
        posX = ThreadLocalRandom.current().nextInt(radius, horizontalBound - radius);
        posY = ThreadLocalRandom.current().nextInt(radius, verticalBound - radius);
        velX = 0;
        velY = 0;
    }

    /** method to simulate the physics of the UFO
     * Template: AccelerometerPlay, but edited and evolved by me
     * @param deltaT delta time
     * @param sensorX x sensor data
     * @param sensorY y sensor data
     * @see <a href="https://github.com/googlearchive/android-AccelerometerPlay">AccelerometerPlay App</a>*/
    public void computePhysics(float sensorX, float sensorY, float deltaT) {

        //check if UFO is in the Planet, if so, complete level
        if(planet.isBallInside(this.posX, this.posY, this.radius)){
            //go to next level
            // add black hole gravity/angles to board gravity/angles
            float[] angles = planet.getAngleAtPosition(this.posX, this.posY);

            // add black hole gravity/angles to board gravity/angles
            sensorX += angles[0];
            sensorY += angles[1];

            velX *= 0.5;
            velY *= 0.5;

            levelManager.finishLevel();
        }

        //check if Ball is inside BlackHole, if so, ad gravity
        for (int i = 0; i < this.blackholes.size(); i++) {
            BlackHole blackHole = this.blackholes.get(i);

            if (blackHole.isPointInsideCircle(this.posX, this.posY)) {
                // include black hole gravity
                float[] angles = blackHole.getAngleAtPosition(this.posX, this.posY);

                // add black hole gravity/angles to board gravity/angles
                sensorX += angles[0];
                sensorY += angles[1];
                velX *= 0.98;
                velY *= 0.98;
            }
        }

        final float ax = -sensorX * 100;
        final float ay = sensorY * 100;

        //Credits: AccelerometerPlay App
        posX += velX * deltaT + ax * deltaT * deltaT / 2;
        posY += velY * deltaT + ay * deltaT * deltaT / 2;
        velX += ax * deltaT * (levelManager.getLevelNr()*0.2);
        velY += ay * deltaT * (levelManager.getLevelNr()*0.2);

    }

    /** updates the UFO's position, only if some time has passed,
     * Template: AccelerometerPlay
     * modified by me to fit the projects.
     * @param sx x coordinate from sensor
     * @param sy y coordinate from sensor
     * @param now times of calling the function
     * @see <a href="https://github.com/googlearchive/android-AccelerometerPlay">AccelerometerPlay App</a>
     */
    public void update(float sx, float sy, long now) {
        final long t = now;
        if (lastTimeStamp != 0) {
            final float dT = (float) (t - lastTimeStamp) / 1000.f;
            this.computePhysics(sx, sy, dT);
        }
        lastTimeStamp = t;
        this.resolveCollisionWithBounds();
    }

    /** Overrides the Object method, because here the velocity needs to be changed
     * Template: AccelerometerPlay, but changed by me
     *  @see <a href="https://github.com/googlearchive/android-AccelerometerPlay">AccelerometerPlay App</a>
     */
    public void resolveCollisionWithBounds() {
        final int xmax = horizontalBound - radius;
        final int xmin = radius;

        final int ymax = verticalBound - radius*2;
        final int ymin = radius;

        final int x = posX;
        final int y = posY;
        if (x > xmax) {
            posX = xmax;
            velX = 0;
        } else if (x < xmin) {
            posX = xmin;
            velX = 0;
        }
        if (y > ymax) {
            posY = ymax;
            velY = 0;
        } else if (y < ymin) {
            posY = ymin;
            velY = 0;
        }
    }
    /**
     * gets the current x coordinate
     * @return current x coordinate
     * */
    public int getPosX() {
        return this.posX;
    }

    /**
     * gets the current y coordinate
     * @return current y coordinate
     * */
    public int getPosY() {
        return this.posY;
    }
}

/**
 * creates the goal (here Planet), child class of Object <br>
 * */
class Planet extends Object{

    /**
     * Constructor, sets the Backgroundimage and adds it as a layer to the Activity
     * @param context current context
     * */
    public Planet(Context context){
        super(context);
        this.setBackgroundResource(R.drawable.mars);
        this.setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    /**
     * checks if the UFO is inside the goal (in this case the Planet)
     * Credits: Internet https://www.geeksforgeeks.org/check-if-a-circle-lies-inside-another-circle-or-not/
     * @param ballX x coordinates of the UFO
     * @param ballY y coordinates of the UFO
     * @param radius radius of the UFO
     * @return whether the UFO is inside the circle (Planet) or not
     * @see <a href="https://www.geeksforgeeks.org/check-if-a-circle-lies-inside-another-circle-or-not/">Template</a>
     * */
    public boolean isBallInside(int ballX, int ballY, int radius) {
        //posX left upper Corner of the image, so we have to move it to the center for the calculation
        int x1 = posX + this.radius, y1 = posY + this.radius;
        int x2 = ballX + radius, y2 = ballY + radius;
        int distSq = (int)Math.sqrt(((x1 - x2) * (x1 - x2))
                + ((y1 - y2) * (y1 - y2)));
        return((distSq + radius) <= this.radius*1.25); //*1.25 so that the ball doesn't need to be perfectly on the center point
    }
}