package com.example.heimdall.geo.domain.exception;

import com.example.heimdall.geo.domain.model.DepartmentCode;

public class DepartmentNotFoundException extends RuntimeException {
    public DepartmentNotFoundException(DepartmentCode code) {
        super("Department not found with code: " + code.value());
    }
}
