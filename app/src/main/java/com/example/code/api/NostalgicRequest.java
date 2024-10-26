package com.example.code.api;

public class NostalgicRequest {
    private String question;
    private String phoneModel;

    // 建構函式，接收問題和手機型號
    public NostalgicRequest(String question, String phoneModel) {
        this.question = question;
        this.phoneModel = phoneModel;
    }

    // 獲取問題
    public String getQuestion() {
        return question;
    }

    // 設置問題git add .
    public void setQuestion(String question) {
        this.question = question;
    }

    // 獲取手機型號
    public String getPhoneModel() {
        return phoneModel;
    }

    // 設置手機型號
    public void setPhoneModel(String phoneModel) {
        this.phoneModel = phoneModel;
    }
}
