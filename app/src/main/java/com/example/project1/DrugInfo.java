package com.example.project1;

public class DrugInfo {
    private String name;
    private String details;

    public DrugInfo() {
        // Firestore를 위한 기본 생성자
    }

    public DrugInfo(String name, String details) {
        this.name = name;
        this.details = details;
    }

    public String getName() {
        return name;
    }

    public String getDetails() {
        return details;
    }
}

