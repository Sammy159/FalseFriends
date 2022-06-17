package com.example.sensorplaytest3;

import android.content.Context;
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
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;


//TODO : Herausfinden ob der Ball am Anfang immer nach unten rollt oder nur weil mein Handy nicht gerade liegt

/** View-Class that creates the gaming levels */
public class GameView extends AppCompatActivity {

    private GenerateLevels mGenerateLevels;
    private SensorManager mSensorManager;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gametable);

        // Get an instance of the SensorManager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // instantiate our simulation view and set it as the activity's content
        mGenerateLevels = new GenerateLevels(this);
        mGenerateLevels.setBackgroundResource(R.drawable.wood);
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

    /** Class that creates the all the the stuff for the levels */
    class GenerateLevels extends FrameLayout implements SensorEventListener {

        private static final float sBallDiameter = 0.004f;
        private final int displayWidth;
        private final int displayHeight;
        private final Sensor mAccelerometer;
        private long lastTimeStamp;
        private final float mMetersToPixelsX;
        private final float mMetersToPixelsY;
        private float mXOrigin;
        private float mYOrigin;
        private float mSensorX;
        private float mSensorY;
        //TODO : Z Achse berücksitigen, in späteren Level
        private float mSensorZ;
        private float mHorizontalBound;
        private float mVerticalBound;

        private Ball ball;

        /** creates the Ball with its physics*/
        class Ball extends View {
            private float mPosX = (float) Math.random();
            private float mPosY = (float) Math.random();
            private float mVelX;
            private float mVelY;

            /** Constructor, set the Backgroundimage and adds it as a layer to the Activity */
            public Ball(Context context) {
                super(context);
                this.setBackgroundResource(R.drawable.ball);
                this.setLayerType(LAYER_TYPE_HARDWARE, null);
                addView(this, new ViewGroup.LayoutParams(displayWidth, displayHeight));
            }

            /** method to simulte the physicis of the ball, credits: AccelerometerPlay */
            public void computePhysics(float sx, float sy, float dT) {

                final float ax = -sx/5;
                final float ay = -sy/5;

                mPosX += mVelX * dT + ax * dT * dT / 2;
                mPosY += mVelY * dT + ay * dT * dT / 2;

                mVelX += ax * dT;
                mVelY += ay * dT;
            }

            /** computes the border, so that the ball cant fall of, credits: AccelerometerPlay
             * collision is calculated with the Verlet integrator */
            public void resolveCollisionWithBounds() {
                final float xmax = mHorizontalBound;
                final float ymax = mVerticalBound;
                final float x = mPosX;
                final float y = mPosY;
                if (x > xmax) {
                    mPosX = xmax;
                    mVelX = 0;
                } else if (x < -xmax) {
                    mPosX = -xmax;
                    mVelX = 0;
                }
                if (y > ymax) {
                    mPosY = ymax;
                    mVelY = 0;
                } else if (y < -ymax) {
                    mPosY = -ymax;
                    mVelY = 0;
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
            }

            /** returns the current X-Coordinate */
            public float getPosX() {
                return this.mPosX;
            }
            /** returns the current Y-Coordinate */
            public float getPosY() {
                return this.mPosY;
            }
        }

        /** starts the Simulation by registering a SensorManager*/
        public void startSimulation() {
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        }

        /** stops the Simulation by unregistering a SensorManager*/
        public void stopSimulation() {
            mSensorManager.unregisterListener(this);
        }

        /** Constructor */
        public GenerateLevels(Context context) {
            super(context);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            float mXDpi = metrics.xdpi;
            float mYDpi = metrics.ydpi;
            mMetersToPixelsX = mXDpi / 0.0254f;
            mMetersToPixelsY = mYDpi / 0.0254f;

            // rescale the ball
            displayWidth = (int) (sBallDiameter * mMetersToPixelsX + 10.75f);
            displayHeight = (int) (sBallDiameter * mMetersToPixelsY + 10.75f);
            ball = new Ball(getContext());

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inDither = true;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            // compute the origin of the screen relative to the origin of
            // the bitmap
            mXOrigin = (w - displayWidth) * 0.5f;
            mYOrigin = (h - displayHeight) * 0.5f;
            mHorizontalBound = ((w / mMetersToPixelsX - sBallDiameter) * 0.5f);
            mVerticalBound = ((h / mMetersToPixelsY - sBallDiameter) * 0.5f);
        }

        /** reads the Sensor data into variables.  */
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
                return;
            mSensorX = event.values[0];
            mSensorY = event.values[1];
            mSensorZ = event.values[2];
        }

        /** draws the ball onto the canvas*/
        @Override
        protected void onDraw(Canvas canvas) {
            /*
             * Compute the new position of our object, based on accelerometer
             * data and present time.
             */
            final Ball ball = this.ball;
            final long now = System.currentTimeMillis();
            final float sx = mSensorX;
            final float sy = mSensorY;

            ball.update(sx, sy, now);

            final float xc = mXOrigin;
            final float yc = mYOrigin;
            final float xs = mMetersToPixelsX;
            final float ys = mMetersToPixelsY;
                /*
                 * We transform the canvas so that the coordinate system matches
                 * the sensors coordinate system with the origin in the center
                 * of the screen and the unit is the meter.
                 */
                final float x = xc + ball.getPosX() * xs;
                final float y = yc - ball.getPosY() * ys;
                this.ball.setTranslationX(x);
                this.ball.setTranslationY(y);

            // and make sure to redraw asap
            invalidate();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }



}

