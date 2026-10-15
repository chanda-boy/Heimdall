package com.example.heimdall.project.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "prj_contacts")
public class ContactJpaEntity {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "sci_position", nullable = false, length = 30)
    private String sciPosition;

    @Column(nullable = false)
    private short priority;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ContactJpaEntity() {}

    public ContactJpaEntity(UUID id, UUID projectId, UUID userId, String fullName, String sciPosition,
                            short priority, boolean active, Instant createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.userId = userId;
        this.fullName = fullName;
        this.sciPosition = sciPosition;
        this.priority = priority;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public UUID getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getSciPosition() { return sciPosition; }
    public short getPriority() { return priority; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
