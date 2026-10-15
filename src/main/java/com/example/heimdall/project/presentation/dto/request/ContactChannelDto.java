package com.example.heimdall.project.presentation.dto.request;

import com.example.heimdall.project.domain.model.ChannelType;
import com.example.heimdall.project.domain.model.ContactChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ContactChannelDto(
        UUID id,
        @NotNull ChannelType channelType,
        @NotBlank String destination,
        Boolean verified
) {
    public static ContactChannelDto from(ContactChannel channel) {
        return new ContactChannelDto(
                channel.id(),
                channel.channelType(),
                channel.destination(),
                channel.verified()
        );
    }

    public ContactChannel toDomain() {
        return new ContactChannel(
                id != null ? id : UUID.randomUUID(),
                channelType,
                destination,
                verified != null && verified
        );
    }
}
