package com.example.heimdall.iam.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(@NotBlank @Size(max = 254) String email,
                                 @NotBlank @Size(max = 72) String password) {
    @Override public String toString() { return "RegisterRequestDto[email=" + email + "]"; }
}
