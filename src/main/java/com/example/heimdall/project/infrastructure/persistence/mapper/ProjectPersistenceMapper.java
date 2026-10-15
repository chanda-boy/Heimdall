package com.example.heimdall.project.infrastructure.persistence.mapper;

import com.example.heimdall.project.domain.model.AlertLevel;
import com.example.heimdall.project.domain.model.Coordinates;
import com.example.heimdall.project.domain.model.CoordinatesSource;
import com.example.heimdall.project.domain.model.MonitoringSchedule;
import com.example.heimdall.project.domain.model.NotificationPolicy;
import com.example.heimdall.project.domain.model.Project;
import com.example.heimdall.project.domain.model.ProjectCode;
import com.example.heimdall.project.domain.model.ProjectId;
import com.example.heimdall.project.domain.model.ProjectStatus;
import com.example.heimdall.project.infrastructure.persistence.entity.ProjectJpaEntity;
import java.math.BigDecimal;
import java.time.ZoneId;

public final class ProjectPersistenceMapper {

    private ProjectPersistenceMapper() {}

    public static Project toDomain(ProjectJpaEntity entity) {
        if (entity == null) return null;

        return new Project(
                new ProjectId(entity.getId()),
                entity.getOwnerId(),
                ProjectCode.of(entity.getCode()),
                entity.getName(),
                entity.getCountryCode(),
                entity.getMunicipalityCode(),
                new Coordinates(
                        entity.getLatitude().doubleValue(),
                        entity.getLongitude().doubleValue(),
                        CoordinatesSource.valueOf(entity.getCoordinatesSource())
                ),
                ZoneId.of(entity.getTimezone()),
                new MonitoringSchedule(entity.getDailyRunTime(), entity.isScheduleEnabled()),
                new NotificationPolicy(
                        entity.getNotifyLeadDays(),
                        AlertLevel.valueOf(entity.getNotifyMinLevel())
                ),
                ProjectStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static ProjectJpaEntity toEntity(Project project) {
        if (project == null) return null;

        return new ProjectJpaEntity(
                project.id().value(),
                project.ownerId(),
                project.code() != null ? project.code().value() : null,
                project.name(),
                project.countryCode(),
                project.municipalityCode(),
                BigDecimal.valueOf(project.coordinates().latitude()),
                BigDecimal.valueOf(project.coordinates().longitude()),
                project.coordinates().source().name(),
                project.timezone().getId(),
                project.schedule().dailyRunTime(),
                project.schedule().enabled(),
                (short) project.notificationPolicy().notifyLeadDays(),
                project.notificationPolicy().notifyMinLevel().name(),
                project.status().name(),
                project.createdAt(),
                project.updatedAt()
        );
    }
}
