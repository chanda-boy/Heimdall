package com.example.heimdall.geo.domain.model;

import java.util.Objects;

public record Municipality(MunicipalityCode code, DepartmentCode departmentCode, String name) {
    public Municipality {
        Objects.requireNonNull(code, "Municipality code is required");
        Objects.requireNonNull(departmentCode, "Department code is required");
        Objects.requireNonNull(name, "Municipality name is required");
        name = name.strip();
        if (name.isBlank()) {
            throw new IllegalArgumentException("Municipality name cannot be blank");
        }
    }
}
