package com.example.heimdall.iam.domain.port.in;

import com.example.heimdall.iam.domain.model.AuthenticationResult;

public interface AuthenticateUserUseCase {
    AuthenticationResult authenticate(String email, String password);
}
