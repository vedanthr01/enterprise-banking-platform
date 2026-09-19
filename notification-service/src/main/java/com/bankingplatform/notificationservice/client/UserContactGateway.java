package com.bankingplatform.notificationservice.client;

import com.bankingplatform.notificationservice.dto.UserContactResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Own bean (not a method on NotificationDispatcher) so @Cacheable and
 * @CircuitBreaker actually go through the Spring AOP proxy — same
 * self-invocation concern documented on Account Service's
 * KycVerificationGateway.
 *
 * Unlike KycVerificationGateway (which fails CLOSED because it gates a
 * security decision), this fails OPEN: a notification we can't address is
 * logged as SKIPPED, not treated as an error that should block anything —
 * the underlying business event already happened and already succeeded.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserContactGateway {

    private final UserServiceClient userServiceClient;

    @Cacheable(value = "userContact", key = "#userId", unless = "#result == null")
    @CircuitBreaker(name = "userServiceCB", fallbackMethod = "contactFallback")
    public UserContactResponse getContact(String userId) {
        return userServiceClient.getContact(userId);
    }

    @SuppressWarnings("unused")
    private UserContactResponse contactFallback(String userId, Throwable t) {
        log.warn("Could not resolve contact details for user {}: {} — the triggering notification will be logged as SKIPPED",
                userId, t.toString());
        return null;
    }
}
