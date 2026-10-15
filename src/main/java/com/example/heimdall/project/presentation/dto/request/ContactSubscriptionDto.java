package com.example.heimdall.project.presentation.dto.request;

import com.example.heimdall.project.domain.model.AlertLevel;
import com.example.heimdall.project.domain.model.ContactSubscription;
import com.example.heimdall.project.domain.model.Hazard;
import jakarta.validation.constraints.NotNull;

public record ContactSubscriptionDto(
        @NotNull Hazard hazard,
        @NotNull AlertLevel minLevel
) {
    public static ContactSubscriptionDto from(ContactSubscription subscription) {
        return new ContactSubscriptionDto(
                subscription.hazard(),
                subscription.minLevel()
        );
    }

    public ContactSubscription toDomain() {
        return new ContactSubscription(hazard, minLevel);
    }
}
