package com.example.justrun;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import android.content.IntentSender;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class GoogleMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_CHECK_SETTINGS = 100;
    private static final int FINE_PERMISSION_CODE = 1;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap map;
    private double userLat, userLong;
    private LatLng userLocation, previousLocation;
    private Polyline polyline;
    private ProgressDialog dialog;
    private LocationCallback locationCallback;
    private float totalDistance = 0f;
    private TextView distanceTextView;
    protected TextView textView;
    protected long elapsedTimeMillis = 0;
    protected CountDownTimer countUpTimer;
    protected Button stopButton;
    protected Button saveButton;
    DatabaseManager dbManager;
    long hours = 0;
    long minutes = 0;
    long seconds = 0;

    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPercent = (level / (float) scale) * 100;

            // Check if battery level is less than 5%
            if (batteryPercent < 5) {
                // Save run data and close the map
                saveRunDataAndCloseMap();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_map);
        distanceTextView = findViewById(R.id.distanceTextView);
        Context context = LocaleHelper.onAttach(this);

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

                try {
                    dbManager.open();
                    dbManager.insert(dateTime, duration, totalDistance);
                    dbManager.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Stop the timer
                stopTimer();
            }
        });

        // Start the timer to count up from 00:00
        startTimer();

        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView);
        if (fragment != null) {
            fragment.getMapAsync(this);
        }

        dialog = new ProgressDialog(GoogleMapActivity.this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize LocationCallback to get location updates
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    userLat = location.getLatitude();
                    userLong = location.getLongitude();
                    userLocation = new LatLng(userLat, userLong);

                    if (previousLocation != null) {
                        float[] results = new float[1];
                        Location.distanceBetween(previousLocation.latitude, previousLocation.longitude, userLocation.latitude, userLocation.longitude, results);
                        totalDistance += results[0] / 1000; // convert meters to kilometers
                        // Update distance TextView
                        distanceTextView.setText("Distance: " + totalDistance + " km");
                    }

                    previousLocation = userLocation;
                    map.animateCamera(CameraUpdateFactory.newLatLng(userLocation));
                    // Draw the route
                    drawRoute();
                }
            }
        };

        // Check location settings
        checkLocationSettings();

        // Register battery receiver
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);


    }

    private void updateNotification() {
        Intent serviceIntent = new Intent(this, TrackingService.class);
        serviceIntent.putExtra(TrackingService.EXTRA_DISTANCE, String.format("%.2f km", totalDistance));
        serviceIntent.putExtra(TrackingService.EXTRA_TIME, elapsedTimeMillis);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister battery receiver
        unregisterReceiver(batteryReceiver);
    }

    // Method to start the timer
    private void startTimer() {
        elapsedTimeMillis = 0; // Reset the elapsed time
        countUpTimer = new CountUpTimer();
        countUpTimer.start();
    }

    // Method to stop the timer and move back to MainActivity
    private void stopTimer() {
        if (countUpTimer != null) {
            countUpTimer.cancel();
            // Stop the foreground service
            stopService(new Intent(this, TrackingService.class));
            // Start MainActivity and pass any necessary data
            Intent intent = new Intent(GoogleMapActivity.this, MainActivity.class);
            intent.putExtra("elapsedTimeMillis", elapsedTimeMillis); // Pass elapsed time if needed
            startActivity(intent);
            finish(); // Finish RunningActivity to prevent going back to it with back button
        }
    }

    private class CountUpTimer extends CountDownTimer {
        CountUpTimer() {
            super(Long.MAX_VALUE, 1000); // Count up indefinitely, update every second
        }

        @Override
        public void onTick(long millisUntilFinished) {
            elapsedTimeMillis += 1000; // Increment elapsed time by 1 second
            updateTimerText(elapsedTimeMillis);
            updateNotification(); // Update the notification every second
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

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
        } else {
            map.setMyLocationEnabled(true);
            map.getUiSettings().setZoomControlsEnabled(true);
            fetchMyLocation();
        }
    }

    private String getCurrentDateTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return simpleDateFormat.format(new Date());
    }

    private void saveRunDataAndCloseMap() {
        // Save run data as per your existing logic
        // For example:
        String dateTime = getCurrentDateTime();
        String duration = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        try {
            dbManager.open();
            dbManager.insert(dateTime, duration, totalDistance);
            dbManager.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Close the map activity
        finish();
    }


    private void checkLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);  // 10 seconds
        locationRequest.setFastestInterval(5000);  // 5 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    // All location settings are satisfied. Start location updates
                    fetchMyLocation();
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                resolvable.startResolutionForResult(GoogleMapActivity.this, REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            Toast.makeText(GoogleMapActivity.this, "Location settings are inadequate, and cannot be fixed here. Fix in Settings.", Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                if (resultCode == Activity.RESULT_OK) {
                    fetchMyLocation();
                } else {
                    Toast.makeText(this, "Location services are required to use this app.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private void fetchMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }

        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null){
                    userLat = location.getLatitude();
                    userLong = location.getLongitude();
                    userLocation = new LatLng(userLat, userLong);
                    LatLng latLng = new LatLng(userLat, userLong);
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(latLng).zoom(12).build();
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                    map.addMarker(new MarkerOptions().position(latLng)
                            .icon(setIcon(GoogleMapActivity.this, R.drawable.ic_launcher_foreground)));
                    // Start location updates
                    startLocationUpdates();
                } else {
                    Log.d("Location", "Location is null");
                }
            }
        });
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);  // 10 seconds
        locationRequest.setFastestInterval(5000);  // 5 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void drawRoute() {
        if (polyline == null) {
            polyline = map.addPolyline(new PolylineOptions().color(Color.RED).width(12).startCap(new RoundCap()).endCap(new RoundCap()));
        }
        List<LatLng> points = polyline.getPoints();
        points.add(userLocation);
        polyline.setPoints(points);
    }

    public BitmapDescriptor setIcon(Activity context, int drawableID) {
        Drawable drawable = ActivityCompat.getDrawable(context, drawableID);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == FINE_PERMISSION_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                fetchMyLocation();
            }else{
                Toast.makeText(this,"Location permission is denied, Please allow the permission", Toast.LENGTH_LONG).show();
            }
        }
    }
}