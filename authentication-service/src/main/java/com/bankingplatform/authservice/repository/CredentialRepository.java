package com.bankingplatform.authservice.repository;

import com.bankingplatform.authservice.entity.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, String> {
    Optional<Credential> findByUsername(String username);
    Optional<Credential> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
