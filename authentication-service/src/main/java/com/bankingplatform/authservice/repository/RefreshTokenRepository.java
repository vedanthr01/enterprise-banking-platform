package com.bankingplatform.authservice.repository;

import com.bankingplatform.authservice.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);
    List<RefreshToken> findByUserIdAndRevokedFalse(String userId);
}
