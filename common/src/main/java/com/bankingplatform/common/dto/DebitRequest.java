package com.bankingplatform.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Internal, service-to-service request. Transaction Service calls
 * Account Service with this payload to debit the source account as the
 * first leg of a transfer saga.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebitRequest {
    @NotBlank
    private String accountId;
    @NotNull
    @Positive
    private BigDecimal amount;
    /** transactionId / idempotency key — Account Service dedupes on this. */
    @NotBlank
    private String referenceId;
}
