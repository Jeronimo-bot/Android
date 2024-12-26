package com.lt.colorblock;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {

    private Switch musicSwitch, soundEffectsSwitch;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize switches
        musicSwitch = findViewById(R.id.musicSwitch);
        soundEffectsSwitch = findViewById(R.id.soundEffectsSwitch);

        // Initialize SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Set initial states based on preferences
        boolean isMusicEnabled = sharedPreferences.getBoolean("music_enabled", true);
        boolean areSoundEffectsEnabled = sharedPreferences.getBoolean("sound_effects_enabled", true);

        musicSwitch.setChecked(isMusicEnabled);
        soundEffectsSwitch.setChecked(areSoundEffectsEnabled);

        // Set listeners for switches
        musicSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sharedPreferences.edit().putBoolean("music_enabled", isChecked).apply();
                if (isChecked) {
                    Toast.makeText(SettingsActivity.this, "Background music enabled.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SettingsActivity.this, "Background music disabled.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        soundEffectsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sharedPreferences.edit().putBoolean("sound_effects_enabled", isChecked).apply();
                if (isChecked) {
                    Toast.makeText(SettingsActivity.this, "Sound effects enabled.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SettingsActivity.this, "Sound effects disabled.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
