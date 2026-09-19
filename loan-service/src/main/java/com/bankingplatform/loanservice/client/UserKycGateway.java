package com.bankingplatform.loanservice.client;

import com.bankingplatform.common.exception.BusinessException;
import com.bankingplatform.loanservice.dto.UserKycResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Own bean so @CircuitBreaker actually goes through the Spring AOP proxy —
 *  see the same note on Account Service's KycVerificationGateway, which
 *  this mirrors exactly: fails CLOSED, since an unconfirmed KYC status
 *  should block the loan application rather than silently letting it
 *  through. */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserKycGateway {

    private final UserServiceClient userServiceClient;

    @CircuitBreaker(name = "userServiceCB", fallbackMethod = "kycCheckFallback")
    public void ensureVerified(String userId) {
        UserKycResponse kyc = userServiceClient.getKycStatus(userId);
        if (kyc == null || !"VERIFIED".equalsIgnoreCase(kyc.getKycStatus())) {
            throw new BusinessException("KYC must be verified before applying for a loan",
                    HttpStatus.FORBIDDEN, "KYC_NOT_VERIFIED");
        }
    }

    @SuppressWarnings("unused")
    private void kycCheckFallback(String userId, Throwable t) {
        if (t instanceof BusinessException businessException) {
            // A genuine "not verified" decided by ensureVerified() itself,
            // not a resilience concern — let it through as the real error
            // instead of masking it as a service outage.
            throw businessException;
        }
        log.warn("User Service unavailable while checking KYC for user {}: {}", userId, t.toString());
        throw new BusinessException("Unable to verify KYC status right now — please try again shortly",
                HttpStatus.SERVICE_UNAVAILABLE, "KYC_CHECK_UNAVAILABLE");
    }
}
