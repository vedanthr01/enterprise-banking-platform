package com.bankingplatform.authservice.service;

import com.bankingplatform.authservice.dto.AuthResponse;
import com.bankingplatform.authservice.dto.LoginRequest;
import com.bankingplatform.authservice.dto.RefreshRequest;
import com.bankingplatform.authservice.dto.RegisterRequest;
import com.bankingplatform.authservice.entity.AccountStatus;
import com.bankingplatform.authservice.entity.Credential;
import com.bankingplatform.authservice.entity.RefreshToken;
import com.bankingplatform.authservice.event.UserRegisteredApplicationEvent;
import com.bankingplatform.authservice.repository.CredentialRepository;
import com.bankingplatform.authservice.repository.RefreshTokenRepository;
import com.bankingplatform.common.constants.SecurityConstants;
import com.bankingplatform.common.exception.ConflictException;
import com.bankingplatform.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final CredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (credentialRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username is already taken");
        }
        if (credentialRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already registered");
        }

        Credential credential = Credential.builder()
                .id(UUID.randomUUID().toString())
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(SecurityConstants.ROLE_CUSTOMER))
                .status(AccountStatus.ACTIVE)
                .build();
        credentialRepository.save(credential);

        eventPublisher.publishEvent(new UserRegisteredApplicationEvent(
                credential.getId(), credential.getUsername(), credential.getEmail(),
                request.getFirstName(), request.getLastName(), request.getPhone()));

        return issueTokens(credential);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Credential credential = credentialRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (credential.getStatus() != AccountStatus.ACTIVE) {
            throw new UnauthorizedException("Account is " + credential.getStatus().name().toLowerCase());
        }
        if (!passwordEncoder.matches(request.getPassword(), credential.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        return issueTokens(credential);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String incomingHash = JwtService.hashToken(request.getRefreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(incomingHash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid or has been revoked"));

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        // Validates signature/expiry claims too, not just DB presence.
        jwtService.parse(request.getRefreshToken());

        Credential credential = credentialRepository.findById(stored.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokens(credential);
    }

    @Override
    @Transactional
    public void logout(String userId, String jti, long remainingTtlMs) {
        List<RefreshToken> active = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        active.forEach(rt -> rt.setRevoked(true));
        refreshTokenRepository.saveAll(active);

        if (jti != null && !jti.isBlank() && remainingTtlMs > 0) {
            redisTemplate.opsForValue().set("blacklist:jti:" + jti, "1", Duration.ofMillis(remainingTtlMs));
        }
    }

    private AuthResponse issueTokens(Credential credential) {
        String accessToken = jwtService.generateAccessToken(credential.getId(), credential.getUsername(), credential.getRoles());
        String refreshTokenValue = jwtService.generateRefreshToken(credential.getId());

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(credential.getId())
                .tokenHash(JwtService.hashToken(refreshTokenValue))
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .userId(credential.getId())
                .username(credential.getUsername())
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .expiresInSeconds(jwtService.getAccessTokenExpirationSeconds())
                .roles(credential.getRoles())
                .build();
    }
}
