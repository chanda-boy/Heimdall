package com.example.heimdall.iam.presentation.dto.response;

import com.example.heimdall.iam.domain.model.AuthenticationResult;
import com.example.heimdall.iam.domain.model.Role;
import java.util.UUID;

public record AuthResponseDto(String accessToken, String tokenType, long expiresIn,
                              UUID userId, String email, Role role) {
    public static AuthResponseDto from(AuthenticationResult result, long expiresIn) {
        var user = result.user();
        return new AuthResponseDto(result.accessToken(), "Bearer", expiresIn,
                user.id().value(), user.email().value(), user.role());
    }
    @Override public String toString() {
        return "AuthResponseDto[tokenType=" + tokenType + ", expiresIn=" + expiresIn + ", userId=" + userId + "]";
    }
}
