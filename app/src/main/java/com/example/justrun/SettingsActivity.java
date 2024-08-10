package com.example.justrun;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatDelegate;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

public class SettingsActivity extends BaseActivity {
    private Button btnToggleDark;
    private RadioButton englishTranslate;
    private RadioButton hebrewTranslate;
    private RadioGroup languageRadioGroup;

    Resources resources;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = LocaleHelper.onAttach(this);

        setContentView(R.layout.activity_settings);
        btnToggleDark = findViewById(R.id.btnToggleDark);

        // Initialize shared preferences and check dark mode state
        SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        final boolean isDarkModeOn = sharedPreferences.getBoolean("isDarkModeOn", false);

        if (isDarkModeOn) {
            btnToggleDark.setText(R.string.disable);
        } else {
            btnToggleDark.setText(R.string.enable);
        }

        btnToggleDark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isDarkModeOn) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    editor.putBoolean("isDarkModeOn", false);
                    btnToggleDark.setText(R.string.enable);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    editor.putBoolean("isDarkModeOn", true);
                    btnToggleDark.setText(R.string.disable);
                }
                editor.apply();
            }
        });

        englishTranslate = findViewById(R.id.radioEnglish);
        hebrewTranslate = findViewById(R.id.radioHebrew);
        languageRadioGroup = findViewById(R.id.languageRadioGroup);

        englishTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateLocale("en");
            }
        });

        hebrewTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateLocale("iw");
            }
        });

        // Set the correct language radio button based on the current locale
        String currentLang = LocaleHelper.getLanguage(this);
        if ("iw".equals(currentLang)) {
            hebrewTranslate.setChecked(true);
        } else {
            englishTranslate.setChecked(true);
        }
    }

    private void updateLocale(String language) {
        SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("selected_language", language);
        editor.apply();

        context = LocaleHelper.setLocale(SettingsActivity.this, language);
        resources = context.getResources();
        recreate(); // Refresh activity to apply language change
    }
}
