package com.bankingplatform.accountservice.service;

import com.bankingplatform.accountservice.client.UserServiceClient;
import com.bankingplatform.accountservice.dto.UserKycResponse;
import com.bankingplatform.common.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Split into its own bean (rather than a method on AccountServiceImpl) on
 * purpose: Resilience4j's @CircuitBreaker is a Spring AOP proxy, and a
 * same-class self-invocation (AccountServiceImpl calling its own method)
 * would silently bypass the proxy and the breaker would never engage.
 * Going through a separate bean guarantees the call is intercepted.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KycVerificationGateway {

    private final UserServiceClient userServiceClient;

    @CircuitBreaker(name = "userServiceCB", fallbackMethod = "kycCheckFallback")
    public void ensureVerified(String userId) {
        UserKycResponse kyc = userServiceClient.getKycStatus(userId);
        if (kyc == null || !"VERIFIED".equalsIgnoreCase(kyc.getKycStatus())) {
            throw new BusinessException("KYC must be verified before opening an account",
                    HttpStatus.FORBIDDEN, "KYC_NOT_VERIFIED");
        }
    }

    @SuppressWarnings("unused")
    private void kycCheckFallback(String userId, Throwable t) {
        log.warn("User Service unavailable while checking KYC for user {}: {}", userId, t.toString());
        throw new BusinessException("Unable to verify KYC status right now — please try again shortly",
                HttpStatus.SERVICE_UNAVAILABLE, "KYC_CHECK_UNAVAILABLE");
    }
}
