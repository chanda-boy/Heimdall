package com.example.heimdall.iam.domain.model;

import java.util.Locale;
import java.util.regex.Pattern;

public record Email(String value) {
    private static final Pattern FORMAT = Pattern.compile(
            "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@"
                    + "[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$");

    public Email {
        if (value == null) {
            throw new IllegalArgumentException("Email is required");
        }
        value = value.strip().toLowerCase(Locale.ROOT);
        int separator = value.indexOf('@');
        if (value.length() > 254 || separator > 64 || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email address");
        }
        for (String label : value.substring(separator + 1).split("\\.")) {
            if (label.length() > 63) {
                throw new IllegalArgumentException("Invalid email address");
            }
        }
    }
}
