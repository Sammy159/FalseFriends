package com.example.falsefriends;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

/**
 * class for the options activity which lets the user toggle the sound and change the sensor data input
 */
public class Options extends AppCompatActivity implements View.OnClickListener{
        private TextView tv_ip;
        private TextInputEditText ipinput;
        boolean boolBroker = false;
        Button useIpAddress;
        boolean soundOn = true;

    /**
     * initiates the view objects and sets onclicklisteners to the buttons
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.options);

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switchSound = (Switch) findViewById(R.id.switch1);
        switchSound.setChecked(true);
        switchSound.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if(isChecked){
                soundOn = true;
            }
            else{
                soundOn = false;
            }
        });
        RadioButton SmartphoneButton = (RadioButton)this.findViewById(R.id.rb_smartphone);
        SmartphoneButton.setOnClickListener(this);
        SmartphoneButton.isChecked();
        RadioButton RaspberryButton = (RadioButton) this.findViewById(R.id.rb_raspberry);
        RaspberryButton.setOnClickListener(this);
        useIpAddress = (Button) this.findViewById(R.id.btn_ipAdress);
        useIpAddress.setOnClickListener(this);
        Button backToGameButton = (Button) this.findViewById(R.id.btn_optbacktogame);
        backToGameButton.setOnClickListener(this);
        Button backToStartButton = (Button) this.findViewById(R.id.btn_optbacktostart);
        backToStartButton.setOnClickListener(this);
        tv_ip = (TextView) this.findViewById(R.id.tv_ip);
        ipinput = (TextInputEditText) this.findViewById(R.id.tf_ipinput);

        //only visible if raspberry pi is selected
        tv_ip.setVisibility(View.INVISIBLE);
        ipinput.setVisibility(View.INVISIBLE);
        useIpAddress.setEnabled(false);

    }

    /**
     * logic for the buttons (Template: Book "Android" page 221 and 259), changes sensor input and calls activities
     * @param view current view
     * @see <a href="https://www.hanser-elibrary.com/isbn/9783446445987">Book "Android"</a>
     */
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.rb_smartphone:
                tv_ip.setVisibility(View.INVISIBLE);
                ipinput.setVisibility(View.INVISIBLE);
                useIpAddress.setEnabled(false);
                break;

            case R.id.rb_raspberry:
                tv_ip.setVisibility(View.VISIBLE);
                ipinput.setVisibility(View.VISIBLE);
                useIpAddress.setEnabled(true);
                boolBroker = true;
                break;

            case R.id.btn_ipAdress:
                //save ip address persistently
                // Template Book Android page 259
                SharedPreferences prefBroker = getSharedPreferences("TCP", Context.MODE_PRIVATE);
                SharedPreferences.Editor myEditor = prefBroker.edit();
                myEditor.putString("broker", ipinput.getText().toString());
                myEditor.apply();
                boolBroker = true;
                break;

            case R.id.btn_optbacktogame:
                Intent intent = new Intent(this, GameView.class);
                intent.putExtra("broker", boolBroker);
                intent.putExtra("soundOn", soundOn);
                this.startActivity(intent);
                break;

            case R.id.btn_optbacktostart:
                Intent intent1 = new Intent(this, MainActivity.class);
                intent1.putExtra("broker", boolBroker);
                this.startActivity(intent1);
                break;
        }
    }
}
