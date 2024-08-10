package com.example.justrun;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class TrackingService extends Service {
    public static final String CHANNEL_ID = "TrackingServiceChannel";
    public static final String EXTRA_DISTANCE = "extra_distance";
    public static final String EXTRA_TIME = "extra_time";
    private Handler handler;
    private Runnable runnable;
    private String distance;
    private long elapsedTime;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        distance = intent.getStringExtra(EXTRA_DISTANCE);
        elapsedTime = intent.getLongExtra(EXTRA_TIME, 0);
        startForeground(1, createNotification(distance, formatElapsedTime(elapsedTime)));
        startUpdatingNotification();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }

    private void startUpdatingNotification() {
        runnable = new Runnable() {
            @Override
            public void run() {
                Notification notification = createNotification(distance, formatElapsedTime(elapsedTime));
                startForeground(1, notification);
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);
    }

    private Notification createNotification(String distance, String time) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Running Tracker")
                .setContentText("Distance: " + distance + " | Time: " + time)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true)
                .build();
    }

    private String formatElapsedTime(long elapsedTime) {
        long seconds = (elapsedTime / 1000) % 60;
        long minutes = (elapsedTime / (1000 * 60)) % 60;
        long hours = (elapsedTime / (1000 * 60 * 60)) % 24;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Tracking Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}