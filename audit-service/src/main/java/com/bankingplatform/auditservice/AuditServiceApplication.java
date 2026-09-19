package com.bankingplatform.auditservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * No @EnableFeignClients here (unlike every other service) — Audit Service
 * never calls another service synchronously. It only consumes Kafka events
 * and serves its own read API.
 */
@SpringBootApplication(scanBasePackages = "com.bankingplatform")
public class AuditServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}
