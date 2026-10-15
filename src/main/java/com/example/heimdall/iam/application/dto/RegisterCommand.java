package com.example.heimdall.iam.application.dto;

public record RegisterCommand(String email, String password) {
    @Override
    public String toString() {
        return "RegisterCommand[email=" + email + "]";
    }
}
