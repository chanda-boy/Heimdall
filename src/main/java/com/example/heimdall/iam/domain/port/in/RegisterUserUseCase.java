package com.example.heimdall.iam.domain.port.in;

import com.example.heimdall.iam.domain.model.AuthenticationResult;

public interface RegisterUserUseCase {
    AuthenticationResult register(String email, String password);
}
