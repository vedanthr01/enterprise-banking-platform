package com.bankingplatform.gateway.filter;

import com.bankingplatform.common.constants.SecurityConstants;
import com.bankingplatform.common.security.JwtKeyFactory;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the edge-of-cluster JWT filter. Runs the filter directly
 * against mock reactive request/response objects (MockServerHttpRequest /
 * MockServerWebExchange from spring-test) rather than a full WebTestClient
 * setup — this filter has no Spring context dependencies beyond the two
 * constructor arguments, so standing up a full application context would
 * only slow the test down for no extra coverage.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String SECRET = "gateway-test-secret-key-must-be-at-least-256-bits-long-0123456789";

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private GatewayFilterChain chain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(SECRET, redisTemplate);
    }

    private String mintToken(String userId, String username, List<String> roles, String jti) {
        return Jwts.builder()
                .id(jti)
                .subject(userId)
                .claim("username", username)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(JwtKeyFactory.build(SECRET))
                .compact();
    }

    @Test
    void filter_allowsOpenPath_withoutRequiringAToken() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/login").build());
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(exchange);
        verify(redisTemplate, never()).hasKey(anyString());
    }

    @Test
    void filter_rejectsProtectedPath_whenBearerTokenMissing() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/accounts/acc-1").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_rejectsProtectedPath_whenTokenIsMalformed() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/accounts/acc-1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-jwt")
                        .build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_rejectsProtectedPath_whenTokenIsBlacklisted() {
        String token = mintToken("user-1", "alice", List.of(SecurityConstants.ROLE_CUSTOMER), "jti-1");
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/accounts/acc-1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());
        when(redisTemplate.hasKey("blacklist:jti:jti-1")).thenReturn(Mono.just(true));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_forwardsRequest_withTrustedIdentityHeaders_forValidNonBlacklistedToken() {
        String token = mintToken("user-42", "bob", List.of(SecurityConstants.ROLE_CUSTOMER), "jti-2");
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/accounts/acc-1")
                        // A caller-supplied spoofed identity header must never survive:
                        // the filter has to strip this, not merge with it.
                        .header(SecurityConstants.USER_ID_HEADER, "someone-else")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());
        when(redisTemplate.hasKey("blacklist:jti:jti-2")).thenReturn(Mono.just(false));
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain, times(1)).filter(any());
        ServerHttpRequest forwarded = captor.getValue().getRequest();
        assertThat(forwarded.getHeaders().getFirst(SecurityConstants.USER_ID_HEADER)).isEqualTo("user-42");
        assertThat(forwarded.getHeaders().getFirst(SecurityConstants.USERNAME_HEADER)).isEqualTo("bob");
        assertThat(forwarded.getHeaders().getFirst(SecurityConstants.USER_ROLES_HEADER))
                .isEqualTo(SecurityConstants.ROLE_CUSTOMER);
    }
}
