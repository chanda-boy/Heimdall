package com.example.heimdall.project.infrastructure.persistence.repository;

import com.example.heimdall.project.infrastructure.persistence.entity.ProjectJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataProjectRepository extends JpaRepository<ProjectJpaEntity, UUID> {
    List<ProjectJpaEntity> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    boolean existsByOwnerIdAndCode(UUID ownerId, String code);
    Optional<ProjectJpaEntity> findByOwnerIdAndCode(UUID ownerId, String code);
}
