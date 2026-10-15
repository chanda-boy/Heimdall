package com.example.heimdall.iam.infrastructure.security.jwt;

import com.example.heimdall.iam.domain.exception.InvalidTokenException;
import com.example.heimdall.iam.domain.model.Email;
import com.example.heimdall.iam.domain.model.Role;
import com.example.heimdall.iam.domain.model.TokenClaims;
import com.example.heimdall.iam.domain.model.User;
import com.example.heimdall.iam.domain.model.UserId;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceAdapterTest {
    private static final Instant NOW = Instant.parse("2026-10-11T12:00:00Z");
    private static final byte[] KEY = new byte[64];
    private static final User USER = new User(UserId.random(), new Email("user@example.com"),
            "$2a$12$secret-hash", Role.USER);
    private final JwtProperties properties = new JwtProperties(Base64.getEncoder().encodeToString(KEY),
            Duration.ofMinutes(15), "heimdall");
    private final JwtTokenServiceAdapter adapter = new JwtTokenServiceAdapter(properties,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void roundTripContainsIdentityWithoutPassword() {
        String token = adapter.generateToken(USER);

        assertEquals(new TokenClaims(USER.id(), USER.email(), USER.role()), adapter.validateToken(token));
        var parsed = Jwts.parser().verifyWith(new SecretKeySpec(KEY, "HmacSHA256"))
                .clock(() -> Date.from(NOW)).build().parseSignedClaims(token);
        assertEquals("HS256", parsed.getHeader().getAlgorithm());
        assertEquals("heimdall", parsed.getPayload().getIssuer());
        assertEquals(Date.from(NOW), parsed.getPayload().getIssuedAt());
        assertEquals(Date.from(NOW.plusSeconds(900)), parsed.getPayload().getExpiration());
        assertEquals(6, parsed.getPayload().size());
        assertFalse(parsed.getPayload().containsKey("passwordHash"));
    }

    @Test
    void fractionalDurationUsesWholeSecondsConsistentWithExpiresIn() {
        var fractionalProperties = new JwtProperties(properties.secret(), Duration.ofMillis(1500), "heimdall");
        var fractionalClock = Clock.fixed(NOW.plusMillis(250), ZoneOffset.UTC);
        var fractionalAdapter = new JwtTokenServiceAdapter(fractionalProperties, fractionalClock);
        String token = fractionalAdapter.generateToken(USER);
        var claims = Jwts.parser().verifyWith(new SecretKeySpec(KEY, "HmacSHA256"))
                .clock(() -> Date.from(fractionalClock.instant())).build().parseSignedClaims(token).getPayload();

        assertEquals(fractionalProperties.expiration().toSeconds(),
                Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant()).toSeconds());
        assertEquals(Date.from(NOW.plusSeconds(1)), claims.getExpiration());
        assertEquals(new TokenClaims(USER.id(), USER.email(), USER.role()), fractionalAdapter.validateToken(token));
        var atExpiration = new JwtTokenServiceAdapter(fractionalProperties,
                Clock.fixed(NOW.plusSeconds(1), ZoneOffset.UTC));
        assertThrows(InvalidTokenException.class, () -> atExpiration.validateToken(token));
    }

    @Test
    void rejectsExpiredTokenAtExactExpiration() {
        String token = adapter.generateToken(USER);
        var atExpiration = new JwtTokenServiceAdapter(properties,
                Clock.fixed(NOW.plusSeconds(900), ZoneOffset.UTC));

        assertThrows(InvalidTokenException.class, () -> atExpiration.validateToken(token));
    }

    @Test
    void rejectsModifiedSignature() {
        String token = adapter.generateToken(USER);
        int signature = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signature) == 'A' ? 'B' : 'A';
        String modified = token.substring(0, signature) + replacement + token.substring(signature + 1);

        assertThrows(InvalidTokenException.class, () -> adapter.validateToken(modified));
    }

    @Test
    void rejectsTokenSignedByOtherKey() {
        byte[] otherKey = new byte[64];
        Arrays.fill(otherKey, (byte) 42);
        String token = validBuilder().signWith(new SecretKeySpec(otherKey, "HmacSHA256"), Jwts.SIG.HS256).compact();

        assertThrows(InvalidTokenException.class, () -> adapter.validateToken(token));
    }

    @Test
    void rejectsOtherAlgorithmEvenWithSameSecret() {
        String token = validBuilder().signWith(new SecretKeySpec(KEY, "HmacSHA384"), Jwts.SIG.HS384).compact();

        assertThrows(InvalidTokenException.class, () -> adapter.validateToken(token));
    }

    @ParameterizedTest
    @MethodSource("invalidClaims")
    void rejectsSignedTokensWithInvalidClaims(Consumer<JwtBuilder> mutation) {
        JwtBuilder builder = validBuilder();
        mutation.accept(builder);
        String token = builder.signWith(new SecretKeySpec(KEY, "HmacSHA256"), Jwts.SIG.HS256).compact();

        assertThrows(InvalidTokenException.class, () -> adapter.validateToken(token));
    }

    static Stream<Consumer<JwtBuilder>> invalidClaims() {
        return Stream.of(
                builder -> builder.issuer("other-service"),
                builder -> builder.issuer(null),
                builder -> builder.subject(null),
                builder -> builder.subject("not-a-uuid"),
                builder -> builder.subject("1-1-1-1-1"),
                builder -> builder.claim("email", "invalid"),
                builder -> builder.claim("email", null),
                builder -> builder.claim("role", "SUPERUSER"),
                builder -> builder.claim("role", null),
                builder -> builder.claim("role", 123),
                builder -> builder.issuedAt(null),
                builder -> builder.expiration(null),
                builder -> builder.issuedAt(Date.from(NOW.plusSeconds(60))),
                builder -> builder.issuedAt(Date.from(NOW)).expiration(Date.from(NOW)),
                builder -> builder.issuedAt(Date.from(NOW)).expiration(Date.from(NOW.minusSeconds(1)))
        );
    }

    @Test
    void rejectsMalformedUnsignedAndEmptyTokens() {
        for (String token : new String[]{"", "not-a-jwt", "a.b.c", validBuilder().compact()}) {
            assertThrows(InvalidTokenException.class, () -> adapter.validateToken(token));
        }
        assertThrows(InvalidTokenException.class, () -> adapter.validateToken(null));
    }

    @Test
    void failsFastOnUnsafeConfigurationWithoutPrintingSecret() {
        for (String secret : new String[]{"", "invalid-base64!", Base64.getEncoder().encodeToString(new byte[31])}) {
            assertThrows(IllegalArgumentException.class, () -> new JwtProperties(secret, Duration.ofMinutes(15), "heimdall"));
        }
        assertThrows(IllegalArgumentException.class, () -> new JwtProperties(properties.secret(), Duration.ZERO, "heimdall"));
        assertThrows(IllegalArgumentException.class, () -> new JwtProperties(properties.secret(), Duration.ofMillis(1), "heimdall"));
        assertFalse(properties.toString().contains(properties.secret()));
        assertEquals("heimdall", new JwtProperties(properties.secret(), null, null).issuer());
    }

    private static JwtBuilder validBuilder() {
        return Jwts.builder().issuer("heimdall").subject(USER.id().value().toString())
                .claim("email", USER.email().value()).claim("role", USER.role().name())
                .issuedAt(Date.from(NOW)).expiration(Date.from(NOW.plusSeconds(900)));
    }
}
