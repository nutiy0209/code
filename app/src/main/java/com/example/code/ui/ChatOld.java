package com.example.code.ui;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.content.IntentFilter;
import android.content.ActivityNotFoundException;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.AdapterView;

import com.example.code.AlarmReceiver;
import com.example.code.api.ApiResponse;
import com.example.code.api.ApiService;
import com.example.code.Message;
import com.example.code.MessageAdapter;
import com.example.code.api.NostalgicRequest;
import com.example.code.R;
import com.example.code.exercise.PoseMaster;

public class ChatOld extends AppCompatActivity {

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
        setContentView(R.layout.chat_old);

        // 从 Intent 中获取 flask1 传递过来的数据
//        Intent flask = getIntent();
//        if (flask != null && flask.hasExtra("flaskReply")) {
//            String flaskReply = flask.getStringExtra("flaskReply");
//
//            // 打印收到的数据
//            Log.d("MainActivity6", "收到的 Flask 回應: " + flaskReply);
//
//            // 处理收到的数据，例如将其显示在聊天界面上
//            if (flaskReply != null) {
//                addMessageToChat(flaskReply, false); // 使用现有的聊天方法显示
//            }
//        }

        // 创建适配器
        Spinner spinnerMode = findViewById(R.id.SpinnerMode3);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_modes3, android.R.layout.simple_spinner_item);

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
                if ("量表模式".equals(selectedMode)) {
                    // 启动 MainActivity4
                    Intent intent = new Intent(ChatOld.this, ScaleChat.class);
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
        //editor.putString("api_key", "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");
        editor.apply();

        ImageButton setting = findViewById(R.id.Setting1);
        setting.setOnClickListener(v -> {
            Intent intent = new Intent(ChatOld.this, SettingList.class);
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
        buttonVoiceInput.setOnClickListener(v -> {
            // 先调用语音输入
            startVoiceInput();
        });


        // 注册广播接收器
        LocalBroadcastManager.getInstance(this).registerReceiver(reminderReceiver,
                new IntentFilter("com.example.code.REMINDER"));


    }


    private void callFlaskApi(String Query) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)  // 連線超時
                .readTimeout(60, TimeUnit.SECONDS)     // 讀取超時
                .writeTimeout(60, TimeUnit.SECONDS)    // 寫入超時
                .build();

        // 使用這個 OkHttpClient 建立 Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.245.8:5000/") // 替換為 Flask 伺服器的局域網 IP
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient) // 設置自定義 OkHttpClient
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        // 模拟一个输入数据，可以从其他地方获取数据
        String userQuery = Query; // 示例输入数据

        // 构建请求对象
        NostalgicRequest request = new NostalgicRequest(userQuery);

        // 发起网络请求
        Call<ApiResponse> call = apiService.sendQuery(request);

        // 异步请求，使用回调处理S
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse != null) {
                        // 打印日志或显示在 UI 上
                        Log.d("API_RESPONSE", "ChatGPT 回應: " + apiResponse.getChatgptReply());
                        Toast.makeText(ChatOld.this, "收到响应", Toast.LENGTH_SHORT).show();

                        // 将响应数据传递到聊天界面
                        addMessageToChat(apiResponse.getChatgptReply(), false);
                    }
                } else {
                    // 打印错误状态码和错误信息
                    Log.e("API_ERROR", "Response error: " + response.code() + " " + response.errorBody().toString());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                // 网络请求失败
                Log.e("API_ERROR", "Request failed: " + t.getMessage());
            }
        });
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

                // 调用 Flask API，将识别到的文本传递过去
                callFlaskApi(spokenText);
            }
        }
    }

    private void sendMessage(String messageText) {
        // 将用户消息添加到聊天记录中
        addMessageToChat(messageText, true);
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