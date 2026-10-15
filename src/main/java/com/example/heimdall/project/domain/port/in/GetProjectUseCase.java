package com.example.heimdall.project.domain.port.in;

import com.example.heimdall.project.domain.model.Project;
import com.example.heimdall.project.domain.model.ProjectId;
import java.util.UUID;

public interface GetProjectUseCase {
    Project getProject(ProjectId id, UUID requesterUserId, boolean isAdmin);
}
