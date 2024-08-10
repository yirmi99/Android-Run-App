package com.example.justrun;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION = 99;
    private static final String TAG = "MainActivity";
    private static final String WEATHER_API_KEY = "c6ed1f5007a977231590fbd5826c0ca5";
    private Button startButton;
    private Button showHistoryButton;
    private Button settingsButton;
    private TextView locationTextView;
    private TextView weatherTextView;
    private TextView weatherDetailsTextView;
    private TextView lowBatteryTextView;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;
    private BroadcastReceiver batteryLevelReceiver;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Context context = LocaleHelper.onAttach(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.startRun);
        showHistoryButton = findViewById(R.id.showHistoryButton);
        settingsButton = findViewById(R.id.settingsButton);
        locationTextView = findViewById(R.id.locationTextView);
        weatherTextView = findViewById(R.id.weatherTextView);
        weatherDetailsTextView = findViewById(R.id.weatherDetailsTextView);
        lowBatteryTextView = findViewById(R.id.lowBatteryTextView);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.d(TAG, "Location result is null");
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        displayLocation(location.getLatitude(), location.getLongitude());
                    }
                }
            }
        };

        getLocationPermission();

        startButton.setOnClickListener(view -> {
            if (checkLocationPermission()) {
                checkBatteryLevel();
            } else {
                requestLocationPermission();
            }
        });

        showHistoryButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        settingsButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        prefListener = (sharedPrefs, key) -> {
            if (key.equals("selected_language")) {
                recreate();
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener);

        // Register the battery level receiver
        batteryLevelReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                float batteryPct = (level / (float) scale) * 100;
                updateBatteryStatus(batteryPct);
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(batteryLevelReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(batteryLevelReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(prefListener);
    }

    private void getLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopTrackingService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTrackingService();
    }

    private void stopTrackingService() {
        stopService(new Intent(this, TrackingService.class));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void displayLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                String streetName = address.getThoroughfare();
                String stateName = address.getAdminArea();
                String countryName = address.getCountryName();
                String location = "Location: " + streetName + ", " + stateName + ", " + countryName;
                locationTextView.setText(location);
                getWeather(latitude, longitude);
            } else {
                locationTextView.setText("Latitude: " + latitude + "\nLongitude: " + longitude);
            }
        } catch (IOException e) {
            e.printStackTrace();
            locationTextView.setText("Latitude: " + latitude + "\nLongitude: " + longitude);
        }
    }

    private void getWeather(double latitude, double longitude) {
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&units=metric&appid=" + WEATHER_API_KEY;
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject main = response.getJSONObject("main");
                            double temp = main.getDouble("temp");
                            int humidity = main.getInt("humidity");
                            JSONObject wind = response.getJSONObject("wind");
                            double windSpeed = wind.getDouble("speed");
                            String weatherDescription = response.getJSONArray("weather").getJSONObject(0).getString("description");

                            weatherTextView.setText("Weather: " + temp + "°C");
                            weatherDetailsTextView.setText("Conditions: " + weatherDescription + "\nHumidity: " + humidity + "%\nWind Speed: " + windSpeed + " m/s");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching weather data: " + error.getMessage());
                    }
                });
        queue.add(jsonObjectRequest);
    }

    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
    }

    private void checkBatteryLevel() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);
        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : 0;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : 100;
        float batteryPct = (level / (float) scale) * 100;

        if (batteryPct < 5.0) {
            startButton.setVisibility(View.GONE);
            lowBatteryTextView.setVisibility(View.VISIBLE);
        } else {
            startButton.setVisibility(View.VISIBLE);
            lowBatteryTextView.setVisibility(View.GONE);
            startRunActivity();
        }
    }

    private void updateBatteryStatus(float batteryPct) {
        if (batteryPct < 5.0) {
            startButton.setVisibility(View.GONE);
            lowBatteryTextView.setVisibility(View.VISIBLE);
        } else {
            startButton.setVisibility(View.VISIBLE);
            lowBatteryTextView.setVisibility(View.GONE);
        }
    }

    private void startRunActivity() {
        if (checkLocationPermission()) {
            Intent intent = new Intent(MainActivity.this, GoogleMapActivity.class);
            startActivity(intent);
        } else {
            requestLocationPermission();
        }
    }
}
