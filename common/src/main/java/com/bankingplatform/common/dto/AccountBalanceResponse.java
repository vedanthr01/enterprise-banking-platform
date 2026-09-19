package com.bankingplatform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceResponse {
    private String accountId;
    private String accountNumber;
    private String ownerUserId;
    private BigDecimal balance;
    private String currency;
    private String status;
}
