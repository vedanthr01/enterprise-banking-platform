package com.bankingplatform.accountservice.client;

import com.bankingplatform.accountservice.config.FeignClientConfig;
import com.bankingplatform.accountservice.dto.UserKycResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * The caller's own JWT is forwarded onto this call (see FeignClientConfig),
 * so from User Service's point of view this looks like the user checking
 * their own KYC status — the same authorization rule
 * (#userId == authentication.name) applies either way.
 */
@FeignClient(name = "user-service", path = "/users", configuration = FeignClientConfig.class)
public interface UserServiceClient {

    @GetMapping("/{userId}/kyc-status")
    UserKycResponse getKycStatus(@PathVariable("userId") String userId);
}
