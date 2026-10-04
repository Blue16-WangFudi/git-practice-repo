package com.example.monitor.domain;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

public class StatusSubscriptionRequest {

    @NotBlank
    @Email
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
