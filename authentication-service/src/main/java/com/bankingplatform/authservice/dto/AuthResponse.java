package com.bankingplatform.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String userId;
    private String username;
    private String accessToken;
    private String refreshToken;
    private long expiresInSeconds;
    private Set<String> roles;
}
