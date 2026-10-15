package com.example.heimdall.iam.infrastructure.config;

import com.example.heimdall.iam.application.usecase.*;
import com.example.heimdall.iam.domain.port.in.*;
import com.example.heimdall.iam.domain.port.out.*;
import com.example.heimdall.iam.infrastructure.security.jwt.JwtProperties;
import com.example.heimdall.iam.presentation.rest.AuthResponseMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class IamConfiguration {
    @Bean
    AuthResponseMapper authResponseMapper(JwtProperties jwt) {
        return new AuthResponseMapper(jwt.expiration().toSeconds());
    }
    @Bean
    RegisterUserUseCase registerUserUseCase(UserRepositoryPort users, PasswordEncoderPort passwords, TokenServicePort tokens) {
        return new RegisterUserService(users, passwords, tokens);
    }
    @Bean
    AuthenticateUserUseCase authenticateUserUseCase(UserRepositoryPort users, PasswordEncoderPort passwords, TokenServicePort tokens) {
        return new AuthenticateUserService(users, passwords, tokens);
    }
}
