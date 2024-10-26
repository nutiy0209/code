package com.example.code.ui;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;

import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

import androidx.camera.lifecycle.ProcessCameraProvider;

import com.example.code.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import android.speech.tts.TextToSpeech;
import android.widget.Toast;

@OptIn(markerClass = ExperimentalGetImage.class)
public class PoseMaster extends AppCompatActivity {

    // CameraX库的组件，用于提供相机的提供者。
    private PreviewView previewView;
    private Paint mPaint;
    private Display display;
    private ImageAnalysis imageAnalysis;
    private Preview preview;

    private TextView tvPoseInfo; // 引用TextView
    private TextView tvPoseInfo_2;
    private TextView tvPoseInfo_3;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    // 用于跟踪当前选择的摄像头，默认为后置摄像头
    private CameraSelector cameraSelector;
    private int lensFacing = CameraSelector.LENS_FACING_FRONT; // 默认设置为后置摄像头
    private boolean isFrontCamera() {
        return lensFacing == CameraSelector.LENS_FACING_FRONT;
    }

    // 请求权限时使用的请求码。
    private static final int PERMISSION_REQUESTS = 1;
    // 只保留最新的一貞
    private static final int SOME_THRESHOLD = 1;

    // 阈值和状态配置。
    private static final float straightArmThreshold = 0.1f;// 手臂伸直的阈值。
    //點頭
    private int checkNodding = 0;
    private int nodCount = 0; // 點頭次數
    private boolean isNodding = false; // 是否正在點頭
    private boolean noddingDirectionDown = false; // 点头方向是否向下
    private static final float NOD_THRESHOLD_START = 8.0f; // 開始點頭閥值
    private static final float NOD_THRESHOLD_END = 8.0f; // 結束點頭閥值
    //搖頭
    private int checkShaking = 0;
    private int shakeCount = 0; // 计算摇头的次数
    private boolean isShaking = false; // 是否正在摇头
    private boolean ShakingDirectionLeft = false; // 点头方向是否向左
    private static final float SHAKE_THRESHOLD_START = 8.0f; // 开始摇头的阈值
    private static final float SHAKE_THRESHOLD_END = 8.0f; // 结束摇头的阈值
    //坐站坐
    public int key = 1;
    private static final int SITTING = 0;
    private static final int STANDING = 1;
    private int currentState = SITTING;
    private int standUpCount = 0;
    //二頭肌
    private int leftArmStraightenCount = 0; // 左手臂伸直次数
    private int rightArmStraightenCount = 0; // 右手臂伸直次数
    private boolean isLeftArmStraightening = false; // 左手臂是否正在伸直
    private boolean isRightArmStraightening = false; // 右手臂是否正在伸直

    //距離
    private static float Distance = 0;
    // 头部位置相关
    private static final int WINDOW_SIZE = 8; // 用于移动平均的窗口大小
    private PointF lastAvgNosePosition  = null;
    private final Queue<PointF> nosePositions = new LinkedList<>();

    // 語音合成
    private TextToSpeech textToSpeech;
    // 标志位，指示 TTS 是否初始化完成
    private boolean isTTSInitialized = false;
    private boolean hasSpokenDetecting = false;

    //計算PointF 對列中所有點的平均位置
    private PointF calculateAverage(Queue<PointF> points) {
        float sumX = 0, sumY = 0;
        for (PointF point : points) {
            sumX += point.x;
            sumY += point.y;
        }
        return new PointF(sumX / points.size(), sumY / points.size());
    }
    // 計算兩點距離
    private float calculateDistance(PointF point1, PointF point2) {
        return (float) Math.sqrt(Math.pow(point2.x - point1.x, 2) + Math.pow(point2.y - point1.y, 2));
    }
    //计算由三个点 A, B, C 形成的角 ABC 的大小（以度为单位）
    private float calculateAngle(PointF a, PointF b, PointF c) {
        // 向量 AB
        float abX = b.x - a.x;
        float abY = b.y - a.y;

        // 向量 BC
        float bcX = c.x - b.x;
        float bcY = c.y - b.y;

        // AB 和 BC 的点积
        float dotProduct = (abX * bcX) + (abY * bcY);
        // AB 和 BC 的模（长度）
        float magnitudeAB = (float) Math.sqrt(abX * abX + abY * abY);
        float magnitudeBC = (float) Math.sqrt(bcX * bcX + bcY * bcY);

        // 计算 AB 和 BC 的夹角（弧度）
        float angleInRadians = (float) Math.acos(dotProduct / (magnitudeAB * magnitudeBC));

        // 将弧度转换为度

        return (float) Math.toDegrees(angleInRadians);
    }

    // Base pose detector with streaming frames, when depending on the pose-detection sdk
    // ML Kit Pose Detection的配置选项。
    PoseDetectorOptions options =
            new PoseDetectorOptions.Builder()
                    .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                    .build();
    // PoseDetector 用來偵測圖片或影像中的人體姿勢
    PoseDetector poseDetector = PoseDetection.getClient(options);
    // 畫布上繪製影像或姿勢
    Canvas canvas;
    // 用來保存當前處理後的 Bitmap 影像
    Bitmap bitmap4Save;

    ArrayList<Bitmap> bitmapArrayList = new ArrayList<>();
    ArrayList<Bitmap> bitmap4DisplayArrayList = new ArrayList<>();
    ArrayList<Pose> poseArrayList = new ArrayList<>();

    boolean isRunning = false;

    @SuppressLint("MissingInflatedId")
    @OptIn(markerClass = ExperimentalGetImage.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pose_master);

        previewView = findViewById(R.id.previewView);
        display = findViewById(R.id.displayOverlay);
        tvPoseInfo = findViewById(R.id.tv_pose_info);
        tvPoseInfo_2 = findViewById(R.id.tv_pose_info2);
        tvPoseInfo_3 = findViewById(R.id.tv_pose_info3);

        mPaint = new Paint();
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setStrokeWidth(10);

        // 初始化摄像头
        initializeCamera();
        Button button = findViewById(R.id.setting); // 替换为您的按钮 ID
        button.setOnClickListener(v -> {
            // 创建 RadioGroup
            RadioGroup radioGroup = new RadioGroup(PoseMaster.this);
            RadioButton radioButton1 = new RadioButton(PoseMaster.this);
            RadioButton radioButton2 = new RadioButton(PoseMaster.this);
            RadioButton radioButton3 = new RadioButton(PoseMaster.this);
            RadioButton radioButton4 = new RadioButton(PoseMaster.this);

            radioButton1.setText("左右手臂伸直");
            radioButton2.setText("站起來");
            radioButton3.setText("手舉起");
            radioButton4.setText("點頭搖頭");

            radioGroup.addView(radioButton1);
            radioGroup.addView(radioButton2);
            radioGroup.addView(radioButton3);
            radioGroup.addView(radioButton4);
            // 默認
            radioButton1.setChecked(true);

            //  AlertDialog
            AlertDialog.Builder builder = new AlertDialog.Builder(PoseMaster.this);
            builder.setTitle("要識別的動作");
            builder.setView(radioGroup);
            builder.setPositiveButton("確認", (dialog, which) -> {
                int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
                // 在这里处理选择结果
                if (checkedRadioButtonId == radioButton1.getId()) {
                    key = 1;
                } else if (checkedRadioButtonId == radioButton2.getId()) {
                    key = 2;
                }else if (checkedRadioButtonId == radioButton3.getId()) {
                    key = 3;
                }else if (checkedRadioButtonId == radioButton4.getId()) {
                    key = 4;
                }
            });
            builder.setNegativeButton("取消", null);
            builder.show();
        });
        // 初始化 TextToSpeech 对象
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    // 设置语言为繁体中文
                    int result = textToSpeech.setLanguage(Locale.TRADITIONAL_CHINESE);

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // 语言数据丢失或不支持
                        Log.e("TTS", "繁体中文语言不支持");
                        Toast.makeText(getApplicationContext(), "繁体中文语言不支持", Toast.LENGTH_SHORT).show();
                    } else {
                        // 初始化成功，设置标志位
                        isTTSInitialized = true;
                    }
                } else {
                    Log.e("TTS", "初始化失败");
                }
            }
        });



        //切換攝像頭按鈕
        Button switchCameraButton = findViewById(R.id.switchCameraButton);
        switchCameraButton.setOnClickListener(v -> {
            // 切換鏡頭
            lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK)
                    ? CameraSelector.LENS_FACING_FRONT
                    : CameraSelector.LENS_FACING_BACK;

            // 更新cameraSelector
            cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (Exception e) {
                    Log.e("CameraX", "Error switching camera", e);
                }
            }, ContextCompat.getMainExecutor(this));
        });

        if (!allPermissionsGranted()) {
            getRuntimePermissions();
        }
    }
    private void initializeCamera() {
        // 初始化CameraX相关组件
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                if (imageAnalysis == null) {
                    imageAnalysis = createImageAnalysis();
                }
                if (preview == null) {
                    preview = createPreview();
                }
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("MainActivity", "Error initializing camera", e);
                Thread.currentThread().interrupt();
            }
        }, ContextCompat.getMainExecutor(this));
    }
    private Preview createPreview() {
        Preview preview = new Preview.Builder()
                .setTargetRotation(previewView.getDisplay().getRotation())
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        return preview;
    }

    // 用于处理ML Kit的姿态检测结果。
    Runnable RunMlkit = () -> poseDetector.process(InputImage.fromBitmap(bitmapArrayList.get(0),0))
            .addOnSuccessListener(pose -> {
                poseArrayList.add(pose);
                // 处理完成，设置 isRunning 为 false
                isRunning = false;
            }).addOnFailureListener(e -> {
                Log.e("PoseDetection", "Error during pose detection", e);
                isRunning = false;
            });
    public void speakText(String text) {
        if (isTTSInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            Toast.makeText(getApplicationContext(), "语音引擎尚未初始化", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    //手舉起
    private boolean isArmRaised(PoseLandmark shoulder, PoseLandmark elbow, PoseLandmark wrist) {
        if (shoulder == null || elbow == null || wrist == null) {
            return false;
        }

        // 檢查肩膀、肘部和手腕的置信度是否都大於或等於0.5
        if (shoulder.getInFrameLikelihood() < 0.5 || elbow.getInFrameLikelihood() < 0.5 || wrist.getInFrameLikelihood() < 0.5) {
            return false;
        }

        float shoulderToElbowDistance = calculateDistance(shoulder.getPosition(), elbow.getPosition());
        float elbowToWristDistance = calculateDistance(elbow.getPosition(), wrist.getPosition());

        // 如果手腕的y坐标低于肩膀的y坐标，且手臂伸直，則認為手臂举起
        return wrist.getPosition().y < shoulder.getPosition().y &&
                elbowToWristDistance > shoulderToElbowDistance - straightArmThreshold;
    }

    private boolean isSitting(PoseLandmark leftHip, PoseLandmark leftKnee, PoseLandmark rightHip, PoseLandmark rightKnee,
                              PoseLandmark leftShoulder, PoseLandmark rightShoulder) {
        // 檢查關鍵點是否為 null
        if (leftHip == null || leftKnee == null || rightHip == null || rightKnee == null || leftShoulder == null || rightShoulder == null) {
            return false; // 如果任何關鍵點為 null，則無法判斷
        }

        // 檢查膝蓋的自信度是否大於等於 0.5
        if (leftKnee.getInFrameLikelihood() < 0.5 || rightKnee.getInFrameLikelihood() < 0.5) {
            return true; // 如果膝蓋自信度過低，判斷為坐著
        }

        // 計算膝蓋與腰部的角度
        float angleAtKneeLeft = calculateAngle(leftHip.getPosition(), leftKnee.getPosition(), leftShoulder.getPosition());
        float angleAtKneeRight = calculateAngle(rightHip.getPosition(), rightKnee.getPosition(), rightShoulder.getPosition());

        // 計算腰部與肩膀的垂直位置差
        float leftHipToShoulder = leftShoulder.getPosition().y - leftHip.getPosition().y;
        float rightHipToShoulder = rightShoulder.getPosition().y - rightHip.getPosition().y;

        // 定義角度和位置的閾值
        float standingAngleThreshold = 165.0f; // 站立時的膝蓋角度
        float sittingHipToShoulderThreshold = 50.0f; // 坐著時腰部與肩膀的相對高度差，視情況調整

        // 判斷站立條件：膝蓋角度大於或等於 165 度，並且臀部與肩膀之間的距離小於一定值
        boolean isStanding = angleAtKneeLeft >= standingAngleThreshold && angleAtKneeRight >= standingAngleThreshold;

        // 判斷坐著條件：膝蓋角度小於站立閾值，並且臀部應該在肩膀下方（坐著時肩膀應該在較高位置）

        return !isStanding && leftHipToShoulder > sittingHipToShoulderThreshold && rightHipToShoulder > sittingHipToShoulderThreshold; // 返回是否為坐著
    }

    private int continuousStandingFrames = 0; // 追蹤持續站立的幀數
    private static final int STANDING_FRAME_THRESHOLD = 8; // 定義連續幀數閾值

    @SuppressLint("SetTextI18n")
    private void updateStandingCount(PoseLandmark leftHip, PoseLandmark leftKnee, PoseLandmark rightHip, PoseLandmark rightKnee,
                                     PoseLandmark leftShoulder, PoseLandmark rightShoulder) {
        // 判斷是否坐著
        boolean isCurrentlySitting = isSitting(leftHip, leftKnee, rightHip, rightKnee, leftShoulder, rightShoulder);

        // 判斷是否站立
        boolean isCurrentlyStanding = !isCurrentlySitting;

        // 當狀態從坐著變為站立
        if (currentState == SITTING && isCurrentlyStanding) {
            continuousStandingFrames++; // 增加連續站立幀數
            if (continuousStandingFrames >= STANDING_FRAME_THRESHOLD) {
                currentState = STANDING;
                continuousStandingFrames = 0; // 重置站立幀數
            }
        }
        // 當狀態從站立變為坐著，並且先前的狀態是站立
        if (currentState == STANDING && isCurrentlySitting) {
            currentState = SITTING;
            standUpCount++;
            Log.d("PoseDetection2", "站立次數: " + standUpCount);
            runOnUiThread(() -> tvPoseInfo_2.setText("站立次數: " + standUpCount));
        }

        // 如果沒有滿足站立條件，則重置站立幀數
        if (!isCurrentlyStanding) {
            continuousStandingFrames = 0;
        }
    }

    @SuppressLint("SetTextI18n")
    private ImageAnalysis createImageAnalysis() {
        // 创建ImageAnalysis对象并设置分析器
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ActivityCompat.getMainExecutor(this), imageProxy -> {
            if (!poseArrayList.isEmpty()) {

                Pose pose = poseArrayList.get(0);

                // 获取鼻子的 PoseLandmark
                PoseLandmark nose = pose.getPoseLandmark(PoseLandmark.NOSE);
                // 獲取右肩、右肘、右腕的 PoseLandmark
                PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
                PoseLandmark rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);
                PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
                // 獲取左肩、左肘、左腕的 PoseLandmark
                PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
                PoseLandmark leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
                PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
                // 获取臀部、膝盖和脚踝的 PoseLandmark
                PoseLandmark leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
                PoseLandmark leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE);
                PoseLandmark rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);
                PoseLandmark rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE);
                //嘴巴
                PoseLandmark leftMouth = pose.getPoseLandmark(PoseLandmark.LEFT_MOUTH);
                PoseLandmark rightMouth = pose.getPoseLandmark(PoseLandmark.RIGHT_MOUTH);
                //腳

                //距離換算
                float referenceDistance = 0.15f;
                float referenceMouthWidth = 0.02f;
                if (leftMouth != null && rightMouth != null) {
                    // 計算當前鼻子寬度
                    float currentNoseWidth = calculateDistance(leftMouth.getPosition(), rightMouth.getPosition());

                    // 使用简单的比例估算距离
                    Distance = (referenceDistance * (referenceMouthWidth / currentNoseWidth)) * 500000;
                    @SuppressLint("DefaultLocale") String formattedDistance = String.format("%.2f", Distance);
                    // 处理计算出的距离
                    Log.d("Distance", "Estimated distance: " + formattedDistance + " 公分");
                    runOnUiThread(() -> tvPoseInfo_3.setText(formattedDistance + "公分"));
                }

                //左右手臂伸直計數
                if(key == 1){
                    float ANGLE_THRESHOLD_FOR_BEND = 50.0f; // 手臂弯曲的角度阈值
                    float ANGLE_THRESHOLD_FOR_STRAIGHT = 160.0f;
                    if (leftShoulder != null && leftElbow != null && leftWrist != null) {
                        runOnUiThread(() -> tvPoseInfo.setText("偵測中... "  ));

                        float angleAtLeftElbow = calculateAngle(leftShoulder.getPosition(), leftElbow.getPosition(), leftWrist.getPosition());
                        // 检查左手臂是否从弯曲到伸直，并且手腕高于鼻子
                        if (angleAtLeftElbow <= ANGLE_THRESHOLD_FOR_STRAIGHT && leftWrist.getPosition().y > (leftMouth != null ? leftMouth.getPosition().y : 0)) {
                            if (!isLeftArmStraightening) {
                                //                            Log.d("PoseDetection", "伸直手臂: ");
                                isLeftArmStraightening = true; // 开始伸直左手臂
                            }
                        } else if (angleAtLeftElbow <= ANGLE_THRESHOLD_FOR_BEND) {
                            if (isLeftArmStraightening) {
                                //                            Log.d("PoseDetection", "彎曲");
                                isLeftArmStraightening = false; // 结束伸直左手臂
                                leftArmStraightenCount++; // 累加左手臂伸直次数
                                //                            Log.d("PoseDetection", "左手臂伸直次数: " + leftArmStraightenCount);
                            }
                        }
                    }
                    if (rightShoulder != null && rightElbow != null && rightWrist != null) {
                        runOnUiThread(() -> tvPoseInfo.setText("偵測中... "  ));

                        float angleAtRightElbow = calculateAngle(rightShoulder.getPosition(), rightElbow.getPosition(), rightWrist.getPosition());
                        // 检查右手臂是否从弯曲到伸直，并且手腕高于鼻子
                        if (angleAtRightElbow <= ANGLE_THRESHOLD_FOR_STRAIGHT && rightWrist.getPosition().y > (rightMouth != null ? rightMouth.getPosition().y : 0)) {
                            if (!isRightArmStraightening) {
                                Log.d("PoseDetection", "伸直手臂: ");
                                isRightArmStraightening = true; // 开始伸直右手臂
                            }
                        } else if (angleAtRightElbow <= ANGLE_THRESHOLD_FOR_BEND) {
                            if (isRightArmStraightening) {
                                Log.d("PoseDetection", "彎曲");
                                isRightArmStraightening = false; // 结束伸直右手臂
                                rightArmStraightenCount++; // 累加右手臂伸直次数
                                //                                Log.d("PoseDetection", "右手臂伸直次数: " + rightArmStraightenCount);
                            }
                        }
                    }
                    runOnUiThread(() -> {
                        @SuppressLint("DefaultLocale") String text = String.format("%s手臂伸直: %d, %s手臂伸直: %d",
                                isFrontCamera() ? "右" : "左", leftArmStraightenCount,
                                isFrontCamera() ? "左" : "右", rightArmStraightenCount);
                        tvPoseInfo_2.setText(text);
                    });
                }

                //站起來計數
                if(key == 2) {
                    if (Distance < 30) {
                        runOnUiThread(() -> tvPoseInfo.setText("你離鏡頭太近了!" ));
                    } else {
                        runOnUiThread(() -> tvPoseInfo.setText("偵測中... "  ));
                        updateStandingCount(leftHip, leftKnee, rightHip, rightKnee, leftShoulder, rightShoulder);
                    }
                }

                hasSpokenDetecting = false;
                //判斷哪隻手舉起(左,右,雙)
                if (key == 3) {
                    runOnUiThread(() -> tvPoseInfo.setText("偵測中... "));

                    if (!hasSpokenDetecting) {
                        speakText("偵測中");
                        hasSpokenDetecting = true; // 设置标志，防止重复朗读
                    }

                    boolean isLeftArmRaised = isArmRaised(leftShoulder, leftElbow, leftWrist);
                    boolean isRightArmRaised = isArmRaised(rightShoulder, rightElbow, rightWrist);

                    runOnUiThread(() -> tvPoseInfo_2.setText("舉起你的手"));
                    if (isLeftArmRaised && isRightArmRaised) {
                        Log.d("PoseDetection", "Both arms are raised!");
                        runOnUiThread(() -> tvPoseInfo_2.setText("舉雙手"));
                    } else {
                        if (isLeftArmRaised) {
                            runOnUiThread(() -> tvPoseInfo_2.setText(isFrontCamera() ? "舉右手" : "舉左手"));
                        }
                        if (isRightArmRaised) {
                            runOnUiThread(() -> tvPoseInfo_2.setText(isFrontCamera() ? "舉左手" : "舉右手"));
                        }
                    }
                } else if (key == 4) {// 检查點頭搖頭
                    if (nose != null) {
                        runOnUiThread(() -> tvPoseInfo.setText("偵測中... "  ));
                        // 如果还没有朗读过“偵測中”，则朗读一次
                        if (!hasSpokenDetecting) {
                            speakText("偵測中");
                            hasSpokenDetecting = true; // 设置标志，防止重复朗读
                        }

                        PointF currentNosePosition = nose.getPosition();
                        // 更新鼻子位置队列
                        nosePositions.add(currentNosePosition);
                        if (nosePositions.size() > WINDOW_SIZE) {
                            nosePositions.poll(); // 如果队列满了，移除最旧的数据
                        }
                        // 计算移动平均
                        PointF avgNosePosition = calculateAverage(nosePositions);

                        // 只有当我们有足够的数据时，才进行点头或摇头的判断
                        if (nosePositions.size() == WINDOW_SIZE && lastAvgNosePosition != null) {
                            float deltaY = avgNosePosition.y - lastAvgNosePosition.y;
                            float deltaX = avgNosePosition.x - lastAvgNosePosition.x;

                            // 檢測點頭（頭部上下运动）
                            if (Math.abs(deltaY) > NOD_THRESHOLD_START) {
                                if (!isNodding) {
                                    isNodding = true; // 开始点头
                                    noddingDirectionDown = deltaY > 0; // 检测点头方向
                                }
                            } else if (Math.abs(deltaY) < NOD_THRESHOLD_END) {
                                if (isNodding) {
                                    // 检查点头方向是否发生变化，表示完成了一次完整的点头动作
                                    if (noddingDirectionDown != (deltaY > 0)) {
                                        isNodding = false; // 结束点头
                                        if ((checkNodding += 1) == 2) {
                                            nodCount++; // 累加摇头次数
                                            checkNodding = 0;
                                            // 更新點頭和搖頭次數顯示
                                            runOnUiThread(() -> tvPoseInfo_2.setText("點頭次數: " + nodCount + ", 搖頭次數: " + shakeCount));
                                        }
                                        Log.d("PoseDetection", "點頭次数: " + nodCount);
                                    }
                                }
                            }
                            // 檢測搖頭（头部左右运动）
                            if (Math.abs(deltaX) > SHAKE_THRESHOLD_START) {
                                if (!isShaking) {
                                    isShaking = true; // 开始摇头
                                    ShakingDirectionLeft = deltaX > 0;// 检测搖頭方向
                                }
                            } else if (Math.abs(deltaX) < SHAKE_THRESHOLD_END) {
                                if (isShaking) {
                                    if (ShakingDirectionLeft != (deltaX > 0)) {
                                        isShaking = false; // 结束摇头
                                        if ((checkShaking += 1) == 2) {
                                            shakeCount++; // 累加摇头次数
                                            checkShaking = 0;
                                            runOnUiThread(() -> tvPoseInfo_2.setText("點頭次數: " + nodCount + ", 搖頭次數: " + shakeCount));
                                        }
                                        Log.d("PoseDetection", "摇头次数: " + shakeCount);
                                    }
                                }
                            }
                        }
                        // 更新平均鼻子的位置以供下一帧使用
                        lastAvgNosePosition = avgNosePosition;
                    }else {
                        runOnUiThread(() -> tvPoseInfo.setText("無法偵測到鼻子"  ));
                    }
                }else {
                    hasSpokenDetecting = false;
                }
            }
            // 从 ImageProxy 提取 Bitmap
            ByteBuffer byteBuffer = Objects.requireNonNull(imageProxy.getImage()).getPlanes()[0].getBuffer();
            byteBuffer.rewind();
            Bitmap bitmap = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(byteBuffer);

            // 对 Bitmap 进行旋转和镜像
            Matrix matrix = new Matrix();
            if (isFrontCamera()) {
                // 前置摄像头，旋转270度并进行水平镜像
                matrix.postRotate(270);
                matrix.postScale(-1, 1);  // 水平镜像
            } else {
                // 后置摄像头，旋转180度
                matrix.postRotate(90);
            }
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, imageProxy.getWidth(), imageProxy.getHeight(), matrix, false);

            // 添加到列表并绘制姿态
            bitmapArrayList.add(rotatedBitmap);
            // 绘制线条的辅助方法
            if (!poseArrayList.isEmpty()) {
                Pose pose = poseArrayList.get(0);
                canvas = new Canvas(bitmapArrayList.get(0));

                PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
                PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
                PoseLandmark leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
                PoseLandmark rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);
                PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
                PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
                PoseLandmark leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
                PoseLandmark rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);
                PoseLandmark leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE);
                PoseLandmark rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE);
                PoseLandmark leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE);
                PoseLandmark rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE);
                PoseLandmark leftPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY);
                PoseLandmark rightPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY);
                PoseLandmark leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX);
                PoseLandmark rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX);
                PoseLandmark leftThumb = pose.getPoseLandmark(PoseLandmark.LEFT_THUMB);
                PoseLandmark rightThumb = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB);
                PoseLandmark leftHeel = pose.getPoseLandmark(PoseLandmark.LEFT_HEEL);
                PoseLandmark rightHeel = pose.getPoseLandmark(PoseLandmark.RIGHT_HEEL);
                PoseLandmark leftFootIndex = pose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX);
                PoseLandmark rightFootIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX);
                PoseLandmark nose = pose.getPoseLandmark(PoseLandmark.NOSE);
                PoseLandmark leftEyeInner = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_INNER);
                PoseLandmark leftEye = pose.getPoseLandmark(PoseLandmark.LEFT_EYE);
                PoseLandmark leftEyeOuter = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_OUTER);
                PoseLandmark rightEyeInner = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_INNER);
                PoseLandmark rightEye = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE);
                PoseLandmark rightEyeOuter = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_OUTER);
                PoseLandmark leftEar = pose.getPoseLandmark(PoseLandmark.LEFT_EAR);
                PoseLandmark rightEar = pose.getPoseLandmark(PoseLandmark.RIGHT_EAR);
                PoseLandmark leftMouth = pose.getPoseLandmark(PoseLandmark.LEFT_MOUTH);
                PoseLandmark rightMouth = pose.getPoseLandmark(PoseLandmark.RIGHT_MOUTH);

                drawLine(canvas, leftShoulder, rightShoulder, Color.RED); // 肩膀连线
                drawLine(canvas, leftShoulder, leftElbow, Color.GREEN); // 左臂
                drawLine(canvas, leftElbow, leftWrist, Color.GREEN);
                drawLine(canvas, rightShoulder, rightElbow, Color.BLUE); // 右臂
                drawLine(canvas, rightElbow, rightWrist, Color.BLUE);
                drawLine(canvas, leftShoulder, leftHip, Color.BLACK); // 躯干
                drawLine(canvas, rightShoulder, rightHip, Color.BLACK);
                drawLine(canvas, leftHip, rightHip, Color.BLACK);
                drawLine(canvas, leftHip, leftKnee, Color.GRAY); // 左腿
                drawLine(canvas, leftKnee, leftAnkle, Color.GRAY);
                drawLine(canvas, rightHip, rightKnee, Color.GRAY); // 右腿
                drawLine(canvas, rightKnee, rightAnkle, Color.GRAY);
                drawLine(canvas, leftWrist, leftThumb, Color.RED); // 左手的拇指
                drawLine(canvas, leftWrist, leftPinky, Color.RED); // 左手的小指
                drawLine(canvas, leftWrist, leftIndex, Color.RED); // 左手的食指
                drawLine(canvas, rightWrist, rightThumb, Color.BLUE); // 右手的拇指
                drawLine(canvas, rightWrist, rightPinky, Color.BLUE); // 右手的小指
                drawLine(canvas, leftIndex, leftPinky, Color.BLUE);
                drawLine(canvas, rightWrist, rightIndex, Color.RED); // 右手的食指
                drawLine(canvas, leftAnkle, leftHeel, Color.GRAY); // 左脚的脚跟
                drawLine(canvas, leftHeel, leftFootIndex, Color.GRAY); // 左脚的脚尖
                drawLine(canvas, leftAnkle, leftFootIndex, Color.GRAY);
                drawLine(canvas, rightAnkle, rightHeel, Color.GRAY); // 右脚的脚跟
                drawLine(canvas, rightHeel, rightFootIndex, Color.GRAY); // 右脚的脚尖
                drawLine(canvas, rightAnkle, rightFootIndex, Color.GRAY);
                drawLine(canvas, nose, leftEyeInner, Color.RED); // 鼻子到左眼内侧
                drawLine(canvas, leftEyeInner, leftEye, Color.RED); // 左眼内侧到左眼
                drawLine(canvas, leftEye, leftEyeOuter, Color.RED); // 左眼到左眼外侧
                drawLine(canvas, nose, rightEyeInner, Color.RED); // 鼻子到右眼内侧
                drawLine(canvas, rightEyeInner, rightEye, Color.RED); // 右眼内侧到右眼
                drawLine(canvas, rightEye, rightEyeOuter, Color.RED); // 右眼到右眼外侧
                drawLine(canvas, leftEyeOuter, leftEar, Color.RED); // 左眼外侧到左耳
                drawLine(canvas, rightEyeOuter, rightEar, Color.RED); // 右眼外侧到右耳
                drawLine(canvas, leftMouth, rightMouth, Color.RED); // 嘴巴
                drawLine(canvas, rightIndex, rightPinky, Color.RED);
                // 绘制所有关键点

                for (PoseLandmark poseLandmark : pose.getAllPoseLandmarks()) {
                    PointF point = poseLandmark.getPosition();
                    canvas.drawCircle(point.x, point.y, 5, mPaint);  // 绘制关键点

                    // 获取关键点的置信度
                    float confidence = poseLandmark.getInFrameLikelihood();
                    // 格式化置信度文本
                    String confidenceText = String.format(Locale.US, "%.2f", confidence);

                    // 设置文本大小和颜色
                    mPaint.setTextSize(15);  // 调整大小为合适值
                    mPaint.setColor(Color.BLACK);  // 明显颜色

                    // 计算文本的宽度和高度，以便定位
                    float textWidth = mPaint.measureText(confidenceText);
                    float textHeight = mPaint.getTextSize();

                    // 根据关键点位置动态调整文本位置
                    float textX = point.x + 10; // 水平偏移
                    float textY = point.y - textHeight / 2 - 5; // 垂直偏移，使文本垂直居中于点

                    // 绘制文本背景
                    mPaint.setStyle(Paint.Style.FILL);
                    canvas.drawRect(textX - 5, textY - textHeight, textX + textWidth + 5, textY + 5, mPaint);

                    // 绘制文本
                    mPaint.setColor(Color.WHITE);
                    canvas.drawText(confidenceText, textX, textY, mPaint);
                }

                // 清理和重置逻辑
                bitmap4DisplayArrayList.clear();
                bitmap4DisplayArrayList.add(bitmapArrayList.get(0));
                bitmap4Save = bitmapArrayList.get(bitmapArrayList.size() - 1);
                bitmapArrayList.clear();
                bitmapArrayList.add(bitmap4Save);
                poseArrayList.clear();
                isRunning = false;
            }

            // 顯示 Bitmap
            if (!bitmap4DisplayArrayList.isEmpty()) {
                display.getBitmap(bitmap4DisplayArrayList.get(0));
            }
            imageProxy.close();
            bitmap.recycle(); // 回收原始 bitmap

            // 如果列表中的 Bitmap 数量超过了阈值，移除并回收最旧的 Bitmap
            if (bitmapArrayList.size() > SOME_THRESHOLD) {
                Bitmap oldBitmap = bitmapArrayList.remove(0);
                oldBitmap.recycle();
            }
            // 如果姿態檢查未在運行，則啟動
            if (!isRunning) {
                isRunning = true;
                RunMlkit.run();
            }
            // 更新TextView顯示
//            runOnUiThread(() -> tvPoseInfo.setText("點頭次數: " + nodCount + ", 搖頭次數: " + shakeCount));

        });
        return imageAnalysis;
    }

    private void drawLine(Canvas canvas, PoseLandmark start, PoseLandmark end, int color) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(5);
        if (start != null && end != null) {
            PointF startPointF = start.getPosition();
            PointF endPointF = end.getPosition();
            canvas.drawLine(startPointF.x, startPointF.y, endPointF.x, endPointF.y, paint);
        }
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        // 解绑之前的所有摄像头
        cameraProvider.unbindAll();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        // 如果预览不为空，设置其旋转方向
        if (preview != null) {
            preview.setTargetRotation(previewView.getDisplay().getRotation());
        }

        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);

        // 尝试绑定摄像头
        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e("CameraX", "Error binding camera", e);
        }
    }

    // 获取应用请求的所有权限
    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }
    // 检查所有必要权限是否已授予
    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (isPermissionNotGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }
    // 检查单个权限是否未被授予
    private static boolean isPermissionNotGranted(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED;
    }

    // 如果需要，请求运行时权限
    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (isPermissionNotGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }
        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }
}