package com.example.code.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/query")
    Call<ApiResponse> sendQuery(@Body HealthEducationRequest queryRequest);

    @POST("/query")
    Call<ApiResponse> sendQuery(@Body NostalgicRequest queryRequest);
}
