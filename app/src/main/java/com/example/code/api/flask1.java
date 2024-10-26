//package com.example.code;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//import retrofit2.Retrofit;
//import retrofit2.converter.gson.GsonConverterFactory;
//
//public class flask1 extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        // 初始化 Retrofit
//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl("http://192.168.53.111:5000/") // 替换为 Flask 服务器的局域网 IP
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//
//        ApiService apiService = retrofit.create(ApiService.class);
//
//        // 模拟一个输入数据，可以从其他地方获取数据
//        String userQuery = "你好"; // 示例输入数据
//
//        // 构建请求对象
//        QueryRequest request = new QueryRequest(userQuery);
//
//        // 发起网络请求
//        Call<ApiResponse> call = apiService.sendQuery(request);
//
//        // 异步请求，使用回调处理
//        call.enqueue(new Callback<ApiResponse>() {
//            @Override
//            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
//                if (response.isSuccessful()) {
//                    ApiResponse apiResponse = response.body();
//                    if (apiResponse != null) {
//                        // 打印日志或显示在 UI 上
//                        Log.d("API_RESPONSE", "ChatGPT 回應: " + apiResponse.getChatgptReply());
//                        Toast.makeText(flask1.this, "收到响应", Toast.LENGTH_SHORT).show();
//
//                        // 将响应数据传递到 MainActivity6
//                        Intent intent = new Intent(flask1.this, MainActivity6.class);
//                        intent.putExtra("flaskReply", apiResponse.getChatgptReply());
//                        startActivity(intent); // 启动 MainActivity6
//                        finish(); // 结束当前 Activity
//                    }
//                } else {
//                    // 打印错误状态码和错误信息
//                    Log.e("API_ERROR", "Response error: " + response.code() + " " + response.errorBody().toString());
//                }
//            }
//
//            @Override
//            public void onFailure(Call<ApiResponse> call, Throwable t) {
//                // 网络请求失败
//                Log.e("API_ERROR", "Request failed: " + t.getMessage());
//            }
//        });
//    }
//}
