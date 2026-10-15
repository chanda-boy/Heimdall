package com.example.heimdall.project.domain.exception;

import com.example.heimdall.project.domain.model.ProjectCode;

public class DuplicateProjectCodeException extends RuntimeException {
    public DuplicateProjectCodeException(ProjectCode code) {
        super("A project with code " + code.value() + " already exists for this owner");
    }
}
