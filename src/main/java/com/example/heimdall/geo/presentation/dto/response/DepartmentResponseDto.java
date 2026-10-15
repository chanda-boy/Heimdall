package com.example.heimdall.geo.presentation.dto.response;

import com.example.heimdall.geo.domain.model.Department;

public record DepartmentResponseDto(String code, String countryCode, String name) {
    public static DepartmentResponseDto from(Department department) {
        return new DepartmentResponseDto(
                department.code().value(),
                department.countryCode(),
                department.name()
        );
    }
}
