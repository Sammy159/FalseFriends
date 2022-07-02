package com.example.falsefriends;

import android.os.Handler;
import android.widget.TextView;
import java.util.Locale;

/** Stopwatch class to measure the time the player needs to finish the game
 * Template: Internet https://www.geeksforgeeks.org/how-to-create-a-stopwatch-app-using-android-studio/
 * @see <a href="https://www.geeksforgeeks.org/how-to-create-a-stopwatch-app-using-android-studio/">Template</a>*/
public class Stopwatch {
    private boolean isRunning = false;
    private int seconds = 0;
    private final TextView tv_stopwatch;

    /**
     * gets the textView element where the time will be displayed
     * @param textView current textView object
     */
    public Stopwatch(TextView textView){
        this.tv_stopwatch = textView;
    }

    /**
     * starts the stopwatch
     */
    public void startTimer(){
            isRunning = true;
            runTimer();
    }

    /**
     * stops the stopwatch
     */
    public void stopTimer(){
        isRunning = false;
    }

    /**
     * returns the seconds of the stopwatch
     * @return seconds
     */
    public int getTime(){
        return seconds;
    }

    /** runs code every second to check whether the stopwatch is running, <br>
     * and, if it is, increment the number of seconds and display the number of seconds in the text view. <br>
     * Completely taken from template
      */
    public void runTimer(){
        final Handler handler = new Handler();

        // Call the post() method, passing in a new Runnable.
        // The post() method processes code without a delay,
        // so the code in the Runnable will run almost immediately.
        handler.post(new Runnable() {

            @Override
            public void run()
            {
                int minutes = (seconds % 3600) / 60;
                int secs = seconds % 60;

                // Format the seconds into hours, minutes,
                // and seconds.
                String time = String.format(Locale.getDefault(), "%02d:%02d", minutes, secs);

                // Set the text view text.
                tv_stopwatch.setText(time);

                // If running is true, increment the
                // seconds variable.
                if (isRunning) {
                    seconds++;
                }

                // Post the code again
                // with a delay of 1 second.
                handler.postDelayed(this, 1000);
            }
        });
    }

}
