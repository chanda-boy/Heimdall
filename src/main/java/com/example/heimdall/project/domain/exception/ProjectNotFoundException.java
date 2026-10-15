package com.example.heimdall.project.domain.exception;

import com.example.heimdall.project.domain.model.ProjectId;

public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException(ProjectId id) {
        super("Project not found with id: " + id.value());
    }
}
