package com.example.falsefriends;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.List;


/** View-Class that provides the basic activity features, <br>
 * implements the LevelManager Class and the functionality for the MQTT Broker*/
public class GameView extends AppCompatActivity {

    private LevelManager levelManager;
    private SensorManager sensorManager;
    public int levelNr = 1;

    //MQTT Variables
    private static final String sub_topic = "sensor/data";
    private static final String pub_topic = "sensehat/message";
    private final int qos = 0; // MQTT quality of service
    private String clientId;
    private final MemoryPersistence persistence = new MemoryPersistence();
    private MqttClient client;
    private final String TAG = GameView.class.getSimpleName();
    SharedPreferences prefBroker;
    private String BROKER;
    private boolean boolBroker = true;

    private SoundPool soundPool;
    private boolean soundPoolBereit;
    private int winSoundID;
    private boolean soundOn = true;


    /**
     * Gets the sensor manager, initializes the sharedprefs for the broker, so that it contains a default value <br>
     *  and instantiate our simulation view and set it as the activity's content
     *  */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gametable);

        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //initializes the sharedprefs for the broker, so that it contains a default value
        prefBroker = getSharedPreferences("TCP", Context.MODE_PRIVATE);
        BROKER = prefBroker.getString("broker", "tcp://192.168.137.2:1883");  // default: "tcp://192.168.137.2:1883" | "tcp://100.80.182.165:1883"

        //Soundeffects, book Android p. 283
        SoundPool.Builder builder = new SoundPool.Builder();
        AudioAttributes.Builder attrsBuilder = new AudioAttributes.Builder();
        attrsBuilder.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC);
        attrsBuilder.setUsage(AudioAttributes.USAGE_MEDIA);
        builder.setAudioAttributes(attrsBuilder.build());
        builder.setMaxStreams(5);
        soundPool = builder.build();
        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> soundPoolBereit = true);
        winSoundID = soundPool.load(this, R.raw.win, 1);


        // instantiate our simulation view and set it as the activity's content
        levelManager = new LevelManager(this);
        levelManager.setBackgroundResource(R.drawable.space);
        setContentView(levelManager);
    }

    /** Creates an ActionBar Menu*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    /** Checks if a broker ip was sett and starts the simulation of the game*/
    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        boolBroker = intent.getBooleanExtra("broker", false);
        soundOn = intent.getBooleanExtra("soundOn", true);
        levelManager.startSimulation(boolBroker);
    }

    /** Stops the simulation if the activity is paused*/
    @Override
    protected void onPause() {
        super.onPause();
        levelManager.stopSimulation(boolBroker);
    }

    /**
     * Calls the leaderboard activity upon game completion and passes the time needed by the player to finish the game
     * @param time seconds needed by the player to finish the game
     * */
    private void finishGame(int time){
        Intent intent = new Intent(this, Leaderboard.class);
        intent.putExtra("finished", true);
        intent.putExtra("time", time);
        this.startActivity(intent);
    }

    /**
     * Contains the logic if a button from the menu is pressed <br>
     * Template: Book "Android" by Dirk Louis, Peter Müller
     * @param item item which was pressed
     * @return whether the selection was successful
     * @see <a href="https://www.hanser-elibrary.com/isbn/9783446445987">Book "Android"</a>
     * */
    @SuppressLint("NonConstantResourceId")
    //Android Book page 221
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.btn_liste: showLeaderboard(); return true;
            case R.id.btn_options: showOptions(); return true;
            case R.id.btn_restart: restartLevel(); return true;
            default: return false;
        }
    }

    /** Passes on to the leaderboard activity*/
    public void showLeaderboard(){
        Intent intent = new Intent(this, Leaderboard.class);
        intent.putExtra("finished", false);
        this.startActivity(intent);
    }

    /** Passes on to the Options activity*/
    public void showOptions(){
        Intent intent = new Intent(this, Options.class);
        this.startActivity(intent);
    }

    /** Restarts the level by recreating the activity */
    public void restartLevel(){
        this.recreate();
    }

    /** Class that creates everything need for the levels */
    class LevelManager extends FrameLayout implements SensorEventListener {

        private final Sensor accelerometerSensor;
        private float sensorX;
        private float sensorY;
        private final int horizontalBound;
        private final int verticalBound;

        private final Stopwatch stopwatch;
        private final TextView tv_stopwatch;

        /** initiates objects with values from the needed classes*/
        private UFO UFO;
        private final int ballRadius = 60;
        private final List<BlackHole> blackHoles = new ArrayList<>();
        private final int radiusBlackHole = 200;
        private final float angleBlackHole = 2.5f;
        private Planet planet;
        private final int planetRadius = 100;
        private final float planetAngle = 7f;

        /** Checks if the maximal level is achieved, if so finishes the game, if not increases the level number and resets objects for the next level*/
        public void finishLevel(){
            //book Android, page  284
            if(soundPoolBereit) {
                float lautstaerkeLinks = 1.0f;
                float lautstaerkeRechts = 1.0f;
                if(!soundOn) {
                    lautstaerkeLinks = 0f;
                    lautstaerkeRechts = 0f;
                }
                float geschwindigkeit = 1.0f;
                int endlosschleife = 0; // nur einmal abspielen
                soundPool.play(winSoundID, lautstaerkeLinks, lautstaerkeRechts, 1, endlosschleife, geschwindigkeit);
            }

            if(levelNr == 10)
            {
                stopwatch.stopTimer();
                int time = stopwatch.getTime();
                if(boolBroker) publish(pub_topic, "heart");
                soundPool.release(); //Freigeben der Ressource
                finishGame(time);
            }

            //send message to pi
            String[] messages = {"rot", "gruen", "blau", "weiss", "gelb"};
            if(boolBroker) publish(pub_topic, messages[levelNr%5]);

            //remove old black holes from view
            for (int i = 0; i < blackHoles.size(); i++) {
                View view = findViewById(i);
                ((ViewGroup)this).removeView(view);
            }

            //increase level
            levelNr++;

            resetObjectsPositions();

            planet.bringToFront();
            UFO.bringToFront();
            tv_stopwatch.bringToFront();
        }

        /** Calculates a new position for the ball, obstacle and goal for the next level, checks the new position for overlay with others, so that overlaying is minimal*/
        private void resetObjectsPositions(){
            //set random position for objects
            UFO.resetPos();
            planet.resetPos();

            //spawn new black holes
            blackHoles.clear();
            for (int i = 0; i < levelNr; i++) {
                BlackHole blackHole = new BlackHole(getContext(), i);
                blackHole.angle = angleBlackHole + (0.1f*levelNr);
                blackHole.radius = radiusBlackHole;
                blackHole.horizontalBound = horizontalBound;
                blackHole.verticalBound = verticalBound;
                blackHole.resetPos();
                //to avoid that blackholes overlap with the planet
                if(blackHole.isPointInsideCircle(planet.posX, planet.posY)) blackHole.resetPos();
                //if more the one blackhole exists
                if(i > 0) {
                    for (int j = 0; j < blackHoles.size(); j++) {
                        BlackHole bhTemp = blackHoles.get(j);
                        if (bhTemp.isPointInsideCircle(blackHole.posX, blackHole.posY)) {
                            blackHole.resetPos();
                            j=0;
                        }
                    }
                }
                addView(blackHole,new ViewGroup.LayoutParams(blackHole.radius * 2, blackHole.radius * 2));
                blackHoles.add(blackHole);
            }

            //to avoid that ball spawns inside planet
            if(planet.isBallInside(UFO.posX, UFO.posY, UFO.radius)) UFO.resetPos();
        }

        /** Constructor for the LevelManager Class, based on the template AccelerometerPlay but changed and extended
         * @param context current context
         * @see <a href="https://github.com/googlearchive/android-AccelerometerPlay">AccelerometerPlay</a> */
        public LevelManager(Context context) {
            super(context);
            //get sensor
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            //textview for stopwatch
            tv_stopwatch = new TextView(context);
            tv_stopwatch.setTextColor(Color.RED);
            addView(tv_stopwatch,new ViewGroup.LayoutParams(150, 50));
            stopwatch = new Stopwatch(tv_stopwatch);

            //get the width and height of the display
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            horizontalBound = metrics.widthPixels;
            verticalBound = metrics.heightPixels;

            //Order is relevant for what is in front of what an screen
            //creates the object objects
            createBlackhole();
            createPlanet();
            createUFO();

            stopwatch.startTimer();
        }

        /** initializes an object from the UFO class and sets all needed values*/
        private void createUFO(){
            UFO = new UFO(getContext(), blackHoles, planet, this);
            UFO.radius = ballRadius;
            UFO.horizontalBound = horizontalBound;
            UFO.verticalBound = verticalBound;
            UFO.resetPos();
            addView(UFO,new ViewGroup.LayoutParams(UFO.radius * 2, UFO.radius * 2));
        }

        /** initializes an object from the Blackhole class and sets all needed values*/
        private void createBlackhole(){
            BlackHole blackHole = new BlackHole(getContext(), 0);
            blackHole.radius = radiusBlackHole;
            blackHole.horizontalBound = horizontalBound;
            blackHole.verticalBound = verticalBound;
            blackHole.resetPos();
            addView(blackHole,new ViewGroup.LayoutParams(blackHole.radius * 2, blackHole.radius * 2));
            blackHoles.add(blackHole);
        }

        /** initializes an object from the Planet class and sets all needed values*/
        private void createPlanet(){
            planet = new Planet(getContext());
            planet.radius = planetRadius;
            planet.angle = planetAngle;
            planet.horizontalBound = horizontalBound;
            planet.verticalBound = verticalBound;
            planet.resetPos();
            addView(planet,new ViewGroup.LayoutParams(planet.radius * 2, planet.radius * 2));
        }

        /** starts the Simulation by registering a SensorManager, Credits: AccelerometerPlay*/
        public void startSimulation(boolean boolBroker) {
            if(boolBroker){
                connect(BROKER);
                //Only subscribe if connection established
                subscribe(sub_topic);
            }
            else sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
            stopwatch.startTimer();
        }

        /** stops simulation, if broker is connected, disconnect it. if not unregister the sensorListener, stops the stopwatch
         * based on the template AccelerometerPlay but changed and extended
         * @param boolBroker is broker is connected or not*/
        public void stopSimulation(boolean boolBroker) {
            if(boolBroker){
                disconnect();
            }
            else sensorManager.unregisterListener(this);
            stopwatch.stopTimer();
        }


        /** reads the sensor data into variables, but only if no broker is connect
         * based on the template AccelerometerPlay but changed and extended
         * @param event sensorevent*/
        @Override
        public void onSensorChanged(SensorEvent event) {
            // don't scan device sensor when data comes from broker
            if (boolBroker) {
                return;
            }

            if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
                return;
            sensorX = event.values[0];
            sensorY = event.values[1];

        }

        /** draws the objects onto the canvas
         * @param canvas drawing place*/
        @Override
        protected void onDraw(Canvas canvas) {

            final long now = System.currentTimeMillis();
            final float sx = sensorX;
            final float sy = sensorY;

            //get Ball Position and draw it
            UFO.update(sx, sy, now);

            int ballXOrigin = this.UFO.getPosX() - this.UFO.radius;
            int ballYOrigin = this.UFO.getPosY() - this.UFO.radius;

            this.UFO.setX(ballXOrigin);
            this.UFO.setY(ballYOrigin);

            final int planetXOrigin = planet.posX - planet.radius;
            final int planetYOrigin = planet.posY - planet.radius;
            planet.resolveCollisionWithBounds();
            planet.setX(planetXOrigin);
            planet.setY(planetYOrigin);


            // draw blackholes
            for (int i = 0; i < this.blackHoles.size(); i++) {
                BlackHole blackHole = this.blackHoles.get(i);

                final int blackHoleXOrigin = blackHole.posX - blackHole.radius;
                final int blackHoleYOrigin = blackHole.posY - blackHole.radius;
                blackHole.setX(blackHoleXOrigin);
                blackHole.setY(blackHoleYOrigin);
            }

            //redraw
            invalidate();
        }

        public int getLevelNr(){
            return levelNr;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }


    //MQTT Code

    /**
     * Connect to broker and
     * @param broker Broker to connect to
     * @author Prof. Schäfer
     */
    private void connect (String broker) {
        try {
            clientId = MqttClient.generateClientId();
            client = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setConnectionTimeout(5);
            Log.d(TAG, "Connecting to broker: " + broker);
            client.connect(connOpts);
            Log.d(TAG, "Connected with broker: " + broker);
        } catch (MqttException me) {
            //inform the user about failed connected
            Context context = getApplicationContext();
            CharSequence text = "Connection failed!";
            int duration = Toast.LENGTH_SHORT;
            Toast.makeText(context, text, duration).show();

            Log.e(TAG, "Reason: " + me.getReasonCode());
            Log.e(TAG, "Message: " + me.getMessage());
            Log.e(TAG, "localizedMsg: " + me.getLocalizedMessage());
            Log.e(TAG, "cause: " + me.getCause());
            Log.e(TAG, "exception: " + me);
        }
    }

    /**
     * Subscribes to a given topic and saves the passed sensor data
     * @param topic Topic to subscribe to
     * @author Prof. Schäfer
     */
    private void subscribe(String topic) {
        try {
            client.subscribe(topic, qos, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage msg) throws Exception {
                    String message = new String(msg.getPayload());
                    String[] sensorDataStr = message.split(",");

                    levelManager.sensorX = Float.parseFloat(sensorDataStr[1]) * 5;
                    levelManager.sensorY = Float.parseFloat(sensorDataStr[0]) * 5;

                    Log.d(TAG, "Message with topic " + topic + " arrived: " + message);
                }
            });
            Log.d(TAG, "subscribed to topic " + topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    /**
     * Publishes a message via MQTT (with fixed topic)
     * @param topic topic to publish with
     * @param msg message to publish with publish topic
     * @author Prof. Schäfer
     */
    private void publish(String topic, String msg) {
        MqttMessage message = new MqttMessage(msg.getBytes());
        message.setQos(qos);
        try {
            client.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unsubscribe from default topic (please unsubscribe from further
     * topics prior to calling this function)
     * @author Prof. Schäfer
     */
    private void disconnect() {
        try {
            client.unsubscribe(sub_topic);
        } catch (MqttException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
        try {
            Log.d(TAG, "Disconnecting from broker");
            client.disconnect();
            Log.d(TAG, "Disconnected.");
        } catch (MqttException me) {
            Log.e(TAG, me.getMessage());
        }
    }
}

