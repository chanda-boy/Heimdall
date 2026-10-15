package com.example.heimdall.project.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ContactId(UUID value) {
    public ContactId {
        Objects.requireNonNull(value, "Contact ID is required");
    }

    public static ContactId random() {
        return new ContactId(UUID.randomUUID());
    }
}
