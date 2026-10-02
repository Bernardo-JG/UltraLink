package org.feup.apm.cmeb_login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.ViewCompat;

public class SettingsActivity extends AppCompatActivity {

    private Switch soundToggle, themeToggle;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.bg_color));
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        // Initialize the shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Defining the elements
        soundToggle = findViewById(R.id.sound_toggle);
        themeToggle = findViewById(R.id.theme_toggle);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // For when user presses the back button
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                intent.putExtra("targetFragment", "HomeFragment");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish(); // Finishing the SettingsActivity
            }
        });

        // Load saved settings from SharedPreferences
        loadSettings();

        // Setting listeners
        soundToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Saving the sound setting in SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("sound_enabled", isChecked);
            editor.apply();

            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);


            // Give feedback
            if (isChecked) {
                int previousVolume = sharedPreferences.getInt("previous_volume", audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0);
                Toast.makeText(SettingsActivity.this, "Sound Enabled", Toast.LENGTH_SHORT).show();
            } else {
                SharedPreferences.Editor editorVolume = sharedPreferences.edit();
                int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                editorVolume.putInt("previous_volume", currentVolume); // Save the current volume
                editorVolume.apply();

                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0); // Mute the sound
                Toast.makeText(SettingsActivity.this, "Sound Disabled", Toast.LENGTH_SHORT).show();
            }
        });

        themeToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the theme setting in SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("dark_mode_enabled", isChecked);
            editor.apply();

            // Apply theme based on the toggle
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                Toast.makeText(SettingsActivity.this, "Dark Mode Enabled", Toast.LENGTH_SHORT).show();
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                Toast.makeText(SettingsActivity.this, "Day Mode Enabled", Toast.LENGTH_SHORT).show();
            }
        });


        
    }

    // Load saved settings from SharedPreferences
    private void loadSettings() {
        boolean soundEnabled = sharedPreferences.getBoolean("sound_enabled", true); // Default true
        boolean darkModeEnabled = sharedPreferences.getBoolean("dark_mode_enabled", false); // Default false

        soundToggle.setChecked(soundEnabled);
        themeToggle.setChecked(darkModeEnabled);

        // Set the current theme at the start
        if (darkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
