package com.example.heimdall.iam.presentation.rest;

import com.example.heimdall.iam.domain.model.TokenClaims;
import com.example.heimdall.iam.domain.port.in.*;
import com.example.heimdall.iam.presentation.dto.request.*;
import com.example.heimdall.iam.presentation.dto.response.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final RegisterUserUseCase register;
    private final AuthenticateUserUseCase authenticate;
    private final AuthResponseMapper responses;

    public AuthController(RegisterUserUseCase register, AuthenticateUserUseCase authenticate, AuthResponseMapper responses) {
        this.register = register;
        this.authenticate = authenticate;
        this.responses = responses;
    }
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(responses.from(register.register(request.email(), request.password())));
    }
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(responses.from(authenticate.authenticate(request.email(), request.password())));
    }
    @GetMapping("/me")
    public UserResponseDto me(@AuthenticationPrincipal TokenClaims claims) {
        return UserResponseDto.from(claims);
    }
}
