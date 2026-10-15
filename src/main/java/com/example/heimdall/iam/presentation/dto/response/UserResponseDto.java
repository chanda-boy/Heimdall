package com.example.heimdall.iam.presentation.dto.response;

import com.example.heimdall.iam.domain.model.Role;
import com.example.heimdall.iam.domain.model.TokenClaims;
import java.util.UUID;

public record UserResponseDto(UUID userId, String email, Role role) {
    public static UserResponseDto from(TokenClaims claims) {
        return new UserResponseDto(claims.userId().value(), claims.email().value(), claims.role());
    }
}
