package com.example.heimdall.project.domain.port.in;

import com.example.heimdall.project.domain.model.ProjectId;
import com.example.heimdall.project.domain.model.ProjectStatus;
import java.util.UUID;

public interface ChangeProjectStatusUseCase {
    void changeStatus(ProjectId id, ProjectStatus newStatus, UUID requesterUserId, boolean isAdmin);
}
