package com.example.heimdall.project.infrastructure.persistence.mapper;

import com.example.heimdall.project.domain.model.AlertLevel;
import com.example.heimdall.project.domain.model.ChannelType;
import com.example.heimdall.project.domain.model.Contact;
import com.example.heimdall.project.domain.model.ContactChannel;
import com.example.heimdall.project.domain.model.ContactId;
import com.example.heimdall.project.domain.model.ContactSubscription;
import com.example.heimdall.project.domain.model.Hazard;
import com.example.heimdall.project.domain.model.ProjectId;
import com.example.heimdall.project.domain.model.SciPosition;
import com.example.heimdall.project.infrastructure.persistence.entity.ContactChannelJpaEntity;
import com.example.heimdall.project.infrastructure.persistence.entity.ContactJpaEntity;
import com.example.heimdall.project.infrastructure.persistence.entity.ContactSubscriptionId;
import com.example.heimdall.project.infrastructure.persistence.entity.ContactSubscriptionJpaEntity;
import java.util.List;
import java.util.UUID;

public final class ContactPersistenceMapper {

    private ContactPersistenceMapper() {}

    public static Contact toDomain(
            ContactJpaEntity entity,
            List<ContactChannelJpaEntity> channels,
            List<ContactSubscriptionJpaEntity> subscriptions
    ) {
        if (entity == null) return null;

        List<ContactChannel> channelList = channels.stream()
                .map(ContactPersistenceMapper::toDomainChannel)
                .toList();

        List<ContactSubscription> subscriptionList = subscriptions.stream()
                .map(ContactPersistenceMapper::toDomainSubscription)
                .toList();

        return new Contact(
                new ContactId(entity.getId()),
                new ProjectId(entity.getProjectId()),
                entity.getUserId(),
                entity.getFullName(),
                SciPosition.valueOf(entity.getSciPosition()),
                entity.getPriority(),
                entity.isActive(),
                entity.getCreatedAt(),
                channelList,
                subscriptionList
        );
    }

    public static ContactJpaEntity toEntity(Contact contact) {
        if (contact == null) return null;

        return new ContactJpaEntity(
                contact.id().value(),
                contact.projectId().value(),
                contact.userId(),
                contact.fullName(),
                contact.sciPosition().name(),
                (short) contact.priority(),
                contact.active(),
                contact.createdAt()
        );
    }

    public static ContactChannel toDomainChannel(ContactChannelJpaEntity entity) {
        return new ContactChannel(
                entity.getId(),
                ChannelType.valueOf(entity.getChannelType()),
                entity.getDestination(),
                entity.isVerified()
        );
    }

    public static ContactChannelJpaEntity toEntityChannel(UUID contactId, ContactChannel channel) {
        return new ContactChannelJpaEntity(
                channel.id(),
                contactId,
                channel.channelType().name(),
                channel.destination(),
                channel.verified()
        );
    }

    public static ContactSubscription toDomainSubscription(ContactSubscriptionJpaEntity entity) {
        return new ContactSubscription(
                Hazard.valueOf(entity.getId().getHazard()),
                AlertLevel.valueOf(entity.getMinLevel())
        );
    }

    public static ContactSubscriptionJpaEntity toEntitySubscription(UUID contactId, ContactSubscription subscription) {
        return new ContactSubscriptionJpaEntity(
                new ContactSubscriptionId(contactId, subscription.hazard().name()),
                subscription.minLevel().name()
        );
    }
}
