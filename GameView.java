package com.example.sensorplaytest3;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;


//TODO: Ball, Planet und SchwarzesLoch mit Vererbung realisieren
/** View-Class that creates the gaming levels */
public class GameView extends AppCompatActivity {

    private GenerateLevels mGenerateLevels;
    private SensorManager mSensorManager;

    //TODO: timer
    //TODO: menu
    //TODO: bestenliste
    //TODO: broker

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gametable);

        // Get an instance of the SensorManager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // instantiate our simulation view and set it as the activity's content
        mGenerateLevels = new GenerateLevels(this);
        mGenerateLevels.setBackgroundResource(R.drawable.space);
        setContentView(mGenerateLevels);
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Start the simulation
        mGenerateLevels.startSimulation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop the simulation
        mGenerateLevels.stopSimulation();

    }

    public void finishLevel(){
        nextLevel.proceed();
        this.recreate();
    }
    /** Class that creates the all the the stuff for the levels */
    class GenerateLevels extends FrameLayout implements SensorEventListener {

        private final Sensor mAccelerometer;
        private long lastTimeStamp;

        private float mSensorX;
        private float mSensorY;

        private final int mHorizontalBound;
        private final int mVerticalBound;

        private final Ball ball;
        private final List<BlackHole> blackHoles = new ArrayList<>();
        private final Planet planet;


        class BlackHole extends View {
            private final int radius = 200;
            private final int hPosX = (int) (Math.random() * mHorizontalBound - radius);
            private final int hPosY = (int) (Math.random() * mVerticalBound - radius);
            //TODO: mit steigendem Level vergrößern
            private final float angle = 2.5f;

            /** Constructor, set the Backgroundimage and adds it as a layer to the Activity */
            public BlackHole(Context context) {
                super(context);
                this.setBackgroundResource(R.drawable.spacehole);
                this.setLayerType(LAYER_TYPE_HARDWARE, null);
                addView(this, new ViewGroup.LayoutParams(radius * 2, radius * 2));
            }

            /** Vorlage: https://www.geeksforgeeks.org/find-if-a-point-lies-inside-or-on-circle/ */
            public boolean isBallInside(int posX, int posY) {
                return ((posX - hPosX) * (posX - hPosX)
                        + (posY - hPosY) * (posY - hPosY))
                        < (radius * radius);
            }

            public float[] getAngleAtPosition(int posX, int posY) {
                float[] angles = new float[2];

                // get x angle
                if (posX > hPosX) {
                    angles[0] = angle;
                }
                else if (posX < hPosX) {
                    angles[0] = -angle;
                }
                else {
                    angles[0] = 0;
                }

                // get y angle
                if (posY > hPosY) {
                    angles[1] = -angle;
                }
                else if (posY < hPosY) {
                    angles[1] = angle;
                }
                else {
                    angles[1] = 0;
                }

                return angles;
            }
        }


        /** creates the Ball with its physics*/
        class Ball extends View {
            private int bPosX = (int) (Math.random() * mHorizontalBound);
            private int bPosY = (int) (Math.random() * mVerticalBound);
            private float bVelX;
            private float bVelY;
            private final int radius = 60;
            private final List<BlackHole> blackholes;
            private final Planet planet;

            /** Constructor, set the Backgroundimage and adds it as a layer to the Activity */
            public Ball(Context context, List<BlackHole> blackHoles, Planet planet) {
                super(context);
                this.setBackgroundResource(R.drawable.ufo);
                this.setLayerType(LAYER_TYPE_HARDWARE, null);
                this.blackholes = blackHoles;
                this.planet = planet;
                addView(this, new ViewGroup.LayoutParams(radius, radius));
            }

            /** method to simulte the physicis of the ball, credits: AccelerometerPlay */
            public void computePhysics(float sensorX, float sensorY, float deltaT) {

                //check if Ball is in the finishing Planet
                if(planet.isBallInside(this.bPosX, this.bPosY, this.radius)){
                    //go to next level
                    // add black hole gravity/angles to board gravity/angles
                    float[] angles = planet.getAngleAtPosition(this.bPosX, this.bPosY);

                    // add black hole gravity/angles to board gravity/angles
                    sensorX += angles[0];
                    sensorY += angles[1];

                    bVelX *= 0.5;
                    bVelY *= 0.5;

                    finishLevel();
                }
                //check if Ball is inside BlackHole, if so, ad gravity
                boolean isInBlackHole = false;
                for (int i = 0; i < this.blackholes.size(); i++) {
                    BlackHole blackHole = this.blackholes.get(i);

                    if (blackHole.isBallInside(this.bPosX, this.bPosY)) {
                        isInBlackHole = true;
                        // include black hole gravity
                        float[] angles = blackHole.getAngleAtPosition(this.bPosX, this.bPosY);

                        // add black hole gravity/angles to board gravity/angles
                        sensorX += angles[0];
                        sensorY += angles[1];
                        bVelX *= 0.98;
                        bVelY *= 0.98;
                    }
                }

                final float ax = -sensorX * 100;
                final float ay = sensorY * 100;

                //Credits: VorlagenApp
                bPosX += bVelX * deltaT + ax * deltaT * deltaT / 2;
                bPosY += bVelY * deltaT + ay * deltaT * deltaT / 2;
                bVelX += ax * deltaT;
                bVelY += ay * deltaT;


                // if in blackhole, slow down
                if (isInBlackHole) {
                    bVelX *= 0.98;
                    bVelY *= 0.98;
                }
            }

            /** computes the border, so that the ball cant fall of, credits: AccelerometerPlay
             * collision is calculated with the Verlet integrator */
            public void resolveCollisionWithBounds() {
                final int xmax = mHorizontalBound - ball.radius;
                final int xmin = ball.radius;

                final int ymax = mVerticalBound - ball.radius*2;
                final int ymin = ball.radius;

                final int x = bPosX;
                final int y = bPosY;
                if (x > xmax) {
                    bPosX = xmax;
                    bVelX = 0;
                } else if (x < xmin) {
                    bPosX = xmin;
                    bVelX = 0;
                }
                if (y > ymax) {
                    bPosY = ymax;
                    bVelY = 0;
                } else if (y < ymin) {
                    bPosY = ymin;
                    bVelY = 0;
                }
            }

            /** update the balls's position, only if some time has passed, Credits: AccelerometerPlay
             * modified by me to fit the projects.
             * @param sx x coordinate from sensor
             * @param sy y coordinate from sensor
             * @param now times of calling the function
             */
            public void update(float sx, float sy, long now) {
                final long t = now;
                if (lastTimeStamp != 0) {
                    final float dT = (float) (t - lastTimeStamp) / 1000.f;
                    this.computePhysics(sx, sy, dT);
                }
                lastTimeStamp = t;
                this.resolveCollisionWithBounds();

                //TODO Check if Ball is inside Hole
            }

            /** returns the current X-Coordinate */
            public int getPosX() {
                return this.bPosX;
            }
            /** returns the current Y-Coordinate */
            public int getPosY() {
                return this.bPosY;
            }
        }

        class Planet extends View{
            private final int radius = 100;
            private int pPosX = (int) (Math.random() * mHorizontalBound - radius);
            private int pPosY = (int) (Math.random() * mVerticalBound - radius);
            private final float angle = 7f;

            public Planet(Context context){
                super(context);
                this.setBackgroundResource(R.drawable.mars);
                this.setLayerType(LAYER_TYPE_HARDWARE, null);
                addView(this, new ViewGroup.LayoutParams(radius * 2, radius * 2));
            }

            /** Vorlage: https://www.geeksforgeeks.org/check-if-a-circle-lies-inside-another-circle-or-not/ */
            public boolean isBallInside(int ballX, int ballY, int radius) {
                //posX left upper Corner of the image, so we have to move it to the center for the calculation
                //circle1 = planet; circle2=ball
                int x1 = pPosX + this.radius, y1 = pPosY + this.radius;
                int x2 = ballX + radius, y2 = ballY + radius;
                int distSq = (int)Math.sqrt(((x1 - x2) * (x1 - x2))
                                    + ((y1 - y2) * (y1 - y2)));
                return((distSq + radius) <= this.radius*1.25); //*1.25 so that the ball doesnet need to be perfectly on the center point
            }

            public float[] getAngleAtPosition(int posX, int posY) {
                float[] angles = new float[2];

                // get x angle
                if (posX > pPosX) {
                    angles[0] = angle;
                }
                else if (posX < pPosX) {
                    angles[0] = -angle;
                }
                else {
                    angles[0] = 0;
                }

                // get y angle
                if (posY > pPosY) {
                    angles[1] = -angle;
                }
                else if (posY < pPosY) {
                    angles[1] = angle;
                }
                else {
                    angles[1] = 0;
                }

                return angles;
            }

            public void resolveCollisionWithBounds() {
                final int xmax = mHorizontalBound - planet.radius;
                final int xmin = planet.radius;

                final int ymax = mVerticalBound - planet.radius;
                final int ymin = planet.radius;

                final int x = pPosX;
                final int y = pPosY;
                if (x > xmax) {
                    pPosX = xmax;
                } else if (x < xmin) {
                    pPosX = xmin;
                }
                if (y > ymax) {
                    pPosY = ymax;
                } else if (y < ymin) {
                    pPosY = ymin;
                }
            }
        }

        /** Constructor abgewandelt aber basierend auf AccelerometerPlay*/
        public GenerateLevels(Context context) {
            super(context);

            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            //TODO funktioniert das wirklich??
            //check for navigation bar
            //Credit: https://stackoverflow.com/questions/20264268/how-do-i-get-the-height-and-width-of-the-android-navigation-bar-programmatically
            int navigationBar = 0;
            boolean hasNavigationBar = ViewConfiguration.get(context).hasPermanentMenuKey();
            if(hasNavigationBar){
                Resources resources = context.getResources();
                int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
                if (resourceId > 0) {
                    navigationBar = resources.getDimensionPixelSize(resourceId);
                }
            }

            //get the display width and height
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mHorizontalBound = metrics.widthPixels - navigationBar;
            mVerticalBound = metrics.heightPixels - navigationBar;


            // generate random black holes
            blackHoles.clear();
            //TODO: nochmal abändern
            int blackHoleCounter = nextLevel.getLevel();
            for (int i = 0; i < blackHoleCounter; i++) {
                BlackHole blackHole = new BlackHole(getContext());
                blackHoles.add(blackHole);
            }


            //Order is relevant for what is in front of what an screen
            planet = new Planet(getContext());
            ball = new Ball(getContext(), blackHoles, planet);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inDither = true;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;

        }

        /** starts the Simulation by registering a SensorManager, Credits: AccelerometerPlay*/
        public void startSimulation() {
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        }

        /** stops the Simulation by unregistering a SensorManager, Credits: AccelerometerPlay*/
        public void stopSimulation() {
            mSensorManager.unregisterListener(this);
        }


        /** reads the Sensor data into variables.  */
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
                return;
            mSensorX = event.values[0];
            mSensorY = event.values[1];

        }

        /** draws the ball onto the canvas*/
        @Override
        protected void onDraw(Canvas canvas) {

            final long now = System.currentTimeMillis();
            final float sx = mSensorX;
            final float sy = mSensorY;

            //get Ball Position and draw it
            ball.update(sx, sy, now);

            int ballXOrigin = this.ball.getPosX() - this.ball.radius;
            int ballYOrigin = this.ball.getPosY() - this.ball.radius;

            this.ball.setX(ballXOrigin);
            this.ball.setY(ballYOrigin);

            //TODO hole mit steigendem Level kleiner werden lassen
            final int planetXOrigin = planet.pPosX - planet.radius;
            final int planetYOrigin = planet.pPosY - planet.radius;
            planet.resolveCollisionWithBounds();
            planet.setX(planetXOrigin);
            planet.setY(planetYOrigin);


            // draw blackholes
            for (int i = 0; i < this.blackHoles.size(); i++) {
                BlackHole blackHole = this.blackHoles.get(i);

                final int blackHoleXOrigin = blackHole.hPosX - blackHole.radius;
                final int blackHoleYOrigin = blackHole.hPosY - blackHole.radius;
                blackHole.setX(blackHoleXOrigin);
                blackHole.setY(blackHoleYOrigin);
            }

            // and make sure to redraw asap
            invalidate();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }



}

