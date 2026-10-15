package com.example.heimdall.project.presentation.rest;

import com.example.heimdall.project.application.usecase.ProjectManagementService;
import com.example.heimdall.project.domain.model.Project;
import com.example.heimdall.project.domain.model.ProjectId;
import com.example.heimdall.project.presentation.dto.request.ChangeStatusRequestDto;
import com.example.heimdall.project.presentation.dto.request.CreateProjectRequestDto;
import com.example.heimdall.project.presentation.dto.request.UpdateProjectRequestDto;
import com.example.heimdall.project.presentation.dto.response.ProjectResponseDto;
import com.example.heimdall.project.presentation.security.SecurityUserContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectManagementService projectService;

    public ProjectController(ProjectManagementService projectService) {
        this.projectService = Objects.requireNonNull(projectService);
    }

    @PostMapping
    public ResponseEntity<ProjectResponseDto> createProject(@Valid @RequestBody CreateProjectRequestDto request) {
        SecurityUserContext user = SecurityUserContext.current();
        Project created = projectService.createProject(
                user.userId(),
                request.toProjectCode(),
                request.name(),
                request.resolveCountryCode(),
                request.municipalityCode(),
                request.toCoordinates(),
                request.toTimezone(),
                request.toSchedule(),
                request.toNotificationPolicy()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponseDto.from(created));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponseDto>> listProjects() {
        SecurityUserContext user = SecurityUserContext.current();
        List<ProjectResponseDto> projects = projectService.listProjectsByOwner(user.userId())
                .stream()
                .map(ProjectResponseDto::from)
                .toList();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDto> getProject(@PathVariable UUID id) {
        SecurityUserContext user = SecurityUserContext.current();
        Project project = projectService.getProject(new ProjectId(id), user.userId(), user.isAdmin());
        return ResponseEntity.ok(ProjectResponseDto.from(project));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponseDto> updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequestDto request
    ) {
        SecurityUserContext user = SecurityUserContext.current();
        Project updated = projectService.updateProject(
                new ProjectId(id),
                user.userId(),
                user.isAdmin(),
                request.toProjectCode(),
                request.name(),
                request.municipalityCode(),
                request.toCoordinates(),
                request.toTimezone(),
                request.toSchedule(),
                request.toNotificationPolicy()
        );
        return ResponseEntity.ok(ProjectResponseDto.from(updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeStatusRequestDto request
    ) {
        SecurityUserContext user = SecurityUserContext.current();
        projectService.changeStatus(new ProjectId(id), request.status(), user.userId(), user.isAdmin());
        return ResponseEntity.noContent().build();
    }
}
