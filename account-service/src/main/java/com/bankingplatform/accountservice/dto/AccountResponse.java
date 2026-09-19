package com.bankingplatform.accountservice.dto;

import com.bankingplatform.accountservice.entity.AccountStatus;
import com.bankingplatform.accountservice.entity.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private String id;
    private String accountNumber;
    private String ownerUserId;
    private AccountType accountType;
    private BigDecimal balance;
    private String currency;
    private AccountStatus status;
    private LocalDate maturityDate;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private Instant createdAt;
}
