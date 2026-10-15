package com.example.heimdall.project.infrastructure.persistence.repository;

import com.example.heimdall.project.infrastructure.persistence.entity.ContactChannelJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SpringDataContactChannelRepository extends JpaRepository<ContactChannelJpaEntity, UUID> {
    List<ContactChannelJpaEntity> findByContactId(UUID contactId);
    void deleteByContactId(UUID contactId);
}
