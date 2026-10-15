package com.example.heimdall.project.presentation.dto.request;

import com.example.heimdall.project.domain.model.SciPosition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateContactRequestDto(
        UUID userId,
        @NotBlank(message = "El nombre completo es obligatorio")
        String fullName,
        @NotNull(message = "La posición SCI es obligatoria")
        SciPosition sciPosition,
        Integer priority,
        @Valid List<ContactChannelDto> channels,
        @Valid List<ContactSubscriptionDto> subscriptions
) {
    public int resolvePriority() {
        return priority != null && priority > 0 ? priority : 1;
    }
}
