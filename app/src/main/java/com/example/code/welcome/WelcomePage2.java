package com.example.code.welcome;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.code.R;

public class WelcomePage2 extends AppCompatActivity {

    private TextView text1;
    private Button patientButton;
    private Button caregiverButton;
    private String welcomeMessage = "我是";
    private int index = 0; // 文字索引位置
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_page2);

        text1 = findViewById(R.id.text1); // 初始化 TextView
        patientButton = findViewById(R.id.patient_button);
        caregiverButton = findViewById(R.id.caregiver_button);

        // 開始逐字顯示文字
        showTextOneByOne();

        // 設定按鈕點擊事件
        patientButton.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomePage2.this, LobbyPatient.class);
            startActivity(intent);
        });

        caregiverButton.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomePage2.this, LobbyCaregiver.class);
            startActivity(intent);
        });
    }

    private void showTextOneByOne() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (index < welcomeMessage.length()) {
                    // 將字母逐字加入到 TextView 中
                    text1.setText(text1.getText().toString() + welcomeMessage.charAt(index));
                    index++;
                    handler.postDelayed(this, 200); // 每個字延遲200毫秒
                } else {
                    // 當文字顯示完成後顯示按鈕
                    fadeInButton(patientButton);
                    fadeInButton(caregiverButton);
                }
            }
        }, 200);
    }
    private void fadeInButton(Button button) {
        button.setVisibility(View.VISIBLE);
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f); // 從透明到完全顯示
        fadeIn.setDuration(1000); // 持續時間為 1 秒
        button.startAnimation(fadeIn);
    }
}
