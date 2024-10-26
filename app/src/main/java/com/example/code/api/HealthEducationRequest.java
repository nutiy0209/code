package com.example.code.api;

public class HealthEducationRequest {
    private String question;
    private String mainCategory;
    private String subCategory;

    public HealthEducationRequest(String question, String mainCategory, String subCategory) {
        this.question = question;
        this.mainCategory = mainCategory;
        this.subCategory = subCategory;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getMainCategory() {
        return mainCategory;
    }

    public void setMainCategory(String mainCategory) {
        this.mainCategory = mainCategory;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }
}

