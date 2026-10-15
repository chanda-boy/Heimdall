package com.example.heimdall.project.domain.exception;

import com.example.heimdall.project.domain.model.ProjectId;
import java.util.UUID;

public class ProjectAccessDeniedException extends RuntimeException {
    public ProjectAccessDeniedException(ProjectId projectId, UUID userId) {
        super("User " + userId + " does not have access to project " + projectId.value());
    }
}
