package com.bankingplatform.gateway.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/** Hit when a Resilience4j circuit breaker on a gateway route is open, or
 *  when a client tries to reach a service's internal-only endpoints
 *  directly (see the block-internal-* routes in api-gateway.yml). */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/{service}")
    public ResponseEntity<Map<String, Object>> fallback(@PathVariable String service) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "timestamp", Instant.now().toString(),
                "service", service,
                "status", 503,
                "message", service + " is temporarily unavailable (circuit open) — please retry shortly."
        ));
    }

    @RequestMapping(value = "/blocked",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<Map<String, Object>> blocked() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 404,
                "message", "This endpoint is for internal service-to-service use only and is not reachable through the public gateway."
        ));
    }
}
