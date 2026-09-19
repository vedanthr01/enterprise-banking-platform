package com.bankingplatform.authservice.controller;

import com.bankingplatform.authservice.dto.AuthResponse;
import com.bankingplatform.authservice.dto.LoginRequest;
import com.bankingplatform.authservice.dto.RefreshRequest;
import com.bankingplatform.authservice.dto.RegisterRequest;
import com.bankingplatform.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, token refresh and logout")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange credentials for an access + refresh token pair")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate a refresh token for a new access + refresh token pair")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke all refresh tokens and blacklist the current access token")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        long remainingTtlMs = jwt.getExpiresAt() != null
                ? Math.max(0, jwt.getExpiresAt().toEpochMilli() - Instant.now().toEpochMilli())
                : 0;
        authService.logout(jwt.getSubject(), jwt.getId(), remainingTtlMs);
        return ResponseEntity.noContent().build();
    }
}
