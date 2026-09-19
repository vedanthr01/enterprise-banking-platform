package com.bankingplatform.transactionservice.config;

import com.bankingplatform.common.constants.SecurityConstants;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Every call this service makes to Account Service is service-initiated
 *  (part of the Saga), not a passthrough of a user's own request, so it
 *  authenticates with the internal API key rather than forwarding a JWT. */
@Configuration
public class InternalFeignClientConfig {

    @Value("${security.internal.api-key}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor internalApiKeyInterceptor() {
        return requestTemplate -> requestTemplate.header(SecurityConstants.INTERNAL_API_KEY_HEADER, internalApiKey);
    }
}
