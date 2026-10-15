package com.example.heimdall.iam.application.dto;

import com.example.heimdall.iam.domain.model.AuthenticationResult;
import com.example.heimdall.iam.domain.model.Email;
import com.example.heimdall.iam.domain.model.Role;
import com.example.heimdall.iam.domain.model.UserId;

public record AuthResult(String accessToken, UserId userId, Email email, Role role) {
    public static AuthResult from(AuthenticationResult result) {
        return new AuthResult(result.accessToken(), result.user().id(), result.user().email(), result.user().role());
    }

    @Override
    public String toString() {
        return "AuthResult[userId=" + userId + ", email=" + email + ", role=" + role + "]";
    }
}
