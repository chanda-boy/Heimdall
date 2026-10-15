package com.example.heimdall.iam.application.dto;

public record LoginCommand(String email, String password) {
    @Override
    public String toString() {
        return "LoginCommand[email=" + email + "]";
    }
}
