package com.bankingplatform.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** scanBasePackages reaches into com.bankingplatform.common so the shared
 *  GlobalExceptionHandler is picked up without every service redeclaring it. */
@SpringBootApplication(scanBasePackages = "com.bankingplatform")
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
