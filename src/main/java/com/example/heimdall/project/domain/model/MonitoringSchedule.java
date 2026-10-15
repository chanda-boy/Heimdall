package com.example.heimdall.project.domain.model;

import java.time.LocalTime;
import java.util.Objects;

public record MonitoringSchedule(LocalTime dailyRunTime, boolean enabled) {
    public MonitoringSchedule {
        Objects.requireNonNull(dailyRunTime, "Daily run time is required");
    }

    public static MonitoringSchedule defaultSchedule() {
        return new MonitoringSchedule(LocalTime.of(6, 0), true);
    }
}
