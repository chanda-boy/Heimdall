package com.example.heimdall.project.presentation.dto.request;

import com.example.heimdall.project.domain.model.SciPosition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateContactRequestDto(
        @NotBlank(message = "El nombre completo es obligatorio")
        String fullName,
        @NotNull(message = "La posición SCI es obligatoria")
        SciPosition sciPosition,
        Integer priority,
        Boolean active
) {
    public int resolvePriority() {
        return priority != null && priority > 0 ? priority : 1;
    }

    public boolean resolveActive() {
        return active == null || active;
    }
}
