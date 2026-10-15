package com.example.heimdall.project.presentation.dto.request;

import com.example.heimdall.project.domain.model.AlertLevel;
import com.example.heimdall.project.domain.model.Coordinates;
import com.example.heimdall.project.domain.model.CoordinatesSource;
import com.example.heimdall.project.domain.model.MonitoringSchedule;
import com.example.heimdall.project.domain.model.NotificationPolicy;
import com.example.heimdall.project.domain.model.ProjectCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.time.ZoneId;

public record UpdateProjectRequestDto(
        @NotBlank(message = "El nombre del proyecto es obligatorio")
        String name,
        String code,
        String municipalityCode,
        @NotNull(message = "La latitud es obligatoria")
        Double latitude,
        @NotNull(message = "La longitud es obligatoria")
        Double longitude,
        CoordinatesSource coordinatesSource,
        String timezone,
        LocalTime dailyRunTime,
        Boolean scheduleEnabled,
        Integer notifyLeadDays,
        AlertLevel notifyMinLevel
) {
    public ProjectCode toProjectCode() {
        return ProjectCode.of(code);
    }

    public Coordinates toCoordinates() {
        return new Coordinates(
                latitude,
                longitude,
                coordinatesSource != null ? coordinatesSource : CoordinatesSource.GEOCODED
        );
    }

    public ZoneId toTimezone() {
        return timezone != null && !timezone.isBlank() ? ZoneId.of(timezone) : ZoneId.of("America/Bogota");
    }

    public MonitoringSchedule toSchedule() {
        LocalTime time = dailyRunTime != null ? dailyRunTime : LocalTime.of(6, 0);
        boolean enabled = scheduleEnabled == null || scheduleEnabled;
        return new MonitoringSchedule(time, enabled);
    }

    public NotificationPolicy toNotificationPolicy() {
        int leadDays = notifyLeadDays != null ? notifyLeadDays : 1;
        AlertLevel minLevel = notifyMinLevel != null ? notifyMinLevel : AlertLevel.ORANGE;
        return new NotificationPolicy(leadDays, minLevel);
    }
}
