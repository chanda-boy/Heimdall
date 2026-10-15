package com.example.heimdall.iam.application.usecase;

import com.example.heimdall.iam.application.dto.LoginCommand;
import com.example.heimdall.iam.application.dto.RegisterCommand;
import com.example.heimdall.iam.domain.exception.InvalidCredentialsException;
import com.example.heimdall.iam.domain.exception.UserAlreadyExistsException;
import com.example.heimdall.iam.domain.model.AuthenticationResult;
import com.example.heimdall.iam.domain.model.Email;
import com.example.heimdall.iam.domain.model.Role;
import com.example.heimdall.iam.domain.model.User;
import com.example.heimdall.iam.domain.model.UserId;
import com.example.heimdall.iam.domain.port.out.PasswordEncoderPort;
import com.example.heimdall.iam.domain.port.out.TokenServicePort;
import com.example.heimdall.iam.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthenticationUseCasesTest {
    private UserRepositoryPort users;
    private PasswordEncoderPort passwords;
    private TokenServicePort tokens;
    private RegisterUserService register;
    private AuthenticateUserService login;

    @BeforeEach
    void setUp() {
        users = mock(UserRepositoryPort.class);
        passwords = mock(PasswordEncoderPort.class);
        tokens = mock(TokenServicePort.class);
        when(passwords.encode(anyString())).thenReturn("encoded-password");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokens.generateToken(any(User.class))).thenReturn("access-token");
        register = new RegisterUserService(users, passwords, tokens);
        login = new AuthenticateUserService(users, passwords, tokens);
        clearInvocations(passwords);
    }

    @Test
    void registrationNormalizesEmailHashesPasswordAndAlwaysAssignsUser() {
        AuthenticationResult result = register.register("  OPERATOR@EXAMPLE.COM  ", "password-123");

        assertEquals(new Email("operator@example.com"), result.user().email());
        assertEquals(Role.USER, result.user().role());
        assertEquals("encoded-password", result.user().passwordHash());
        assertNotNull(result.user().id().value());
        assertEquals("access-token", result.accessToken());
        verify(passwords).encode("password-123");
        verify(users).save(result.user());
        verify(tokens).generateToken(result.user());
    }

    @Test
    void duplicateRegistrationDoesNotHashPersistOrIssueToken() {
        when(users.existsByEmail(new Email("operator@example.com"))).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class,
                () -> register.register("OPERATOR@example.com", "password-123"));

        verifyNoInteractions(passwords, tokens);
        verify(users, never()).save(any());
    }

    @Test
    void invalidRegistrationCannotReachPersistenceOrCryptography() {
        for (String password : new String[]{"short", "        ", "x".repeat(73), "á".repeat(37), "🔐".repeat(7)}) {
            assertThrows(IllegalArgumentException.class,
                    () -> register.register("operator@example.com", password));
        }
        assertThrows(IllegalArgumentException.class, () -> register.register("operator@example.com", null));
        assertThrows(IllegalArgumentException.class, () -> register.register("invalid", "password-123"));
        verifyNoInteractions(users, passwords, tokens);
    }

    @Test
    void passwordAtUtf8ByteLimitIsAccepted() {
        assertNotNull(register.register("operator@example.com", "á".repeat(36)));
        verify(passwords).encode("á".repeat(36));
    }

    @Test
    void loginAuthenticatesUsingStoredHashAndPreservesStoredRole() {
        User user = user(Role.ADMIN);
        when(users.findByEmail(user.email())).thenReturn(Optional.of(user));
        when(passwords.matches("password-123", user.passwordHash())).thenReturn(true);

        AuthenticationResult result = login.authenticate("OPERATOR@example.com", "password-123");

        assertSame(user, result.user());
        assertEquals(Role.ADMIN, result.user().role());
        assertEquals("access-token", result.accessToken());
        verify(tokens).generateToken(user);
        verify(passwords, never()).encode(anyString());
    }

    @Test
    void missingUserAndIncorrectPasswordHaveTheSameFailureMessageAndNeverIssueTokens() {
        User user = user(Role.USER);
        when(users.findByEmail(user.email())).thenReturn(Optional.empty());
        InvalidCredentialsException missing = assertThrows(InvalidCredentialsException.class,
                () -> login.authenticate(user.email().value(), "password-123"));
        verify(passwords).matches("password-123", "encoded-password");

        when(users.findByEmail(user.email())).thenReturn(Optional.of(user));
        InvalidCredentialsException incorrect = assertThrows(InvalidCredentialsException.class,
                () -> login.authenticate(user.email().value(), "password-123"));

        assertEquals(missing.getMessage(), incorrect.getMessage());
        verify(passwords).matches("password-123", "stored-password-hash");
        verifyNoInteractions(tokens);
    }

    @Test
    void unknownUserCannotLoginEvenIfDummyPasswordMatches() {
        when(users.findByEmail(any())).thenReturn(Optional.empty());
        when(passwords.matches(anyString(), anyString())).thenReturn(true);
        assertThrows(InvalidCredentialsException.class,
                () -> login.authenticate("missing@example.com", "password-123"));
        verifyNoInteractions(tokens);
    }

    @Test
    void malformedLoginUsesGenericCredentialFailure() {
        assertThrows(InvalidCredentialsException.class, () -> login.authenticate("invalid", "password-123"));
        assertThrows(InvalidCredentialsException.class,
                () -> login.authenticate("operator@example.com", "x".repeat(73)));
        verifyNoInteractions(users, passwords, tokens);
    }

    @Test
    void commandOverloadsExposeIdentityWithoutPasswordHash() {
        var result = register.register(new RegisterCommand("operator@example.com", "password-123"));
        assertEquals(Role.USER, result.role());
        assertEquals("access-token", result.accessToken());
        assertFalse(result.toString().contains("access-token"));

        User user = user(Role.ADMIN);
        when(users.findByEmail(user.email())).thenReturn(Optional.of(user));
        when(passwords.matches("password-123", user.passwordHash())).thenReturn(true);
        assertEquals(Role.ADMIN, login.authenticate(new LoginCommand(user.email().value(), "password-123")).role());
    }

    private User user(Role role) {
        return new User(UserId.random(), new Email("operator@example.com"), "stored-password-hash", role);
    }
}
