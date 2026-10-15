package com.example.heimdall.project.domain.port.out;

import com.example.heimdall.project.domain.model.Project;
import com.example.heimdall.project.domain.model.ProjectCode;
import com.example.heimdall.project.domain.model.ProjectId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepositoryPort {
    Project save(Project project);
    Optional<Project> findById(ProjectId id);
    List<Project> findByOwnerId(UUID ownerId);
    boolean existsByOwnerIdAndCode(UUID ownerId, ProjectCode code);
    void delete(ProjectId id);
}
