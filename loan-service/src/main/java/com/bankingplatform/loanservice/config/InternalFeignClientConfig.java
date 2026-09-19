package com.bankingplatform.loanservice.config;

import com.bankingplatform.common.constants.SecurityConstants;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Disbursement and EMI auto-debit calls to Account Service are always
 *  service-initiated — the EMI scheduler in particular has no user request
 *  in flight at all — so they authenticate with the shared internal API
 *  key instead of forwarding a JWT, the same as Transaction Service's Saga
 *  calls. */
@Configuration
public class InternalFeignClientConfig {

    @Value("${security.internal.api-key}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor internalApiKeyInterceptor() {
        return requestTemplate -> requestTemplate.header(SecurityConstants.INTERNAL_API_KEY_HEADER, internalApiKey);
    }
}
