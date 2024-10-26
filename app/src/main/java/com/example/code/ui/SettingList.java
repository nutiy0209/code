package com.example.code.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;

import com.example.code.AlarmReceiver;
import com.example.code.R;

import java.util.Calendar;

public class SettingList extends AppCompatActivity {

    private TimePicker timePickerBreakfast;
    private TimePicker timePickerLunch;
    private TimePicker timePickerDinner;
    private NumberPicker numberPickerWater;
    private NumberPicker numberPickerExercise;
    private NumberPicker numberPickerBathroom;
    private NumberPicker numberPickerSurvey;
    private Button saveButton;
    private EditText editTextMessage; // 新增 EditText

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_list);

        // 初始化控件
        timePickerBreakfast = findViewById(R.id.timePickerBreakfast);
        timePickerLunch = findViewById(R.id.timePickerLunch);
        timePickerDinner = findViewById(R.id.timePickerDinner);
        numberPickerWater = findViewById(R.id.numberPickerWater);
        numberPickerExercise = findViewById(R.id.numberPickerExercise);
        numberPickerBathroom = findViewById(R.id.numberPickerBathroom);
        numberPickerSurvey = findViewById(R.id.numberPickerSurvey);
        saveButton = findViewById(R.id.saveButton);
        editTextMessage = findViewById(R.id.editTextMessage); // 初始化 EditText

        // 设置 NumberPicker 的值范围
        numberPickerWater.setMinValue(1);
        numberPickerWater.setMaxValue(120);
        numberPickerExercise.setMinValue(1);
        numberPickerExercise.setMaxValue(120);
        numberPickerBathroom.setMinValue(1);
        numberPickerBathroom.setMaxValue(120);
        numberPickerSurvey.setMinValue(1);
        numberPickerSurvey.setMaxValue(30);

        saveButton.setOnClickListener(v -> {
            // 获取 TimePicker 和 NumberPicker 的值
            String breakfastTime = getTimePickerValue(timePickerBreakfast);
            String lunchTime = getTimePickerValue(timePickerLunch);
            String dinnerTime = getTimePickerValue(timePickerDinner);

            int drinkInterval = numberPickerWater.getValue();
            int exerciseInterval = numberPickerExercise.getValue();
            int bathroomInterval = numberPickerBathroom.getValue();
            int surveyInterval = numberPickerSurvey.getValue();

            // 获取消息文本
            String message = editTextMessage.getText().toString();

            // 设置提醒
            setAlarms(breakfastTime, lunchTime, dinnerTime, drinkInterval, exerciseInterval, bathroomInterval, surveyInterval);

            // 返回到 MainActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("breakfastTime", breakfastTime);
            resultIntent.putExtra("lunchTime", lunchTime);
            resultIntent.putExtra("dinnerTime", dinnerTime);
            resultIntent.putExtra("drinkInterval", drinkInterval);
            resultIntent.putExtra("exerciseInterval", exerciseInterval);
            resultIntent.putExtra("bathroomInterval", bathroomInterval);
            resultIntent.putExtra("surveyInterval", surveyInterval);
            resultIntent.putExtra("message", message); // 添加消息
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private String getTimePickerValue(TimePicker timePicker) {
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        return String.format("%02d:%02d", hour, minute);
    }

    private void setAlarms(String breakfastTime, String lunchTime, String dinnerTime,
                           int drinkInterval, int exerciseInterval, int bathroomInterval, int surveyInterval) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // 设置早餐提醒
        setAlarm(alarmManager, breakfastTime, "早餐提醒");

        // 设置午餐提醒
        setAlarm(alarmManager, lunchTime, "午餐提醒");

        // 设置晚餐提醒
        setAlarm(alarmManager, dinnerTime, "晚餐提醒");

        // 设置定时提醒
        setPeriodicAlarm(alarmManager, drinkInterval * 60 * 60 * 1000, "喝水提醒");
        setPeriodicAlarm(alarmManager, exerciseInterval * 60 * 60 * 1000, "运动提醒");
        setPeriodicAlarm(alarmManager, bathroomInterval * 60 * 60 * 1000, "上厕所提醒");
        setPeriodicAlarm(alarmManager, surveyInterval * 24 * 60 * 60 * 1000, "做量表提醒"); // 天转毫秒
    }

    private void setAlarm(AlarmManager alarmManager, String time, String message) {
        String[] timeParts = time.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // 确保时间在将来
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("message", message);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT // 使用 FLAG_IMMUTABLE
        );

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        Log.d("MainActivity4", "Alarm set for " + time + " with message: " + message);
    }

    private void setPeriodicAlarm(AlarmManager alarmManager, int intervalMillis, String message) {
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("message", message);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT // 使用 FLAG_IMMUTABLE
        );

        long triggerTime = System.currentTimeMillis() + intervalMillis; // 首次触发时间

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, intervalMillis, pendingIntent);
        Log.d("MainActivity4", "Periodic alarm set with interval: " + intervalMillis + " ms and message: " + message);
    }
}
