package com.bankingplatform.authservice.service;

import com.bankingplatform.authservice.dto.AuthResponse;
import com.bankingplatform.authservice.dto.LoginRequest;
import com.bankingplatform.authservice.dto.RefreshRequest;
import com.bankingplatform.authservice.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(RefreshRequest request);
    void logout(String userId, String jti, long remainingTtlMs);
}
