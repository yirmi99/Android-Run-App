package com.example.justrun;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    // Sleep for 3 seconds to show the splash screen.
                    sleep(3000);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // Start MainActivity after the splash screen.
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                }
            }
        };
        // Start the thread.
        thread.start();
    }
}
