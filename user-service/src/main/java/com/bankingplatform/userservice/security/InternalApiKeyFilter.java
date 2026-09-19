package com.bankingplatform.userservice.security;

import com.bankingplatform.common.constants.SecurityConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Mirrors Account Service's InternalApiKeyFilter: the contact-lookup
 * endpoint under /users/internal/** hands back a user's email/phone and
 * must never be reachable with an ordinary customer JWT — only other
 * services (Notification Service) with the shared internal key may call it.
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    @Value("${security.internal.api-key}")
    private String expectedApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/users/internal/")) {
            String providedKey = request.getHeader(SecurityConstants.INTERNAL_API_KEY_HEADER);
            if (providedKey != null && providedKey.equals(expectedApiKey)) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "internal-service", null, List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
