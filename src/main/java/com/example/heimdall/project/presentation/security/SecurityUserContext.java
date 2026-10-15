package com.example.heimdall.project.presentation.security;

import com.example.heimdall.iam.domain.model.Role;
import com.example.heimdall.iam.domain.model.TokenClaims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public record SecurityUserContext(UUID userId, boolean isAdmin) {

    public static SecurityUserContext current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof TokenClaims claims) {
            return new SecurityUserContext(claims.userId().value(), claims.role() == Role.ADMIN);
        }
        throw new IllegalStateException("Authentication token claims not found in security context");
    }
}
