package com.example.heimdall.project.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "prj_contact_channels", uniqueConstraints = {
        @UniqueConstraint(name = "uk_prj_contact_channels", columnNames = {"contact_id", "channel_type", "destination"})
})
public class ContactChannelJpaEntity {

    @Id
    private UUID id;

    @Column(name = "contact_id", nullable = false)
    private UUID contactId;

    @Column(name = "channel_type", nullable = false, length = 20)
    private String channelType;

    @Column(nullable = false, length = 255)
    private String destination;

    @Column(nullable = false)
    private boolean verified;

    protected ContactChannelJpaEntity() {}

    public ContactChannelJpaEntity(UUID id, UUID contactId, String channelType, String destination, boolean verified) {
        this.id = id;
        this.contactId = contactId;
        this.channelType = channelType;
        this.destination = destination;
        this.verified = verified;
    }

    public UUID getId() { return id; }
    public UUID getContactId() { return contactId; }
    public String getChannelType() { return channelType; }
    public String getDestination() { return destination; }
    public boolean isVerified() { return verified; }
}
