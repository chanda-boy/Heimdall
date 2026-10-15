package com.example.heimdall.project.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ContactSubscriptionId implements Serializable {

    @Column(name = "contact_id", nullable = false)
    private UUID contactId;

    @Column(nullable = false, length = 30)
    private String hazard;

    public ContactSubscriptionId() {}

    public ContactSubscriptionId(UUID contactId, String hazard) {
        this.contactId = contactId;
        this.hazard = hazard;
    }

    public UUID getContactId() { return contactId; }
    public String getHazard() { return hazard; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContactSubscriptionId that = (ContactSubscriptionId) o;
        return Objects.equals(contactId, that.contactId) && Objects.equals(hazard, that.hazard);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contactId, hazard);
    }
}
