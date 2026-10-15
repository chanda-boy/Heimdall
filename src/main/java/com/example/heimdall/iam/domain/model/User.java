package com.example.heimdall.iam.domain.model;

import java.util.Objects;

public record User(UserId id, Email email, String passwordHash, Role role) {
    public User {
        Objects.requireNonNull(id, "User ID is required");
        Objects.requireNonNull(email, "Email is required");
        Objects.requireNonNull(role, "Role is required");
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash is required");
        }
    }

    @Override
    public String toString() {
        return "User[id=" + id + ", email=" + email + ", role=" + role + "]";
    }
}
