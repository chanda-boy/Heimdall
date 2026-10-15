package com.example.heimdall.iam.infrastructure.security.jwt;

import com.example.heimdall.iam.domain.exception.InvalidTokenException;
import com.example.heimdall.iam.domain.model.Email;
import com.example.heimdall.iam.domain.model.Role;
import com.example.heimdall.iam.domain.model.TokenClaims;
import com.example.heimdall.iam.domain.model.User;
import com.example.heimdall.iam.domain.model.UserId;
import com.example.heimdall.iam.domain.port.out.TokenServicePort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenServiceAdapter implements TokenServicePort {
    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey key;
    private final JwtParser parser;

    public JwtTokenServiceAdapter(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.key = new SecretKeySpec(Base64.getDecoder().decode(properties.secret()), "HmacSHA256");
        this.parser = Jwts.parser().verifyWith(key).requireIssuer(properties.issuer())
                .clock(() -> Date.from(clock.instant())).build();
    }

    /** Firma los datos mínimos de identidad; la contraseña nunca se incorpora al JWT. */
    @Override
    public String generateToken(User user) {
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        return Jwts.builder().issuer(properties.issuer()).subject(user.id().value().toString())
                .claim("email", user.email().value()).claim("role", user.role().name())
                .issuedAt(Date.from(issuedAt)).expiration(Date.from(issuedAt.plus(properties.expiration())))
                .signWith(key, Jwts.SIG.HS256).compact();
    }

    /** Verifica firma, emisor, vigencia y estructura antes de convertir claims en identidad de dominio. */
    @Override
    public TokenClaims validateToken(String token) {
        try {
            var signedClaims = parser.parseSignedClaims(token);
            if (!"HS256".equals(signedClaims.getHeader().getAlgorithm())) {
                throw new InvalidTokenException();
            }
            Claims claims = signedClaims.getPayload();
            Date issuedAt = claims.getIssuedAt();
            Date expiration = claims.getExpiration();
            Instant now = clock.instant();
            if (issuedAt == null || expiration == null || issuedAt.toInstant().isAfter(now)
                    || !expiration.after(issuedAt) || !expiration.toInstant().isAfter(now)) {
                throw new InvalidTokenException();
            }
            String subject = claims.getSubject();
            UUID id = UUID.fromString(subject);
            if (!id.toString().equalsIgnoreCase(subject)) {
                throw new InvalidTokenException();
            }
            return new TokenClaims(new UserId(id), new Email(claims.get("email", String.class)),
                    Role.valueOf(claims.get("role", String.class)));
        } catch (JwtException | IllegalArgumentException | NullPointerException exception) {
            throw new InvalidTokenException(exception);
        }
    }
}
