package com.bankingplatform.loanservice.client;

import com.bankingplatform.loanservice.config.FeignClientConfig;
import com.bankingplatform.loanservice.dto.UserKycResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** The caller's own JWT is forwarded (see FeignClientConfig) — a loan
 *  application is always submitted by the applicant themselves, so from
 *  User Service's point of view this looks like the user checking their
 *  own KYC status, the same as Account Service's identical pattern. */
@FeignClient(name = "user-service", path = "/users", configuration = FeignClientConfig.class)
public interface UserServiceClient {

    @GetMapping("/{userId}/kyc-status")
    UserKycResponse getKycStatus(@PathVariable("userId") String userId);
}
