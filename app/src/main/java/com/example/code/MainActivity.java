package com.example.code;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import android.content.IntentFilter;
import android.content.ActivityNotFoundException;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.AdapterView;



public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_VOICE_INPUT = 1001;
    private static final int REQUEST_CODE = 1002;
    private static final String CHANNEL_ID = "ReminderChannel";
    private RecyclerView recyclerView;
    private TextToSpeech textToSpeech;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    private BroadcastReceiver reminderReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            addMessageToChat(message, false); // 将提醒信息添加到聊天界面
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Spinner spinnerMode = findViewById(R.id.SpinnerModes);

        // 创建适配器
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_modes1, android.R.layout.simple_spinner_item);

        // 指定下拉列表的样式
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // 应用适配器到Spinner
        spinnerMode.setAdapter(adapter);

        // 设置选择事件监听
        spinnerMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                String selectedMode = (String) parent.getItemAtPosition(position);
                // 处理选择事件，例如更新UI或保存选择
                if ("懷舊模式".equals(selectedMode)) {
                    // 启动 MainActivity4
                    Intent intent = new Intent(MainActivity.this, MainActivity6.class);
                    startActivity(intent);

                }
                if ("量表模式".equals(selectedMode)) {
                    // 启动 MainActivity4
                    Intent intent = new Intent(MainActivity.this, MainActivity5.class);
                    startActivity(intent);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 如果需要，处理没有选择的情况
            }
        });

        // 初始化 RecyclerView 和消息列表
        recyclerView = findViewById(R.id.recyclerView1);
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);
        SharedPreferences preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("api_key", "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");
        editor.apply();

        // 按钮设置
        Button button = findViewById(R.id.camera1);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MainActivity2.class);
            startActivity(intent);
        });

        ImageButton person = findViewById(R.id.Person1);
        person.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MainActivity3.class);
            startActivity(intent);
        });

        ImageButton setting = findViewById(R.id.Setting1);
        setting.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MainActivity4.class);
            startActivityForResult(intent, REQUEST_CODE);
        });

        TextView textViewName = findViewById(R.id.textView2);
        Intent intent = getIntent();
        String userName = intent.getStringExtra("userName");
        if (userName != null && !userName.isEmpty()) {
            textViewName.setText(userName);
        }

        // 初始化 TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.TAIWAN);
                String welcomeText = "你好，今天想聊些什麼";
                speak(welcomeText);
                addMessageToChat(welcomeText, false);
            } else {
                Toast.makeText(this, "Text-to-Speech 初始化失败", Toast.LENGTH_SHORT).show();
            }
        });


        // 语音输入按钮的点击事件
        Button buttonVoiceInput = findViewById(R.id.TALK1);
        buttonVoiceInput.setOnClickListener(v -> startVoiceInput());

        // 注册广播接收器
        LocalBroadcastManager.getInstance(this).registerReceiver(reminderReceiver,
                new IntentFilter("com.example.code.REMINDER"));
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        // 注销广播接收器
        LocalBroadcastManager.getInstance(this).unregisterReceiver(reminderReceiver);
        super.onDestroy();
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        try {
            startActivityForResult(intent, REQUEST_VOICE_INPUT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Voice input not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String breakfastTime = data.getStringExtra("breakfastTime");
            String lunchTime = data.getStringExtra("lunchTime");
            String dinnerTime = data.getStringExtra("dinnerTime");
            int drinkInterval = data.getIntExtra("drinkInterval", 1);
            int exerciseInterval = data.getIntExtra("exerciseInterval", 1);
            int bathroomInterval = data.getIntExtra("bathroomInterval", 1);
            int surveyInterval = data.getIntExtra("surveyInterval", 1);
            Log.d("MainActivity", "Breakfast Time: " + breakfastTime);
            Log.d("MainActivity", "Lunch Time: " + lunchTime);
            Log.d("MainActivity", "Dinner Time: " + dinnerTime);
            Log.d("MainActivity", "Drink Interval: " + drinkInterval);
            Log.d("MainActivity", "Exercise Interval: " + exerciseInterval);
            Log.d("MainActivity", "Bathroom Interval: " + bathroomInterval);
            Log.d("MainActivity", "Survey Interval: " + surveyInterval);

            // 在这里设置提醒
            setReminders(breakfastTime, lunchTime, dinnerTime, drinkInterval, exerciseInterval, bathroomInterval, surveyInterval);
        }

        if (requestCode == REQUEST_VOICE_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                sendMessage(spokenText);
            }
        }
    }


    private void sendMessage(String messageText) {
        // 将用户消息添加到聊天记录中
        addMessageToChat(messageText, true);

        // 从聊天记录中构建消息体
        JSONArray messagesArray = new JSONArray();
        for (Message message : messageList) {
            JSONObject messageObject = new JSONObject();
            try {
                messageObject.put("role", message.isUser() ? "user" : "assistant");
                messageObject.put("content", message.getText());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            messagesArray.put(messageObject);
        }

        // 从 SharedPreferences 获取 API 密钥
        SharedPreferences preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String apiKey = preferences.getString("api_key", null);

        if (apiKey == null || apiKey.isEmpty()) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "API key is missing!", Toast.LENGTH_SHORT).show());
            return;
        }

        Log.d("MainActivity", "API Key: " + apiKey); // 打印 API 密钥以确认

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                "{\n" +
                        "  \"model\": \"gpt-4\",\n" +
                        "  \"messages\": " + messagesArray.toString() + "\n" +
                        "}"
        );
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)  // 使用 Bearer 格式
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to connect to API", Toast.LENGTH_SHORT).show());
                Log.e("MainActivity", "Error: " + e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("MainActivity", "API Response: " + responseBody);

                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        JSONArray choices = jsonObject.getJSONArray("choices");
                        String botReply = choices.getJSONObject(0).getJSONObject("message").getString("content");

                        runOnUiThread(() -> {
                            if (textToSpeech != null) {
                                speak(botReply);
                            } else {
                                Log.e("MainActivity", "TextToSpeech not initialized");
                            }
                            addMessageToChat(botReply, false);
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e("MainActivity", "API request failed with response code: " + response.code());
                    Log.e("MainActivity", "Response: " + response.body().string());
                }
            }
        });
    }




    private void addMessageToChat(String messageText, boolean isUser) {
        Message message = new Message(messageText, isUser);
        messageList.add(message);
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void speak(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void setReminders(String breakfastTime, String lunchTime, String dinnerTime,
                              int drinkInterval, int exerciseInterval, int bathroomInterval, int surveyInterval) {
        // 设置提醒
        setReminder(breakfastTime, "早餐时间到");
        setReminder(lunchTime, "午餐时间到");
        setReminder(dinnerTime, "晚餐时间到");

        // 设置间隔提醒
        setIntervalReminder(drinkInterval, "喝水时间到");
        setIntervalReminder(exerciseInterval, "运动时间到");
        setIntervalReminder(bathroomInterval, "上厕所时间到");
        setIntervalReminder(surveyInterval, "做量表时间到");
    }

    private void setReminder(String time, String message) {
        long triggerTime = convertTimeToMillis(time); // 将时间转换为毫秒

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("message", message);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
    }

    private void setIntervalReminder(int interval, String message) {
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("message", message);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        long firstTrigger = System.currentTimeMillis() + interval * 60000; // 以分钟为单位
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, firstTrigger, interval * 60000, pendingIntent);
    }

    private long convertTimeToMillis(String time) {
        // 示例时间格式是 HH:mm
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);

        // 获取当前日期的开始时间（午夜）
        long now = System.currentTimeMillis();
        long todayMidnight = now - (now % 86400000); // 今日零点

        // 计算目标时间的毫秒值
        return todayMidnight + (hours * 3600000) + (minutes * 60000);
    }
}

