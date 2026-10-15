package com.example.heimdall.geo.domain.model;

import com.example.heimdall.geo.domain.exception.InvalidGeoCodeException;
import java.util.Objects;
import java.util.regex.Pattern;

public record MunicipalityCode(String value) {
    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{5}$");

    public MunicipalityCode {
        Objects.requireNonNull(value, "Municipality code is required");
        value = value.strip();
        if (!CODE_PATTERN.matcher(value).matches()) {
            throw new InvalidGeoCodeException("Municipality code must be exactly 5 digits: " + value);
        }
    }

    public DepartmentCode departmentCode() {
        return new DepartmentCode(value.substring(0, 2));
    }
}
