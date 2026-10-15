package com.example.heimdall.project.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Contact(
        ContactId id,
        ProjectId projectId,
        UUID userId,
        String fullName,
        SciPosition sciPosition,
        int priority,
        boolean active,
        Instant createdAt,
        List<ContactChannel> channels,
        List<ContactSubscription> subscriptions
) {
    public Contact {
        Objects.requireNonNull(id, "Contact ID is required");
        Objects.requireNonNull(projectId, "Project ID is required");
        Objects.requireNonNull(fullName, "Full name is required");
        Objects.requireNonNull(sciPosition, "SCI position is required");
        fullName = fullName.strip();
        if (fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be blank");
        }
        if (priority <= 0) {
            throw new IllegalArgumentException("Priority must be a positive integer: " + priority);
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        channels = channels == null ? List.of() : List.copyOf(channels);
        subscriptions = subscriptions == null ? List.of() : List.copyOf(subscriptions);
    }

    public Contact withChannels(List<ContactChannel> newChannels) {
        return new Contact(id, projectId, userId, fullName, sciPosition, priority, active, createdAt, newChannels, subscriptions);
    }

    public Contact withSubscriptions(List<ContactSubscription> newSubscriptions) {
        return new Contact(id, projectId, userId, fullName, sciPosition, priority, active, createdAt, channels, newSubscriptions);
    }

    public Contact withDetails(String newFullName, SciPosition newPosition, int newPriority, boolean newActive) {
        return new Contact(id, projectId, userId, newFullName, newPosition, newPriority, newActive, createdAt, channels, subscriptions);
    }
}
