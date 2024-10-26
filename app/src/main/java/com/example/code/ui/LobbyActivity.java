package com.example.code.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.code.R;

public class LobbyActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        Button buttonScale = findViewById(R.id.button_scale);
        Button buttonNostalgic = findViewById(R.id.button_nostalgic);

        buttonScale.setOnClickListener(v -> {
            // 導向量表頁面
            Intent intent = new Intent(LobbyActivity.this, ScaleActivity.class);
            startActivity(intent);
        });

        buttonNostalgic.setOnClickListener(v -> {
            // 導向懷舊頁面
            Intent intent = new Intent(LobbyActivity.this, NostalgicActivity.class);
            startActivity(intent);
        });
    }
}
