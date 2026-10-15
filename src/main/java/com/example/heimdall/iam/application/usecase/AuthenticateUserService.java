package com.example.heimdall.iam.application.usecase;

import com.example.heimdall.iam.application.dto.AuthResult;
import com.example.heimdall.iam.application.dto.LoginCommand;
import com.example.heimdall.iam.domain.exception.InvalidCredentialsException;
import com.example.heimdall.iam.domain.model.AuthenticationResult;
import com.example.heimdall.iam.domain.model.Email;
import com.example.heimdall.iam.domain.model.User;
import com.example.heimdall.iam.domain.port.in.AuthenticateUserUseCase;
import com.example.heimdall.iam.domain.port.out.PasswordEncoderPort;
import com.example.heimdall.iam.domain.port.out.TokenServicePort;
import com.example.heimdall.iam.domain.port.out.UserRepositoryPort;
import java.util.Objects;
import java.util.Optional;

public final class AuthenticateUserService implements AuthenticateUserUseCase {
    private final UserRepositoryPort users;
    private final PasswordEncoderPort passwords;
    private final TokenServicePort tokens;
    private final String dummyHash;

    public AuthenticateUserService(UserRepositoryPort users, PasswordEncoderPort passwords, TokenServicePort tokens) {
        this.users = Objects.requireNonNull(users);
        this.passwords = Objects.requireNonNull(passwords);
        this.tokens = Objects.requireNonNull(tokens);
        // Perform BCrypt verification even for unknown users to reduce account enumeration via timing.
        this.dummyHash = passwords.encode("heimdall-dummy-password-for-login");
    }

    @Override
    public AuthenticationResult authenticate(String email, String password) {
        Email normalizedEmail;
        try {
            normalizedEmail = new Email(email);
            PasswordPolicy.validate(password);
        } catch (IllegalArgumentException invalidInput) {
            throw new InvalidCredentialsException();
        }
        Optional<User> candidate = users.findByEmail(normalizedEmail);
        boolean matches = passwords.matches(password, candidate.map(User::passwordHash).orElse(dummyHash));
        if (candidate.isEmpty() || !matches) {
            throw new InvalidCredentialsException();
        }
        User user = candidate.orElseThrow();
        return new AuthenticationResult(tokens.generateToken(user), user);
    }

    public AuthResult authenticate(LoginCommand command) {
        Objects.requireNonNull(command, "Login command is required");
        return AuthResult.from(authenticate(command.email(), command.password()));
    }
}
