package com.example.heimdall.project.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ProjectId(UUID value) {
    public ProjectId {
        Objects.requireNonNull(value, "Project ID is required");
    }

    public static ProjectId random() {
        return new ProjectId(UUID.randomUUID());
    }
}
