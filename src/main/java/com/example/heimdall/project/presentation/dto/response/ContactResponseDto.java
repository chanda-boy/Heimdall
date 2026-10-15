package com.example.heimdall.project.presentation.dto.response;

import com.example.heimdall.project.domain.model.Contact;
import com.example.heimdall.project.domain.model.SciPosition;
import com.example.heimdall.project.presentation.dto.request.ContactChannelDto;
import com.example.heimdall.project.presentation.dto.request.ContactSubscriptionDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContactResponseDto(
        UUID id,
        UUID projectId,
        UUID userId,
        String fullName,
        SciPosition sciPosition,
        int priority,
        boolean active,
        Instant createdAt,
        List<ContactChannelDto> channels,
        List<ContactSubscriptionDto> subscriptions
) {
    public static ContactResponseDto from(Contact contact) {
        List<ContactChannelDto> channelDtos = contact.channels().stream()
                .map(ContactChannelDto::from)
                .toList();

        List<ContactSubscriptionDto> subDtos = contact.subscriptions().stream()
                .map(ContactSubscriptionDto::from)
                .toList();

        return new ContactResponseDto(
                contact.id().value(),
                contact.projectId().value(),
                contact.userId(),
                contact.fullName(),
                contact.sciPosition(),
                contact.priority(),
                contact.active(),
                contact.createdAt(),
                channelDtos,
                subDtos
        );
    }
}
