package com.bankingplatform.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Centralized configuration for every service in the platform. Backed by the
 * "native" profile (files bundled under src/main/resources/config-repo)
 * instead of a git remote, to keep the demo self-contained. In production
 * this would point spring.cloud.config.server.git.uri at a real config repo.
 */
@EnableConfigServer
@SpringBootApplication
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
