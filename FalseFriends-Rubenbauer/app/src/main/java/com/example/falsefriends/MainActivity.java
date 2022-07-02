package com.example.falsefriends;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

/** Launching screen with the name input logic and the options to go to the options or leaderboard activity */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    SharedPreferences sharedpreferences;
    private EditText ti_name;

    //TODO: -bootclasspath /.../android.jar   für JavaDoc

    /** Initiates view elements and the sharedPreferences "Leaderboard" */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initiate the view elements
        Button startGameButton = (Button) this.findViewById(R.id.btn_start);
        startGameButton.setOnClickListener(this);
        ImageButton optionsButton = (ImageButton) this.findViewById(R.id.btn_startOptions);
        optionsButton.setOnClickListener(this);
        ImageButton leaderboardButton = (ImageButton) this.findViewById(R.id.btn_startLeaderboard);
        leaderboardButton.setOnClickListener(this);
        ti_name = (EditText) this.findViewById(R.id.ti_name);

        //Credits: https://developer.android.com/reference/android/content/SharedPreferences#developer-guides
        sharedpreferences = getSharedPreferences("Leaderboard", Context.MODE_PRIVATE);
        //Deletes SharedPrefs for debugging purposes
        //sharedpreferences.edit().clear().apply();

    }

    /** Button logic to go to other activity or start the game
     * based on book "Android" page 254
     * @param v current view
     * @see <a href="https://www.hanser-elibrary.com/isbn/9783446445987">Buch "Andriod"</a> */
    @SuppressLint("NonConstantResourceId")
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_start:
                if(TextUtils.isEmpty(ti_name.getText().toString())){
                    Context context = getApplicationContext();
                    CharSequence text = "Please enter your Name above!";
                    int duration = Toast.LENGTH_SHORT;
                    Toast.makeText(context, text, duration).show();
                    break;
                }
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString("name", ti_name.getText().toString());
                editor.apply();

                Intent intent = new Intent(this, GameView.class);
                this.startActivity(intent);
                break;
            case R.id.btn_startOptions:
                Intent intent1 = new Intent(this, Options.class);
                this.startActivity(intent1);
                break;
            case R.id.btn_startLeaderboard:
                Intent intent2 = new Intent(this, Leaderboard.class);
                intent2.putExtra("finished", false);
                this.startActivity(intent2);
                break;

        }

    }

}
//TODO: in docu diese Quelle mit aufnehmen
//https://magpi.raspberrypi.com/articles/make-a-sense-hat-rainbow-display-for-your-window
