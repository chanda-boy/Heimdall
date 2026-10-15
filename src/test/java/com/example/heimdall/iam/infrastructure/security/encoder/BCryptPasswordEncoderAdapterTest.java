package com.example.heimdall.iam.infrastructure.security.encoder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BCryptPasswordEncoderAdapterTest {
    private final BCryptPasswordEncoderAdapter encoder = new BCryptPasswordEncoderAdapter();

    @Test
    void encodesWithSaltAndChecksOnlyCorrectPassword() {
        String firstHash = encoder.encode("a strong password");
        String secondHash = encoder.encode("a strong password");

        assertNotEquals("a strong password", firstHash);
        assertNotEquals(firstHash, secondHash);
        assertTrue(encoder.matches("a strong password", firstHash));
        assertFalse(encoder.matches("wrong password", firstHash));
        assertFalse(encoder.matches("a strong password", "malformed hash"));
    }
}
