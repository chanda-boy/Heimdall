package com.example.heimdall.geo.domain.model;

import java.util.Objects;

public record Department(DepartmentCode code, String countryCode, String name) {
    public Department {
        Objects.requireNonNull(code, "Department code is required");
        Objects.requireNonNull(countryCode, "Country code is required");
        Objects.requireNonNull(name, "Department name is required");
        countryCode = countryCode.strip().toUpperCase(java.util.Locale.ROOT);
        name = name.strip();
        if (name.isBlank()) {
            throw new IllegalArgumentException("Department name cannot be blank");
        }
    }
}
