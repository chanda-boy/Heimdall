package com.example.heimdall.iam.infrastructure.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Base64;

@ConfigurationProperties(prefix = "heimdall.jwt")
public record JwtProperties(String secret, Duration expiration, String issuer) {
    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("heimdall.jwt.secret must contain a Base64 encoded secret");
        }
        try {
            if (Base64.getDecoder().decode(secret).length < 32) {
                throw new IllegalArgumentException("heimdall.jwt.secret must contain at least 32 decoded bytes");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("heimdall.jwt.secret must be valid Base64 with at least 32 decoded bytes");
        }
        expiration = expiration == null ? Duration.ofMinutes(15) : expiration;
        if (expiration.compareTo(Duration.ofSeconds(1)) < 0) {
            throw new IllegalArgumentException("heimdall.jwt.expiration must be at least one second");
        }
        issuer = issuer == null ? "heimdall" : issuer;
        if (issuer.isBlank()) {
            throw new IllegalArgumentException("heimdall.jwt.issuer must not be blank");
        }
    }

    @Override
    public String toString() {
        return "JwtProperties[secret=REDACTED, expiration=" + expiration + ", issuer=" + issuer + "]";
    }
}
