package com.bankingplatform.accountservice.service;

import com.bankingplatform.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;

    public String generate() {
        String candidate;
        do {
            candidate = randomDigits(12);
        } while (accountRepository.existsByAccountNumber(candidate));
        return candidate;
    }

    private String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        sb.append(1 + RANDOM.nextInt(9)); // non-zero leading digit
        for (int i = 1; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
