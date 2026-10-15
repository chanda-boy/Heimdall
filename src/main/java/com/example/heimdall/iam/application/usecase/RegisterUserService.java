package com.example.heimdall.iam.application.usecase;

import com.example.heimdall.iam.application.dto.AuthResult;
import com.example.heimdall.iam.application.dto.RegisterCommand;
import com.example.heimdall.iam.domain.exception.UserAlreadyExistsException;
import com.example.heimdall.iam.domain.model.AuthenticationResult;
import com.example.heimdall.iam.domain.model.Email;
import com.example.heimdall.iam.domain.model.Role;
import com.example.heimdall.iam.domain.model.User;
import com.example.heimdall.iam.domain.model.UserId;
import com.example.heimdall.iam.domain.port.in.RegisterUserUseCase;
import com.example.heimdall.iam.domain.port.out.PasswordEncoderPort;
import com.example.heimdall.iam.domain.port.out.TokenServicePort;
import com.example.heimdall.iam.domain.port.out.UserRepositoryPort;
import java.util.Objects;

public final class RegisterUserService implements RegisterUserUseCase {
    private final UserRepositoryPort users;
    private final PasswordEncoderPort passwords;
    private final TokenServicePort tokens;

    public RegisterUserService(UserRepositoryPort users, PasswordEncoderPort passwords, TokenServicePort tokens) {
        this.users = Objects.requireNonNull(users);
        this.passwords = Objects.requireNonNull(passwords);
        this.tokens = Objects.requireNonNull(tokens);
    }

    @Override
    public AuthenticationResult register(String email, String password) {
        Email normalizedEmail = new Email(email);
        PasswordPolicy.validate(password);
        if (users.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistsException();
        }
        User user = users.save(new User(UserId.random(), normalizedEmail, passwords.encode(password), Role.USER));
        return new AuthenticationResult(tokens.generateToken(user), user);
    }

    public AuthResult register(RegisterCommand command) {
        Objects.requireNonNull(command, "Register command is required");
        return AuthResult.from(register(command.email(), command.password()));
    }
}
