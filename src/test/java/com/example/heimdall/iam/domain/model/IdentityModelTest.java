package com.example.heimdall.iam.domain.model;

import com.example.heimdall.iam.application.dto.LoginCommand;
import com.example.heimdall.iam.application.dto.RegisterCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdentityModelTest {
    @Test
    void emailNormalizesWhitespaceAndCase() {
        assertEquals(new Email("operator@example.com"), new Email("  OPERATOR@Example.COM  "));
        assertEquals("first.last+tag@example.com", new Email("first.last+tag@example.com").value());
    }

    @Test
    void rejectsMalformedEmailsAndOversizedLabels() {
        for (String invalid : new String[]{"", "no-at-sign", "a@@example.com", "a@localhost", ".a@example.com",
                "a..b@example.com", "a@-example.com", "a@exam ple.com", "x".repeat(65) + "@example.com",
                "a@" + "x".repeat(64) + ".com"}) {
            assertThrows(IllegalArgumentException.class, () -> new Email(invalid), invalid);
        }
        assertThrows(IllegalArgumentException.class, () -> new Email(null));
    }

    @Test
    void userRequiresIdentityRoleAndPasswordHash() {
        UserId id = UserId.random();
        Email email = new Email("a@example.com");
        assertThrows(NullPointerException.class, () -> new User(null, email, "hash", Role.USER));
        assertThrows(NullPointerException.class, () -> new User(id, null, "hash", Role.USER));
        assertThrows(NullPointerException.class, () -> new User(id, email, "hash", null));
        assertThrows(IllegalArgumentException.class, () -> new User(id, email, " ", Role.USER));
        assertThrows(NullPointerException.class, () -> new UserId(null));
    }

    @Test
    void diagnosticRepresentationsDoNotExposeSecrets() {
        User user = new User(UserId.random(), new Email("a@example.com"), "secret-hash", Role.USER);
        assertFalse(user.toString().contains("secret-hash"));
        assertFalse(new LoginCommand("a@example.com", "secret-password").toString().contains("secret-password"));
        assertFalse(new RegisterCommand("a@example.com", "secret-password").toString().contains("secret-password"));
        assertFalse(new AuthenticationResult("secret-token", user).toString().contains("secret-token"));
    }
}
