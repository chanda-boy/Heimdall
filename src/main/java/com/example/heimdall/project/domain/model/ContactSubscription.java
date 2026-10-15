package com.example.heimdall.project.domain.model;

import java.util.Objects;

public record ContactSubscription(Hazard hazard, AlertLevel minLevel) {
    public ContactSubscription {
        Objects.requireNonNull(hazard, "Hazard is required");
        Objects.requireNonNull(minLevel, "Minimum alert level is required");
    }
}
