package com.example.heimdall.project.domain.model;

import java.util.Objects;

public record NotificationPolicy(int notifyLeadDays, AlertLevel notifyMinLevel) {
    public NotificationPolicy {
        Objects.requireNonNull(notifyMinLevel, "Notification minimum level is required");
        if (notifyLeadDays < 0) {
            throw new IllegalArgumentException("Notification lead days cannot be negative: " + notifyLeadDays);
        }
    }

    public static NotificationPolicy defaultPolicy() {
        return new NotificationPolicy(1, AlertLevel.ORANGE);
    }
}
