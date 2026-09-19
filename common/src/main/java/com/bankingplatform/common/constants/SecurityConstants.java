package com.bankingplatform.common.constants;

public final class SecurityConstants {
    private SecurityConstants() {}

    /** Trusted headers set by API Gateway after JWT validation; downstream
     *  services should still independently validate the bearer token on
     *  protected endpoints (defense in depth) rather than trust these blindly
     *  when reachable off-cluster. */
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";
    public static final String USERNAME_HEADER = "X-Username";

    /** Service-to-service (Feign) calls use a static internal API key instead
     *  of the end user's JWT — separates user auth from service auth. */
    public static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    public static final String ROLE_CLAIM = "roles";
    public static final String USER_ID_CLAIM = "userId";

    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
}
