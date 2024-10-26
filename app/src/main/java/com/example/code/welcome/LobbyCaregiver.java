package com.example.code.welcome;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.code.R;
import com.example.code.ui.HealthEducation;


public class LobbyCaregiver extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lobby_caregiver);

        Button buttonCareChat = findViewById(R.id.button_care);
        Button buttonLoseSetting = findViewById(R.id.button_loseSetting);

        buttonCareChat.setOnClickListener(v -> {
            // 導向照護者聊天機器人
            Intent intent = new Intent(LobbyCaregiver.this, HealthEducation.class);
            startActivity(intent);
        });

//        buttonLoseSetting.setOnClickListener(v -> {
//             導向防走失設定
//            Intent intent = new Intent(LobbyCaregiver.this, ChatOld.class);
//            startActivity(intent);
//        });


    }
}
