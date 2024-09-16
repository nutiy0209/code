package com.example.code;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;


public interface ApiService {
    @POST("/query")
    Call<ApiResponse> sendQuery(@Body QueryRequest2 queryRequest);
    Call<ApiResponse> sendQuery(@Body QueryRequest queryRequest);
}



