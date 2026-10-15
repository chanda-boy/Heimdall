package com.example.heimdall.iam.infrastructure.security.encoder;

import com.example.heimdall.iam.domain.port.out.PasswordEncoderPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordEncoderAdapter implements PasswordEncoderPort {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    /** BCrypt genera un salt por contraseña; el coste 12 dificulta ataques de fuerza bruta. */
    @Override
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /** Compara la contraseña con el hash sin recuperar ni descifrar la contraseña original. */
    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
