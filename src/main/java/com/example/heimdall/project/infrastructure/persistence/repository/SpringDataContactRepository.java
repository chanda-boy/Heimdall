package com.example.heimdall.project.infrastructure.persistence.repository;

import com.example.heimdall.project.infrastructure.persistence.entity.ContactJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SpringDataContactRepository extends JpaRepository<ContactJpaEntity, UUID> {
    List<ContactJpaEntity> findByProjectIdOrderByPriorityAsc(UUID projectId);
}
