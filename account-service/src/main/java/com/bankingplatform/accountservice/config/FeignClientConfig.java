package com.bankingplatform.accountservice.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Propagates the caller's own bearer token onto outgoing Feign calls that
 *  happen inside the scope of an incoming user request (e.g. the KYC check
 *  during account creation), so the downstream service sees the same
 *  identity the gateway already validated instead of needing a second,
 *  separate trust mechanism. Service-initiated calls with no originating
 *  user request (see Account Service's own /accounts/internal/** endpoints)
 *  use the internal API key instead — see InternalApiKeyFilter. */
@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor authHeaderForwardingInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (authHeader != null) {
                    requestTemplate.header(HttpHeaders.AUTHORIZATION, authHeader);
                }
            }
        };
    }
}
