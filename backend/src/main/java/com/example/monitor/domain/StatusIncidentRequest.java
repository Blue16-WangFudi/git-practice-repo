package com.example.monitor.domain;

import javax.validation.constraints.NotBlank;

public class StatusIncidentRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String impact;

    @NotBlank
    private String message;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
