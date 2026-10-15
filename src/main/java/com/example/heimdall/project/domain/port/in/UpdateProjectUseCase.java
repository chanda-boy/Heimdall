package com.example.heimdall.project.domain.port.in;

import com.example.heimdall.project.domain.model.Coordinates;
import com.example.heimdall.project.domain.model.MonitoringSchedule;
import com.example.heimdall.project.domain.model.NotificationPolicy;
import com.example.heimdall.project.domain.model.Project;
import com.example.heimdall.project.domain.model.ProjectCode;
import com.example.heimdall.project.domain.model.ProjectId;
import java.time.ZoneId;
import java.util.UUID;

public interface UpdateProjectUseCase {
    Project updateProject(
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
    );
}
