package com.example.heimdall.project.application.usecase;

import com.example.heimdall.project.domain.exception.DuplicateProjectCodeException;
import com.example.heimdall.project.domain.exception.ProjectAccessDeniedException;
import com.example.heimdall.project.domain.exception.ProjectNotFoundException;
import com.example.heimdall.project.domain.model.Coordinates;
import com.example.heimdall.project.domain.model.MonitoringSchedule;
import com.example.heimdall.project.domain.model.NotificationPolicy;
import com.example.heimdall.project.domain.model.Project;
import com.example.heimdall.project.domain.model.ProjectCode;
import com.example.heimdall.project.domain.model.ProjectId;
import com.example.heimdall.project.domain.model.ProjectStatus;
import com.example.heimdall.project.domain.port.in.ChangeProjectStatusUseCase;
import com.example.heimdall.project.domain.port.in.CreateProjectUseCase;
import com.example.heimdall.project.domain.port.in.GetProjectUseCase;
import com.example.heimdall.project.domain.port.in.ListProjectsUseCase;
import com.example.heimdall.project.domain.port.in.UpdateProjectUseCase;
import com.example.heimdall.project.domain.port.out.GeoValidationPort;
import com.example.heimdall.project.domain.port.out.ProjectRepositoryPort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ProjectManagementService implements
        CreateProjectUseCase,
        GetProjectUseCase,
        ListProjectsUseCase,
        UpdateProjectUseCase,
        ChangeProjectStatusUseCase {

    private final ProjectRepositoryPort projectRepository;
    private final GeoValidationPort geoValidation;
    private final Clock clock;

    public ProjectManagementService(ProjectRepositoryPort projectRepository,
                                  GeoValidationPort geoValidation,
                                  Clock clock) {
        this.projectRepository = Objects.requireNonNull(projectRepository);
        this.geoValidation = Objects.requireNonNull(geoValidation);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Project createProject(
            UUID ownerId,
            ProjectCode code,
            String name,
            String countryCode,
            String municipalityCode,
            Coordinates coordinates,
            ZoneId timezone,
            MonitoringSchedule schedule,
            NotificationPolicy notificationPolicy
    ) {
        Objects.requireNonNull(ownerId, "Owner ID is required");
        validateMunicipality(municipalityCode);
        validateUniqueCode(ownerId, code, null);

        Instant now = clock.instant();
        Project project = new Project(
                ProjectId.random(),
                ownerId,
                code,
                name,
                countryCode,
                municipalityCode,
                coordinates,
                timezone,
                schedule,
                notificationPolicy,
                ProjectStatus.ACTIVE,
                now,
                now
        );
        return projectRepository.save(project);
    }

    @Override
    public Project getProject(ProjectId id, UUID requesterUserId, boolean isAdmin) {
        Objects.requireNonNull(id, "Project ID is required");
        Objects.requireNonNull(requesterUserId, "Requester User ID is required");

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));

        validateOwnership(project, requesterUserId, isAdmin);
        return project;
    }

    @Override
    public List<Project> listProjectsByOwner(UUID ownerId) {
        Objects.requireNonNull(ownerId, "Owner ID is required");
        return projectRepository.findByOwnerId(ownerId);
    }

    @Override
    public Project updateProject(
            ProjectId id,
            UUID requesterUserId,
            boolean isAdmin,
            ProjectCode code,
            String name,
            String municipalityCode,
            Coordinates coordinates,
            ZoneId timezone,
            MonitoringSchedule schedule,
            NotificationPolicy notificationPolicy
    ) {
        Project existing = getProject(id, requesterUserId, isAdmin);
        validateMunicipality(municipalityCode);
        validateUniqueCode(existing.ownerId(), code, existing.id());

        Instant now = clock.instant();
        Project updated = existing
                .updateDetails(code, name, municipalityCode, coordinates, timezone, now)
                .updateSchedule(schedule, now)
                .updateNotificationPolicy(notificationPolicy, now);

        return projectRepository.save(updated);
    }

    @Override
    public void changeStatus(ProjectId id, ProjectStatus newStatus, UUID requesterUserId, boolean isAdmin) {
        Project existing = getProject(id, requesterUserId, isAdmin);
        Instant now = clock.instant();
        Project updated = existing.changeStatus(newStatus, now);
        projectRepository.save(updated);
    }

    private void validateOwnership(Project project, UUID requesterUserId, boolean isAdmin) {
        if (!isAdmin && !project.ownerId().equals(requesterUserId)) {
            throw new ProjectAccessDeniedException(project.id(), requesterUserId);
        }
    }

    private void validateMunicipality(String municipalityCode) {
        if (municipalityCode != null && !municipalityCode.isBlank()) {
            if (!geoValidation.isValidMunicipality(municipalityCode)) {
                throw new IllegalArgumentException("Municipality code is invalid or does not exist: " + municipalityCode);
            }
        }
    }

    private void validateUniqueCode(UUID ownerId, ProjectCode code, ProjectId currentProjectId) {
        if (code != null) {
            boolean exists = projectRepository.existsByOwnerIdAndCode(ownerId, code);
            if (exists) {
                // If updating same project with same code, it's allowed
                if (currentProjectId != null) {
                    var current = projectRepository.findById(currentProjectId);
                    if (current.isPresent() && code.equals(current.get().code())) {
                        return;
                    }
                }
                throw new DuplicateProjectCodeException(code);
            }
        }
    }
}
