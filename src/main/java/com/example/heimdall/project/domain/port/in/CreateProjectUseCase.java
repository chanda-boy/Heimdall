package com.example.heimdall.project.domain.port.in;

import com.example.heimdall.project.domain.model.Coordinates;
import com.example.heimdall.project.domain.model.MonitoringSchedule;
import com.example.heimdall.project.domain.model.NotificationPolicy;
import com.example.heimdall.project.domain.model.Project;
import com.example.heimdall.project.domain.model.ProjectCode;
import java.time.ZoneId;
import java.util.UUID;

public interface CreateProjectUseCase {
    Project createProject(
            UUID ownerId,
            ProjectCode code,
            String name,
            String countryCode,
            String municipalityCode,
            Coordinates coordinates,
            ZoneId timezone,
            MonitoringSchedule schedule,
            NotificationPolicy notificationPolicy
    );
}
