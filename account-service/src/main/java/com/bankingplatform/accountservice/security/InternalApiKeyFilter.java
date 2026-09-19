package com.bankingplatform.accountservice.security;

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
 * The debit/credit/compensate endpoints under /accounts/internal/** move
 * money directly and must NEVER be callable by an end user, even one with a
 * perfectly valid JWT for their own account — that would let a customer
 * bypass the transfer Saga and edit their own balance. Those endpoints are
 * authenticated with a separate, server-side-only API key instead of the
 * user token scheme everything else on this service uses.
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    @Value("${security.internal.api-key}")
    private String expectedApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/accounts/internal/")) {
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
