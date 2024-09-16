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
import android.widget.Spinner;
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
import android.widget.AdapterView;

public class MainActivity5 extends AppCompatActivity {

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
        setContentView(R.layout.activity_main5);

        Spinner spinnerModes = findViewById(R.id.SpinnerModes2);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.modes_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModes.setAdapter(adapter);
        spinnerModes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                String selectedMode = (String) parent.getItemAtPosition(position);
                if ("聊天模式".equals(selectedMode)) {
                    Intent intent = new Intent(MainActivity5.this, MainActivity.class);
                    startActivity(intent);
                }
                if ("懷舊模式".equals(selectedMode)) {
                    Intent intent = new Intent(MainActivity5.this, MainActivity6.class);
                    startActivity(intent);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        recyclerView = findViewById(R.id.recyclerView1);
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        SharedPreferences preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("api_key", "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");
        editor.apply();

        Button button = findViewById(R.id.camera1);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity5.this, MainActivity2.class);
            startActivity(intent);
        });

        ImageButton person = findViewById(R.id.Person1);
        person.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity5.this, MainActivity3.class);
            startActivity(intent);
        });

        ImageButton setting = findViewById(R.id.Setting1);
        setting.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity5.this, MainActivity4.class);
            startActivityForResult(intent, REQUEST_CODE);
        });

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.TAIWAN);
                Log.d("TextToSpeech", "初始化成功");
            } else {
                Toast.makeText(this, "Text-to-Speech 初始化失败", Toast.LENGTH_SHORT).show();
                Log.e("TextToSpeech", "初始化失败");
            }
        });

        TextView textViewName = findViewById(R.id.textView2);
        Intent intent = getIntent();
        String userName = intent.getStringExtra("userName");
        if (userName != null && !userName.isEmpty()) {
            textViewName.setText(userName);
        }

        Button buttonVoiceInput = findViewById(R.id.TALK1);
        buttonVoiceInput.setOnClickListener(v -> startVoiceInput());

        LocalBroadcastManager.getInstance(this).registerReceiver(reminderReceiver,
                new IntentFilter("com.example.code.REMINDER"));

        sendInitialTextsToChatGPT(); // 调用发送长文的方法
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(reminderReceiver);
        super.onDestroy();
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-TW");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "请开始说话...");

        try {
            startActivityForResult(intent, REQUEST_VOICE_INPUT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "设备不支持语音输入", Toast.LENGTH_SHORT).show();
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
            Log.d("MainActivity", "早餐时间: " + breakfastTime);
            Log.d("MainActivity", "午餐时间: " + lunchTime);
            Log.d("MainActivity", "晚餐时间: " + dinnerTime);
            Log.d("MainActivity", "喝水间隔: " + drinkInterval);
            Log.d("MainActivity", "运动间隔: " + exerciseInterval);
            Log.d("MainActivity", "上厕所间隔: " + bathroomInterval);
            Log.d("MainActivity", "做量表间隔: " + surveyInterval);

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
        // 添加消息到聊天中
        addMessageToChat(messageText, true);

        // 构建消息数组
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

        SharedPreferences preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String apiKey = preferences.getString("api_key", null);

        if (apiKey == null || apiKey.isEmpty()) {
            runOnUiThread(() -> Toast.makeText(MainActivity5.this, "API 密钥缺失！", Toast.LENGTH_SHORT).show());
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                "{\n" +
                        "  \"model\": \"gpt-4\",\n" +
                        "  \"messages\": " + messagesArray.toString() + "\n" +
                        "}");

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity5.this, "请求失败，请检查网络连接或重试", Toast.LENGTH_SHORT).show());
                Log.e("ChatGPT", "请求失败", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(MainActivity5.this, "请求失败", Toast.LENGTH_SHORT).show());
                    Log.e("ChatGPT", "请求失败: " + response.message());
                    return;
                }

                String responseBody = response.body().string();
                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray choices = jsonResponse.getJSONArray("choices");
                    JSONObject choice = choices.getJSONObject(0);
                    String reply = choice.getJSONObject("message").getString("content");

                    runOnUiThread(() -> {
                        addMessageToChat(reply, false);
                        textToSpeech.speak(reply, TextToSpeech.QUEUE_FLUSH, null, null);
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private void addMessageToChat(String message, boolean isUser) {
        messageList.add(new Message(message, isUser));
        messageAdapter.notifyDataSetChanged();
        recyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void setReminders(String breakfastTime, String lunchTime, String dinnerTime, int drinkInterval, int exerciseInterval, int bathroomInterval, int surveyInterval) {
        // 设置提醒时间
        setReminder(breakfastTime, "吃饭");
        setReminder(lunchTime, "吃饭");
        setReminder(dinnerTime, "吃饭");
        setReminderForInterval(drinkInterval, "喝水");
        setReminderForInterval(exerciseInterval, "运动");
        setReminderForInterval(bathroomInterval, "上厕所");
        setReminderForInterval(surveyInterval, "做量表");
    }

    private void setReminder(String time, String message) {
        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("message", message);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        long triggerTime = getTriggerTime(hour, minute);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
    }

    private void setReminderForInterval(int interval, String message) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("message", message);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        long triggerTime = System.currentTimeMillis() + interval * 60 * 1000;
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, interval * 60 * 1000, pendingIntent);
    }

    private long getTriggerTime(int hour, int minute) {
        long now = System.currentTimeMillis();
        long triggerTime = now;
        // 设置触发时间为指定的小时和分钟
        return triggerTime;
    }

    private void sendInitialTextsToChatGPT() {
        JSONArray messagesArray = new JSONArray();

        // 添加第一段长文
        addTextToMessagesArray(messagesArray, "\n" +
                "### 失智症量表問題問法規則（更新版）\n" +
                "\n" +
                "1. **使用簡單易懂的繁體中文**：避免使用複雜詞彙和專業術語，確保患者能夠理解問題，保持口語化和自然的語氣。\n" +
                "\n" +
                "2. **把握核心問題**：每次只提問一個問題，確保問題簡單且重點明確，避免一次給患者過多資訊，讓他們有充足時間思考。\n" +
                "\n" +
                "3. **引導患者回答**：當患者無法正確理解或回答問題時，適時提供提示或範例。例如：「今天是星期幾？」如不確定，可提示「今天是週二還是週三？」\n" +
                "\n" +
                "4. **觀察並調整問題**：如患者表現出困難或困惑，適當調整提問方式。如「記住三個物品」改為「記住一個物品」，以減少負擔。\n" +
                "\n" +
                "5. **簡單範圍選擇**：對於需要選擇的問題，提供簡單且清晰的選項，如：「現在是春天還是夏天？」\n" +
                "\n" +
                "6. **引導詳細回憶**：在涉及記憶問題時，鼓勵患者慢慢回憶，不要催促。例如：「還記得上次和誰一起去公園嗎？」\n" +
                "\n" +
                "7. **循序漸進地詢問**：問題從簡單到困難，先從定向感問題開始，再逐漸增加記憶或計算難度。\n" +
                "\n" +
                "8. **關注患者狀態**：當患者表現出焦慮或困惑時，調整提問節奏，關心患者感受。如：「沒關係，我們可以慢慢來，不著急。」\n" +
                "\n" +
                "9. **確認回答**：根據患者回答適時給予確認或鼓勵。例如：「很好，您答對了，是星期三。」\n" +
                "\n" +
                "10. **保持對話自然流暢**：避免過於正式的語氣，保持對話口語化、自然。如患者回答錯誤，不直接指出錯誤，而是以溫和的方式引導正確答案。\n" +
                "\n" +
                "### **患者回答內容的紀錄與評判方式**\n" +
                "1. **記錄回答**：每個問題的患者回答都需要詳細紀錄，包括是否出現偏題或無關內容。例：「蘋果很好吃」這樣的回應可表示患者轉移注意力。\n" +
                "   \n" +
                "2. **適時延伸問題**：根據患者的回答進行延伸或簡化提問。如「我忘記了」這類回應，可進一步詢問具體細節，或選擇更簡單的問題。\n" +
                "\n" +
                "3. **評估標準**：\n" +
                "   - **正確回答**：根據問題本身的標準進行評分（如2分、1分、0分）。\n" +
                "   - **偏離主題或無關回答**：此類回答需特別注意，可能顯示患者的注意力或記憶問題，應視情況調整提問方式。\n" +
                "   - **回答困難或模糊**：如果患者無法明確回答，應給予更簡單的問題，並根據其表現進行相應評估。\n" +
                "\n" +
                "4. **觀察行為與情緒**：如患者表現出焦慮、迷惑或反覆提到非相關的事情，應適當降低問題難度，並記錄患者的情緒反應，以便進行更全面的評估。\n" +
                "\n" +
                "這些規則旨在讓問答過程更加順暢和自然，同時確保記錄和評估患者的回答，以獲得準確的評估結果。", true);

        // 添加第二段长文
        addTextToMessagesArray(messagesArray, "這是一個失智症評估量表，涵蓋了日常照護、運動、飲食、記憶、認知能力等多個方面。每個問題根據患者的反應進行評分，分為 2 分、1 分和 0 分。以下是各分類的問題和評分標準：\n" +
                "\n" +
                "1. 日常照護\n" +
                "財務決策：有沒有做出不好的決定或送過不合宜的禮物？\n" +
                "2分：完全沒有變化\n" +
                "1分：輕微改變\n" +
                "0分：明顯改變或困難\n" +
                "使用小型工具或家電：如電視、電話、遙控器，是否有困難？\n" +
                "2分：完全沒有變化\n" +
                "1分：輕微困難\n" +
                "0分：明顯困難或不會使用\n" +
                "處理財務事務：是否經常忘記支付帳單或處理家庭財務？\n" +
                "2分：完全沒有變化\n" +
                "1分：偶爾忘記\n" +
                "0分：經常忘記或無法處理\n" +
                "2. 運動\n" +
                "抄寫圖形：能否抄寫交疊的五角形？\n" +
                "2分：完全正確\n" +
                "1分：部分正確\n" +
                "0分：無法完成\n" +
                "拼圖：能否根據圖片拼圖組合？\n" +
                "2分：完全正確\n" +
                "1分：部分正確\n" +
                "0分：無法完成\n" +
                "3. 飲食\n" +
                "描述蘋果：能否正確描述蘋果的樣子？\n" +
                "2分：描述正確且完整\n" +
                "1分：描述部分正確\n" +
                "0分：無法描述或錯誤\n" +
                "解釋句子：「飲食健康很重要」這句話的意思是什麼？\n" +
                "2分：解釋正確且完整\n" +
                "1分：解釋部分正確\n" +
                "0分：無法解釋或錯誤\n" +
                "4. 記憶\n" +
                "時間與日期：今天是星期幾？現在是哪個月份？\n" +
                "2分：完全正確\n" +
                "1分：部分正確\n" +
                "0分：錯誤或無回答\n" +
                "記憶物品：請記住三樣物品（如蘋果、鐘錶、書本），稍後問。\n" +
                "2分：記住三樣\n" +
                "1分：記住1-2樣\n" +
                "0分：記不起任何物品\n" +
                "重複問題：是否經常忘記月份和年份，或經常重複相同問題？\n" +
                "2分：完全沒有\n" +
                "1分：偶爾\n" +
                "0分：經常\n" +
                "回憶物品：能否回憶剛才記住的物品？\n" +
                "2分：完全正確\n" +
                "1分：記起1-2樣\n" +
                "0分：記不起任何物品\n" +
                "5. 認知能力\n" +
                "簡單數學：9加3等於多少？再加6等於多少？\n" +
                "2分：完全正確\n" +
                "1分：部分正確\n" +
                "0分：錯誤或無回答\n" +
                "連續減數：100減7，連續減五次。\n" +
                "2分：完全正確\n" +
                "1分：1-4次正確\n" +
                "0分：全部錯誤或無法完成\n" +
                "命名物品：能否說出筆和錶的名字？\n" +
                "2分：完全正確\n" +
                "1分：部分正確\n" +
                "0分：無法命名\n" +
                "反義詞：「快的反義詞是慢」，那「大的反義詞是什麼」？\n" +
                "2分：完全正確\n" +
                "1分：部分正確\n" +
                "0分：錯誤或無回答\n" +
                "類別辨識：桔子和香蕉屬於什麼類型？紅色和藍色屬於哪一類？\n" +
                "2分：完全正確\n" +
                "1分：部分正確\n" +
                "0分：錯誤或無回答\n" +
                "6. 其他\n" +
                "興趣減少：對以前喜歡的活動或嗜好是否興趣減少？\n" +
                "2分：完全沒有\n" +
                "1分：偶爾減少\n" +
                "0分：顯著減少\n" +
                "當前位置：今天您在哪裡？（如縣市、醫院、病房、床號、樓層）\n" +
                "2分：完全正確\n" +
                "1分：部分正確\n" +
                "0分：錯誤或無回答", true);

        sendMessagesToChatGPT(messagesArray, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity5.this, "请求失败", Toast.LENGTH_SHORT).show());
                Log.e("ChatGPT", "请求失败", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(MainActivity5.this, "请求失败", Toast.LENGTH_SHORT).show());
                    Log.e("ChatGPT", "请求失败: " + response.message());
                    return;
                }

                String responseBody = response.body().string();
                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray choices = jsonResponse.getJSONArray("choices");
                    JSONObject choice = choices.getJSONObject(0);
                    String reply = choice.getJSONObject("message").getString("content");

                    // 在主线程中处理回复
                    runOnUiThread(() -> {
                        addMessageToChat(reply, false);
                        // 发送第二段文本的回复
                        addTextToMessagesArray(messagesArray, "", true);
                        sendMessagesToChatGPT(messagesArray, new Callback() {
                            @Override
                            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                                runOnUiThread(() -> Toast.makeText(MainActivity5.this, "请求失败", Toast.LENGTH_SHORT).show());
                                Log.e("ChatGPT", "请求失败", e);
                            }

                            @Override
                            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                                if (!response.isSuccessful()) {
                                    runOnUiThread(() -> Toast.makeText(MainActivity5.this, "请求失败", Toast.LENGTH_SHORT).show());
                                    Log.e("ChatGPT", "请求失败: " + response.message());
                                    return;
                                }

                                String responseBody = response.body().string();
                                try {
                                    JSONObject jsonResponse = new JSONObject(responseBody);
                                    JSONArray choices = jsonResponse.getJSONArray("choices");
                                    JSONObject choice = choices.getJSONObject(0);
                                    String reply = choice.getJSONObject("message").getString("content");

                                    runOnUiThread(() -> addMessageToChat("第二段长文的回复: " + reply, false));
                                    // 初始化完成后，允许用户进行聊天
                                    Toast.makeText(MainActivity5.this, "初始对话已完成，您现在可以进行聊天", Toast.LENGTH_SHORT).show();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void addTextToMessagesArray(JSONArray messagesArray, String text, boolean isUserMessage) {
        JSONObject messageObject = new JSONObject();
        try {
            messageObject.put("role", isUserMessage ? "user" : "assistant");
            messageObject.put("content", text);
            messagesArray.put(messageObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void sendMessagesToChatGPT(JSONArray messagesArray, Callback callback) {
        SharedPreferences preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String apiKey = preferences.getString("api_key", "");

        if (apiKey == null || apiKey.isEmpty()) {
            runOnUiThread(() -> Toast.makeText(MainActivity5.this, "API 密钥缺失！", Toast.LENGTH_SHORT).show());
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject requestBodyObject = new JSONObject();
        try {
            requestBodyObject.put("model", "gpt-4");
            requestBodyObject.put("messages", messagesArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(requestBodyObject.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        client.newCall(request).enqueue(callback);
    }



    public static class ReminderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Intent broadcastIntent = new Intent("com.example.code.REMINDER");
            broadcastIntent.putExtra("message", message);
            LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
        }
    }
}
