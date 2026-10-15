package com.example.heimdall.iam.domain.port.out;

import com.example.heimdall.iam.domain.model.TokenClaims;
import com.example.heimdall.iam.domain.model.User;

public interface TokenServicePort {
    String generateToken(User user);
    TokenClaims validateToken(String token);
}
