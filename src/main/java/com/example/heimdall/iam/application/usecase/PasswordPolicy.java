package com.example.heimdall.iam.application.usecase;

import java.nio.charset.StandardCharsets;

/** BCrypt processes at most 72 bytes; reject input that would be silently truncated. */
final class PasswordPolicy {
    private PasswordPolicy() {
    }

    static void validate(String password) {
        if (password == null || password.isBlank() || password.codePointCount(0, password.length()) < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must contain at least 8 characters and at most 72 UTF-8 bytes");
        }
    }
}
