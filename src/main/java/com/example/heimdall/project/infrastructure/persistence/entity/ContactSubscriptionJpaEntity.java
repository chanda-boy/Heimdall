package com.example.heimdall.project.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "prj_contact_subscriptions")
public class ContactSubscriptionJpaEntity {

    @EmbeddedId
    private ContactSubscriptionId id;

    @Column(name = "min_level", nullable = false, length = 20)
    private String minLevel;

    protected ContactSubscriptionJpaEntity() {}

    public ContactSubscriptionJpaEntity(ContactSubscriptionId id, String minLevel) {
        this.id = id;
        this.minLevel = minLevel;
    }

    public ContactSubscriptionId getId() { return id; }
    public String getMinLevel() { return minLevel; }
}
