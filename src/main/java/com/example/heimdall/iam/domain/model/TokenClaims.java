package com.example.heimdall.iam.domain.model;

import java.util.Objects;

public record TokenClaims(UserId userId, Email email, Role role) {
    public TokenClaims {
        Objects.requireNonNull(userId, "User ID is required");
        Objects.requireNonNull(email, "Email is required");
        Objects.requireNonNull(role, "Role is required");
    }
}
