package com.bankingplatform.frauddetectionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * No @EnableFeignClients — like Audit Service, this service never calls
 * another service synchronously. It consumes transaction-events, maintains
 * its own local transfer-history projection, and publishes fraud-alerts.
 */
@SpringBootApplication(scanBasePackages = "com.bankingplatform")
public class FraudDetectionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FraudDetectionServiceApplication.class, args);
    }
}
