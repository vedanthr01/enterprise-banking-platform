package com.bankingplatform.authservice.service;

import com.bankingplatform.authservice.dto.AuthResponse;
import com.bankingplatform.authservice.dto.RegisterRequest;
import com.bankingplatform.authservice.entity.Credential;
import com.bankingplatform.authservice.repository.CredentialRepository;
import com.bankingplatform.authservice.repository.RefreshTokenRepository;
import com.bankingplatform.common.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private CredentialRepository credentialRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private StringRedisTemplate redisTemplate;

    private AuthServiceImpl authService;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(credentialRepository, refreshTokenRepository,
                passwordEncoder, jwtService, eventPublisher, redisTemplate);

        registerRequest = RegisterRequest.builder()
                .username("vedanth")
                .email("vedanth@example.com")
                .password("Secret123")
                .firstName("Vedanth")
                .lastName("R")
                .build();
    }

    @Test
    void register_createsCredentialAndIssuesTokens_whenUsernameAndEmailAreFree() {
        when(credentialRepository.existsByUsername("vedanth")).thenReturn(false);
        when(credentialRepository.existsByEmail("vedanth@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123")).thenReturn("hashed-password");
        when(jwtService.generateAccessToken(anyString(), eq("vedanth"), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyString())).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtService.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getUsername()).isEqualTo("vedanth");
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getRoles()).contains("ROLE_CUSTOMER");

        verify(credentialRepository).save(any(Credential.class));
        verify(refreshTokenRepository).save(any());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void register_throwsConflict_whenUsernameAlreadyTaken() {
        when(credentialRepository.existsByUsername("vedanth")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Username");

        verify(credentialRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
