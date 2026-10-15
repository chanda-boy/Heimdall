package com.example.heimdall.project.presentation.dto.request;

import com.example.heimdall.project.domain.model.ProjectStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequestDto(
        @NotNull(message = "El nuevo estado es obligatorio")
        ProjectStatus status
) {}
