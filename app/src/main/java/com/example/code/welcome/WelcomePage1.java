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

public class WelcomePage1 extends AppCompatActivity {

    private TextView welcomeText;
    private Button nextButton;
    private String welcomeMessage = "歡迎來到失智照護系統";
    private int index = 0; // 文字索引位置
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_page1);

        welcomeText = findViewById(R.id.welcome_text);
        nextButton = findViewById(R.id.next_button);

        // 開始逐字顯示文字
        showTextOneByOne();

        // 設定按鈕點擊事件
        nextButton.setOnClickListener(v -> {
            // 在此加入跳轉到下一頁的邏輯
            Intent intent = new Intent(WelcomePage1.this, WelcomePage2.class);
            startActivity(intent);
        });
    }

    private void showTextOneByOne() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (index < welcomeMessage.length()) {
                    // 將字母逐字加入到 TextView 中
                    welcomeText.setText(welcomeText.getText().toString() + welcomeMessage.charAt(index));
                    index++;
                    handler.postDelayed(this, 200); // 每個字延遲200毫秒
                } else {
                    // 當文字顯示完成後顯示按鈕
                    fadeInButton(nextButton);
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
