package com.example.justrun;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class RunActivity extends BaseActivity {

    protected TextView textView;
    protected long elapsedTimeMillis = 0;
    protected CountDownTimer countUpTimer;
    protected Button stopButton;
    protected Button saveButton;
    DatabaseManager dbManager;
    long hours = 0;
    long minutes = 0;
    long seconds = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = LocaleHelper.onAttach(this);

        setContentView(R.layout.activity_running);
        // Initialize the textView after the layout is inflated
        textView = findViewById(R.id.timer);
        stopButton = findViewById(R.id.stopButton);
        saveButton = findViewById(R.id.stopSaveButton);
        dbManager = new DatabaseManager(this);


        // Set click listener for the stopButton
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTimer();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint({"SimpleDateFormat", "DefaultLocale"})
            @Override
            public void onClick(View v) {
                // Finding the current date
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
                String dateTime = simpleDateFormat.format(calendar.getTime());
                // Finding the duration
                String duration = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                // Finding the kilometers

                try{
                    dbManager.open();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                dbManager.insert(dateTime,duration,1.5);
                dbManager.close();
                // Stop the timer
                stopTimer();
            }
        });

        // Start the timer to count up from 00:00
        startTimer();
    }

    // Method to start the timer
    private void startTimer() {
        countUpTimer = new CountUpTimer();
        countUpTimer.start();
    }

    // Method to stop the timer and move back to MainActivity
    private void stopTimer() {
        if (countUpTimer != null) {
            countUpTimer.cancel();
            // Start MainActivity and pass any necessary data
            Intent intent = new Intent(RunActivity.this, MainActivity.class);
            intent.putExtra("elapsedTimeMillis", elapsedTimeMillis); // Pass elapsed time if needed
            startActivity(intent);
            finish(); // Finish RunningActivity to prevent going back to it with back button
        }
    }

    // Custom CountUpTimer class to count up from 00:00
    private class CountUpTimer extends CountDownTimer {
        CountUpTimer() {
            super(Long.MAX_VALUE, 1000); // Count up indefinitely, update every second
        }

        @Override
        public void onTick(long millisUntilFinished) {
            elapsedTimeMillis += 1000; // Increment elapsed time by 1 second
            updateTimerText(elapsedTimeMillis);
        }

        @Override
        public void onFinish() {
            // Not needed for CountUpTimer
        }
    }

    // Method to update the timer text view with the elapsed time
    private void updateTimerText(long elapsedTimeMillis) {
        // Convert elapsed time to hours, minutes, and seconds
        hours = elapsedTimeMillis / (1000 * 60 * 60);
        minutes = (elapsedTimeMillis % (1000 * 60 * 60)) / (1000 * 60);
        seconds = ((elapsedTimeMillis % (1000 * 60 * 60)) % (1000 * 60)) / 1000;

        // Format the time with leading zeros if necessary
        String formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        // Update the TextView with the formatted time
        textView.setText(formattedTime);
    }


}
