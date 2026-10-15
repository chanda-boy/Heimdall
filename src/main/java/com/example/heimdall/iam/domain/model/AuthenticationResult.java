package com.example.heimdall.iam.domain.model;

import java.util.Objects;

public record AuthenticationResult(String accessToken, User user) {
    public AuthenticationResult {
        Objects.requireNonNull(user, "User is required");
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token is required");
        }
    }

    @Override
    public String toString() {
        return "AuthenticationResult[user=" + user + "]";
    }
}
