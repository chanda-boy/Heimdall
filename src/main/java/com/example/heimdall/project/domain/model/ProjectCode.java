package com.example.heimdall.project.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record ProjectCode(String value) {
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,50}$");

    public ProjectCode {
        if (value != null) {
            value = value.strip().toUpperCase(Locale.ROOT);
            if (value.isBlank()) {
                value = null;
            } else if (!CODE_PATTERN.matcher(value).matches()) {
                throw new IllegalArgumentException("Project code must be alphanumeric with hyphens/underscores (max 50 chars): " + value);
            }
        }
    }

    public static ProjectCode of(String value) {
        return value == null || value.isBlank() ? null : new ProjectCode(value);
    }
}
