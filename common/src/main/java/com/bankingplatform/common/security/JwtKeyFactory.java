package com.bankingplatform.common.security;

import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Every service that issues or validates platform JWTs (Authentication
 * Service issues; API Gateway and every resource server validate) must
 * derive the signing key the exact same way from the shared secret, or
 * signatures won't match. Centralizing it here removes that whole class of
 * bug.
 */
public final class JwtKeyFactory {
    private JwtKeyFactory() {}

    public static SecretKey build(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
