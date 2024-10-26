package com.example.code.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.code.R;
import com.example.code.welcome.LobbyPatient;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Register extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        final EditText editTextName = findViewById(R.id.editTextName);
        final EditText editTextGender = findViewById(R.id.editTextGender);  // 改为直接输入性别
        final EditText editTextAge = findViewById(R.id.editTextAge);
        final EditText editTextMedicalHistory = findViewById(R.id.editTextMedicalHistory);
        Button buttonSubmit = findViewById(R.id.buttonSubmit);

        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String name = editTextName.getText().toString();
                final String gender = editTextGender.getText().toString();  // 直接获取输入的性别
                final String age = editTextAge.getText().toString();
                final String medicalHistory = editTextMedicalHistory.getText().toString();

                // 使用 OkHttp 发起网络请求
                postData(name, gender, age, medicalHistory);
            }
        });
    }

    private void postData(String name, String gender, String age, String medicalHistory) {
        // 创建 OkHttpClient 实例
        OkHttpClient client = new OkHttpClient();

        // 构建 POST 请求的表单数据
        RequestBody requestBody = new FormBody.Builder()
                .add("Name", name)
                .add("Sex", gender)  // 不进行转换，直接使用输入的性别
                .add("Age", age)
                .add("Sick", medicalHistory)
                .build();

        Request request = new Request.Builder()
                .url("http://192.168.53.103/SaveData.php")  // 确保此URL可访问
                .post(requestBody)
                .build();

        // 发起异步请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                // 请求失败时运行
                runOnUiThread(() -> Toast.makeText(Register.this, "请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                // 请求成功时运行
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("HTTP_RESPONSE", "Response Body: " + responseBody); // 打印服务器返回的消息
                    runOnUiThread(() -> {
                        Toast.makeText(Register.this, "注册完成", Toast.LENGTH_LONG).show();
                        // 启动 MainActivity 并关闭当前 Activity
                        Intent intent = new Intent(Register.this, LobbyPatient.class);
                        intent.putExtra("userName", name);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    String responseBody = response.body().string();
                    int statusCode = response.code();
                    Log.d("HTTP_RESPONSE", "Status Code: " + statusCode);
                    Log.d("HTTP_RESPONSE", "Response Body: " + responseBody);
                    runOnUiThread(() -> Toast.makeText(Register.this, "注册失败", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
