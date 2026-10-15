package com.example.heimdall.iam.infrastructure.persistence.entity;

import com.example.heimdall.iam.domain.model.Role;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "iam_users", uniqueConstraints = @UniqueConstraint(name = "uk_iam_users_email", columnNames = "email"))
public class UserJpaEntity {
    @Id
    private UUID id;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(name = "platform_role", nullable = false, length = 20)
    private Role role;

    protected UserJpaEntity() {}
    public UserJpaEntity(UUID id, String email, String passwordHash, Role role) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }
    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
}
