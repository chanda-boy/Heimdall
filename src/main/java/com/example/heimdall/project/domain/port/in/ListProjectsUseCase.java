package com.example.heimdall.project.domain.port.in;

import com.example.heimdall.project.domain.model.Project;
import java.util.List;
import java.util.UUID;

public interface ListProjectsUseCase {
    List<Project> listProjectsByOwner(UUID ownerId);
}
