package com.example.heimdall.project.infrastructure.persistence.repository;

import com.example.heimdall.project.infrastructure.persistence.entity.ContactSubscriptionId;
import com.example.heimdall.project.infrastructure.persistence.entity.ContactSubscriptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SpringDataContactSubscriptionRepository extends JpaRepository<ContactSubscriptionJpaEntity, ContactSubscriptionId> {
    List<ContactSubscriptionJpaEntity> findByIdContactId(UUID contactId);
    void deleteByIdContactId(UUID contactId);
}
