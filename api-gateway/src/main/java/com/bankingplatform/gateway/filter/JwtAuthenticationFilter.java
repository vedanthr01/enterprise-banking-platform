package com.bankingplatform.gateway.filter;

import com.bankingplatform.common.constants.SecurityConstants;
import com.bankingplatform.common.security.JwtKeyFactory;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validates every request at the edge before it reaches a downstream
 * service. Public auth endpoints (register/login/refresh), Swagger, and
 * actuator are whitelisted; everything else needs a valid bearer JWT.
 *
 * On success, the user's identity is re-emitted as trusted headers
 * (X-User-Id / X-User-Roles / X-Username) for downstream services to read —
 * any client-supplied copies of those headers are stripped first so a
 * caller can't forge them by hand. Downstream services still independently
 * validate the JWT on their own protected endpoints (see each service's
 * SecurityConfig) as defense in depth, since in a real deployment those
 * services could in principle be reached directly, not only through the
 * gateway.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final List<String> OPEN_PATH_PREFIXES = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/actuator",
            "/fallback"
    );

    private final SecretKey signingKey;
    private final ReactiveStringRedisTemplate redisTemplate;

    public JwtAuthenticationFilter(@Value("${security.jwt.secret}") String secret,
                                    ReactiveStringRedisTemplate redisTemplate) {
        this.signingKey = JwtKeyFactory.build(secret);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isOpen(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing bearer token");
        }

        String token = authHeader.substring(7);
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            return unauthorized(exchange, "Invalid or expired token");
        }

        String jti = claims.getId();
        String userId = claims.getSubject();
        String username = claims.get("username", String.class);
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        String rolesHeader = roles == null ? "" : roles.stream().collect(Collectors.joining(","));

        Mono<Boolean> blacklistCheck = (jti == null || jti.isBlank())
                ? Mono.just(false)
                : redisTemplate.hasKey("blacklist:jti:" + jti).defaultIfEmpty(false);

        return blacklistCheck.flatMap(isBlacklisted -> {
            if (Boolean.TRUE.equals(isBlacklisted)) {
                return unauthorized(exchange, "Token has been revoked");
            }

            ServerHttpRequest mutated = request.mutate()
                    .headers(h -> {
                        h.remove(SecurityConstants.USER_ID_HEADER);
                        h.remove(SecurityConstants.USER_ROLES_HEADER);
                        h.remove(SecurityConstants.USERNAME_HEADER);
                        h.set(SecurityConstants.USER_ID_HEADER, userId == null ? "" : userId);
                        h.set(SecurityConstants.USER_ROLES_HEADER, rolesHeader);
                        h.set(SecurityConstants.USERNAME_HEADER, username == null ? "" : username);
                    })
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());
        });
    }

    private boolean isOpen(String path) {
        return OPEN_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        byte[] bytes = ("{\"errorCode\":\"UNAUTHORIZED\",\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
