package com.example.heimdall.project.presentation.dto.response;

import com.example.heimdall.project.domain.model.AlertLevel;
import com.example.heimdall.project.domain.model.CoordinatesSource;
import com.example.heimdall.project.domain.model.Project;
import com.example.heimdall.project.domain.model.ProjectStatus;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record ProjectResponseDto(
        UUID id,
        UUID ownerId,
        String code,
        String name,
        String countryCode,
        String municipalityCode,
        double latitude,
        double longitude,
        CoordinatesSource coordinatesSource,
        String timezone,
        LocalTime dailyRunTime,
        boolean scheduleEnabled,
        int notifyLeadDays,
        AlertLevel notifyMinLevel,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProjectResponseDto from(Project project) {
        return new ProjectResponseDto(
                project.id().value(),
                project.ownerId(),
                project.code() != null ? project.code().value() : null,
                project.name(),
                project.countryCode(),
                project.municipalityCode(),
                project.coordinates().latitude(),
                project.coordinates().longitude(),
                project.coordinates().source(),
                project.timezone().getId(),
                project.schedule().dailyRunTime(),
                project.schedule().enabled(),
                project.notificationPolicy().notifyLeadDays(),
                project.notificationPolicy().notifyMinLevel(),
                project.status(),
                project.createdAt(),
                project.updatedAt()
        );
    }
}
