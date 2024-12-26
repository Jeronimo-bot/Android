package com.lt.colorblock;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {

    private Button startGameButton, settingsButton, zenModeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // Initialize buttons
        startGameButton = findViewById(R.id.startGameButton);
        zenModeButton = findViewById(R.id.zenModeButton);
        settingsButton = findViewById(R.id.settingsButton);
        // Removido: aboutButton = findViewById(R.id.aboutButton);

        // Set listener for "Start Game" button
        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MenuActivity.this, MainActivity.class);
                intent.putExtra("MODE", "NORMAL");
                startActivity(intent);
            }
        });

        // Set listener for "Zen Mode" button
        zenModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MenuActivity.this, MainActivity.class);
                intent.putExtra("MODE", "ZEN");
                startActivity(intent);
            }
        });

        // Set listener for "Settings" button
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MenuActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        /* Removido o Listener para o botão "About"
        // Set listener for "About" button
        aboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MenuActivity.this, "Color Block v1.0", Toast.LENGTH_SHORT).show();
            }
        });
        */
    }
}
