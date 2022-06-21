package com.example.sensorplaytest3;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

/**
 * This class is the main activity**/
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public int levelCount = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button activity2Button = (Button)
                this.findViewById(R.id.btn_start);
        activity2Button.setOnClickListener(this);

    }

    /** OnClick-Event, which starts the actual Game Credits: Andriod Buch S.254*/
    public void onClick(View v) {
        Intent intent = new Intent(this, GameView.class);
        intent.putExtra("levelCount", levelCount);
        this.startActivity(intent);
    }

    //TODO bei reset der App das Level zurücksetzen: nextLevel.resetLevel()


}
