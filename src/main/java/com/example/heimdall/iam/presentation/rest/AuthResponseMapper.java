package com.example.heimdall.iam.presentation.rest;

import com.example.heimdall.iam.domain.model.AuthenticationResult;
import com.example.heimdall.iam.presentation.dto.response.AuthResponseDto;

public final class AuthResponseMapper {
    private final long expiresIn;
    public AuthResponseMapper(long expiresIn) { this.expiresIn = expiresIn; }
    public AuthResponseDto from(AuthenticationResult result) {
        return AuthResponseDto.from(result, expiresIn);
    }
}
