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
import com.example.heimdall.project.domain.port.out.GeoValidationPort;
import com.example.heimdall.project.domain.port.out.ProjectRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectManagementServiceTest {

    private ProjectRepositoryPort projectRepository;
    private GeoValidationPort geoValidation;
    private Clock clock;
    private ProjectManagementService service;

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepositoryPort.class);
        geoValidation = mock(GeoValidationPort.class);
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new ProjectManagementService(projectRepository, geoValidation, clock);

        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createProjectValidatesMunicipalityAndUniqueness() {
        when(geoValidation.isValidMunicipality("05113")).thenReturn(true);
        when(projectRepository.existsByOwnerIdAndCode(OWNER_ID, ProjectCode.of("BUR-01"))).thenReturn(false);

        Project created = service.createProject(
                OWNER_ID,
                ProjectCode.of("BUR-01"),
                "Buriticá Gold Mine",
                "CO",
                "05113",
                Coordinates.of(6.9167, -75.9167),
                ZoneId.of("America/Bogota"),
                MonitoringSchedule.defaultSchedule(),
                NotificationPolicy.defaultPolicy()
        );

        assertNotNull(created);
        assertEquals(OWNER_ID, created.ownerId());
        assertEquals("BUR-01", created.code().value());
        assertEquals(ProjectStatus.ACTIVE, created.status());
        verify(geoValidation).isValidMunicipality("05113");
        verify(projectRepository).save(any());
    }

    @Test
    void createProjectRejectsInvalidMunicipality() {
        when(geoValidation.isValidMunicipality("99999")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.createProject(
                OWNER_ID,
                ProjectCode.of("BUR-01"),
                "Buriticá Gold Mine",
                "CO",
                "99999",
                Coordinates.of(6.9167, -75.9167),
                ZoneId.of("America/Bogota"),
                MonitoringSchedule.defaultSchedule(),
                NotificationPolicy.defaultPolicy()
        ));
    }

    @Test
    void createProjectRejectsDuplicateCodeForSameOwner() {
        when(projectRepository.existsByOwnerIdAndCode(OWNER_ID, ProjectCode.of("BUR-01"))).thenReturn(true);

        assertThrows(DuplicateProjectCodeException.class, () -> service.createProject(
                OWNER_ID,
                ProjectCode.of("BUR-01"),
                "Duplicate Code Mine",
                "CO",
                null,
                Coordinates.of(6.9167, -75.9167),
                ZoneId.of("America/Bogota"),
                MonitoringSchedule.defaultSchedule(),
                NotificationPolicy.defaultPolicy()
        ));
    }

    @Test
    void getProjectEnforcesMultiTenancy() {
        ProjectId id = ProjectId.random();
        Project project = new Project(id, OWNER_ID, null, "Mine", "CO", null,
                Coordinates.of(6.0, -75.0), ZoneId.of("America/Bogota"),
                MonitoringSchedule.defaultSchedule(), NotificationPolicy.defaultPolicy(),
                ProjectStatus.ACTIVE, NOW, NOW);

        when(projectRepository.findById(id)).thenReturn(Optional.of(project));

        // Owner can read
        assertSame(project, service.getProject(id, OWNER_ID, false));

        // Admin can read
        assertSame(project, service.getProject(id, OTHER_USER_ID, true));

        // Other user cannot read
        assertThrows(ProjectAccessDeniedException.class, () -> service.getProject(id, OTHER_USER_ID, false));

        // Non existent project throws 404 exception
        ProjectId missingId = ProjectId.random();
        when(projectRepository.findById(missingId)).thenReturn(Optional.empty());
        assertThrows(ProjectNotFoundException.class, () -> service.getProject(missingId, OWNER_ID, false));
    }
}
