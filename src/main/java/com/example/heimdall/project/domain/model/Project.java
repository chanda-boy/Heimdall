package com.example.heimdall.project.domain.model;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

public record Project(
        ProjectId id,
        UUID ownerId,
        ProjectCode code,
        String name,
        String countryCode,
        String municipalityCode,
        Coordinates coordinates,
        ZoneId timezone,
        MonitoringSchedule schedule,
        NotificationPolicy notificationPolicy,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public Project {
        Objects.requireNonNull(id, "Project ID is required");
        Objects.requireNonNull(ownerId, "Owner ID is required");
        Objects.requireNonNull(name, "Project name is required");
        Objects.requireNonNull(countryCode, "Country code is required");
        Objects.requireNonNull(coordinates, "Coordinates are required");
        Objects.requireNonNull(timezone, "Timezone is required");
        Objects.requireNonNull(schedule, "Monitoring schedule is required");
        Objects.requireNonNull(notificationPolicy, "Notification policy is required");
        Objects.requireNonNull(status, "Project status is required");

        name = name.strip();
        if (name.isBlank()) {
            throw new IllegalArgumentException("Project name cannot be blank");
        }
        countryCode = countryCode.strip().toUpperCase(java.util.Locale.ROOT);
        if (municipalityCode != null) {
            municipalityCode = municipalityCode.strip();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    public Project updateDetails(ProjectCode newCode, String newName, String newMunicipalityCode,
                                 Coordinates newCoordinates, ZoneId newTimezone, Instant now) {
        if (status == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot modify an archived project");
        }
        return new Project(id, ownerId, newCode, newName, countryCode, newMunicipalityCode,
                newCoordinates, newTimezone, schedule, notificationPolicy, status, createdAt, now);
    }

    public Project updateSchedule(MonitoringSchedule newSchedule, Instant now) {
        if (status == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot modify schedule of an archived project");
        }
        return new Project(id, ownerId, code, name, countryCode, municipalityCode,
                coordinates, timezone, newSchedule, notificationPolicy, status, createdAt, now);
    }

    public Project updateNotificationPolicy(NotificationPolicy newPolicy, Instant now) {
        if (status == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot modify notification policy of an archived project");
        }
        return new Project(id, ownerId, code, name, countryCode, municipalityCode,
                coordinates, timezone, schedule, newPolicy, status, createdAt, now);
    }

    public Project changeStatus(ProjectStatus newStatus, Instant now) {
        return new Project(id, ownerId, code, name, countryCode, municipalityCode,
                coordinates, timezone, schedule, notificationPolicy, newStatus, createdAt, now);
    }
}
