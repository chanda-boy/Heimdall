package com.example.heimdall.project.infrastructure.persistence;

import com.example.heimdall.project.domain.model.Contact;
import com.example.heimdall.project.domain.model.ContactId;
import com.example.heimdall.project.domain.model.ProjectId;
import com.example.heimdall.project.domain.port.out.ContactRepositoryPort;
import com.example.heimdall.project.infrastructure.persistence.entity.ContactChannelJpaEntity;
import com.example.heimdall.project.infrastructure.persistence.entity.ContactJpaEntity;
import com.example.heimdall.project.infrastructure.persistence.entity.ContactSubscriptionJpaEntity;
import com.example.heimdall.project.infrastructure.persistence.mapper.ContactPersistenceMapper;
import com.example.heimdall.project.infrastructure.persistence.repository.SpringDataContactChannelRepository;
import com.example.heimdall.project.infrastructure.persistence.repository.SpringDataContactRepository;
import com.example.heimdall.project.infrastructure.persistence.repository.SpringDataContactSubscriptionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class PostgresContactRepositoryAdapter implements ContactRepositoryPort {

    private final SpringDataContactRepository contactRepository;
    private final SpringDataContactChannelRepository channelRepository;
    private final SpringDataContactSubscriptionRepository subscriptionRepository;

    public PostgresContactRepositoryAdapter(
            SpringDataContactRepository contactRepository,
            SpringDataContactChannelRepository channelRepository,
            SpringDataContactSubscriptionRepository subscriptionRepository
    ) {
        this.contactRepository = Objects.requireNonNull(contactRepository);
        this.channelRepository = Objects.requireNonNull(channelRepository);
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
    }

    @Override
    @Transactional
    public Contact save(Contact contact) {
        UUID contactId = contact.id().value();
        ContactJpaEntity entity = ContactPersistenceMapper.toEntity(contact);
        ContactJpaEntity saved = contactRepository.save(entity);

        channelRepository.deleteByContactId(contactId);
        List<ContactChannelJpaEntity> channelEntities = contact.channels().stream()
                .map(ch -> ContactPersistenceMapper.toEntityChannel(contactId, ch))
                .toList();
        List<ContactChannelJpaEntity> savedChannels = channelRepository.saveAll(channelEntities);

        subscriptionRepository.deleteByIdContactId(contactId);
        List<ContactSubscriptionJpaEntity> subEntities = contact.subscriptions().stream()
                .map(sub -> ContactPersistenceMapper.toEntitySubscription(contactId, sub))
                .toList();
        List<ContactSubscriptionJpaEntity> savedSubs = subscriptionRepository.saveAll(subEntities);

        return ContactPersistenceMapper.toDomain(saved, savedChannels, savedSubs);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Contact> findById(ContactId id) {
        UUID contactId = id.value();
        return contactRepository.findById(contactId).map(entity -> {
            List<ContactChannelJpaEntity> channels = channelRepository.findByContactId(contactId);
            List<ContactSubscriptionJpaEntity> subs = subscriptionRepository.findByIdContactId(contactId);
            return ContactPersistenceMapper.toDomain(entity, channels, subs);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Contact> findByProjectId(ProjectId projectId) {
        List<ContactJpaEntity> entities = contactRepository.findByProjectIdOrderByPriorityAsc(projectId.value());
        return entities.stream().map(entity -> {
            UUID contactId = entity.getId();
            List<ContactChannelJpaEntity> channels = channelRepository.findByContactId(contactId);
            List<ContactSubscriptionJpaEntity> subs = subscriptionRepository.findByIdContactId(contactId);
            return ContactPersistenceMapper.toDomain(entity, channels, subs);
        }).toList();
    }

    @Override
    @Transactional
    public void delete(ContactId id) {
        contactRepository.deleteById(id.value());
    }
}
