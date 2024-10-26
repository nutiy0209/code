package com.example.code.welcome;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.code.R;
import com.example.code.exercise.PoseMaster;
import com.example.code.ui.ChatOld;
import com.example.code.ui.Register;
import com.example.code.ui.ScaleChat;

public class LobbyPatient extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lobby_patient);

        Button buttonScale = findViewById(R.id.button_scale);
        Button buttonNostalgic = findViewById(R.id.button_nostalgic);
        Button buttonExercise = findViewById(R.id.button_exercise);
        ImageButton personPhoto = findViewById(R.id.person_photo);

        // 導向量表頁面
        buttonScale.setOnClickListener(v -> {
            Intent intent = new Intent(LobbyPatient.this, ScaleChat.class);
            startActivity(intent);
        });

        // 導向懷舊頁面
        buttonNostalgic.setOnClickListener(v -> {
            Intent intent = new Intent(LobbyPatient.this, ChatOld.class);
            startActivity(intent);
        });

        // 導向運動頁面
        buttonExercise.setOnClickListener(v -> {
            Intent intent = new Intent(LobbyPatient.this, PoseMaster.class);
            startActivity(intent);
        });

        // 導向設定個人信息
        personPhoto.setOnClickListener(v -> {

            Intent intent = new Intent(LobbyPatient.this, Register.class);
            startActivity(intent);
        });

    }
}
