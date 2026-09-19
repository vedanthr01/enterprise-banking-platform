package com.bankingplatform.loanservice.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Propagates the caller's own bearer token onto the KYC check, which
 *  always happens inside the scope of the applicant's own incoming request
 *  — see Account Service's identical FeignClientConfig. The EMI auto-debit
 *  scheduler has no such request in flight and uses the internal API key
 *  instead (see InternalFeignClientConfig). */
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
