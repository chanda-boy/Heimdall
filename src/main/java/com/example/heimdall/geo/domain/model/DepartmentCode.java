package com.example.heimdall.geo.domain.model;

import com.example.heimdall.geo.domain.exception.InvalidGeoCodeException;
import java.util.Objects;
import java.util.regex.Pattern;

public record DepartmentCode(String value) {
    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{2}$");

    public DepartmentCode {
        Objects.requireNonNull(value, "Department code is required");
        value = value.strip();
        if (!CODE_PATTERN.matcher(value).matches()) {
            throw new InvalidGeoCodeException("Department code must be exactly 2 digits: " + value);
        }
    }
}
