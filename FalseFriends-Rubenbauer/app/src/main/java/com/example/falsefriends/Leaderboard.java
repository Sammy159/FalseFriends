package com.example.falsefriends;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/** Displays the leaderboard by getting data from sharedPreferences and adjusts the data if necessary */
public class Leaderboard extends AppCompatActivity implements View.OnClickListener {
    int newTimeSec;
    SharedPreferences prefs;
    TextView tv_first;
    TextView tv_second;
    TextView tv_third;
    TextView tv_yourTime;

    /** Checks if the current user needs to be on the leaderboard and then calls the display function*/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.leaderboard_layout);

        //Get links to layout
        Button backButton = (Button) this.findViewById(R.id.btn_back);
        backButton.setOnClickListener(this);
        Button backToStartButton = (Button) this.findViewById(R.id.btn_backToStart);
        backToStartButton.setOnClickListener(this);
        tv_first = (TextView) this.findViewById(R.id.tv_first);
        tv_second = (TextView) this.findViewById(R.id.tv_second);
        tv_third = (TextView) this.findViewById(R.id.tv_third);
        tv_yourTime = (TextView) this.findViewById(R.id.tv_yourTime);

        //Get the SharedPrefs and initiate an editor
        prefs = getSharedPreferences("Leaderboard",Context.MODE_PRIVATE);

       //check if the game is finished
        Intent intent = getIntent();
        Bundle daten = intent.getExtras();
        boolean finished = daten.getBoolean("finished");
        if(finished){
             newTimeSec = daten.getInt("time");
            //get name from the beginning from sharedPrefs
            String name = prefs.getString("name", "withoutName");

            //TODO: Zuerst werden die Plätze aufgefüllt und erst danach wird nach schnelleren Zeiten gesucht
            //check if a place doesnet exist
            int notExitingPlace = existsPlace();
            //if a place doesn't exist, fill with current players data
            if(notExitingPlace != 0) {
                boolean inserted = compareTimes(name, notExitingPlace);
                if(!inserted) fillPlaces(notExitingPlace, name);
            }
            //if all places already have data, compare the time
            else{
                compareTimes(name, 3);
            }
        }
        displayLeaderboard(daten);


    }

    /**
     * checks if a place on the leaderboard has not been filled with data
     * @return the non exiting place or 0 if all places exist
     */
    private int existsPlace(){
        String[] places = {"firstplaceBool", "secondplaceBool", "thirdplaceBool"};
        for(int i=0; i<3; i++){
            boolean temp =  prefs.getBoolean(places[i], false);
            if(!temp) return i+1;
        }
        return 0;
    }

    /**
     * fill non exiting places with the current players data
     * @param notExitingPlace int value of the place that doesn't exit
     * @param name  players name
     */
    private void fillPlaces(int notExitingPlace, String name){
        SharedPreferences.Editor myEditor = prefs.edit();
        if(notExitingPlace == 1){
            //if not: put current user infos in first place
            myEditor.putString("firstplaceName", name);
            myEditor.putInt("firstplaceTime", newTimeSec);
            myEditor.putBoolean("firstplaceBool", true);
            myEditor.apply();
        }
        else if(notExitingPlace == 2){
            myEditor.putString("secondplaceName", name);
            myEditor.putInt("secondplaceTime", newTimeSec);
            myEditor.putBoolean("secondplaceBool", true);
            myEditor.apply();
        }
        else if(notExitingPlace == 3){
            myEditor.putString("thirdplaceName", name);
            myEditor.putInt("thirdplaceTime", newTimeSec);
            myEditor.putBoolean("thirdplaceBool", true);
            myEditor.apply();
        }
    }

    /**
     * Compares the current players time with the time of the exiting leaderboard data, if its quicker, then replace the place with the new data
     * @param name current players name
     */
    private boolean compareTimes(String name, int existingPlaces){
        String[] times = {"firstplaceTime", "secondplaceTime", "thirdplaceTime"};
        String[] names = {"firstplaceName", "secondplaceName", "thirdplaceName"};
        SharedPreferences.Editor myEditor = prefs.edit();
        for(int i=0; i<existingPlaces; i++){
            int oldTime = prefs.getInt(times[i], 500);
            //if new time is lower, put new place data
            if(newTimeSec < oldTime) {
                myEditor.putString(names[i], name);
                myEditor.putInt(times[i], newTimeSec);
                myEditor.apply();
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the data from the sharedPreferences and displays it on the screen
     * @param daten Extras passed on from the intent, containing the time of the current player
     * */
    private void displayLeaderboard(Bundle daten){
        String firstPlaceTime = "0";
        if(prefs.contains("firstplaceTime")) firstPlaceTime = Integer.toString(prefs.getInt("firstplaceTime", 0));
        String firstPlaceName = prefs.getString("firstplaceName", "---");
        tv_first.setText(firstPlaceName + ": "+ firstPlaceTime);

        String secondPlaceTime = "0";
        if(prefs.contains("secondplaceTime")) secondPlaceTime = Integer.toString(prefs.getInt("secondplaceTime", 0));
        String secondPlaceName = prefs.getString("secondplaceName", "---");
        tv_second.setText(secondPlaceName + ": "+ secondPlaceTime);

        String thirdPlaceTime = "0";
        if(prefs.contains("thirdplaceTime")) thirdPlaceTime = Integer.toString(prefs.getInt("thirdplaceTime", 0));
        String thirdPlaceName = prefs.getString("thirdplaceName", "---");
        tv_third.setText(thirdPlaceName + ": "+ thirdPlaceTime);

        String ownPlaceTime = Integer.toString(daten.getInt("time"));
        String ownPlaceName = prefs.getString("name", "noName");
        tv_yourTime.setText(ownPlaceName + ": "+ ownPlaceTime);


    }

    /** To get back to the game or to the starting screen
     * @param view current view*/
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view){

        switch (view.getId()){
            case R.id.btn_back:
                Intent intent = new Intent(this, GameView.class);
                this.startActivity(intent);
                break;
            case R.id.btn_backToStart:
                Intent intent1 = new Intent(this, MainActivity.class);
                this.startActivity(intent1);
                break;

        }
    }

}
