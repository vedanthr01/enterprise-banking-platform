package com.bankingplatform.notificationservice.config;

import com.bankingplatform.common.constants.SecurityConstants;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** This service's call to User Service is a background lookup done on
 *  behalf of the system, not a passthrough of an end user's own request —
 *  most triggers are async Kafka events with no user request in flight at
 *  all — so it authenticates with the shared internal API key rather than
 *  forwarding a JWT. */
@Configuration
public class InternalFeignClientConfig {

    @Value("${security.internal.api-key}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor internalApiKeyInterceptor() {
        return requestTemplate -> requestTemplate.header(SecurityConstants.INTERNAL_API_KEY_HEADER, internalApiKey);
    }
}
