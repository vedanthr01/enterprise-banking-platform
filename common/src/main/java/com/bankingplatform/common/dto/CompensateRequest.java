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
 * Saga compensating action: reverses a debit that already succeeded when a
 * later step in the transfer (e.g. the credit leg) fails. Idempotent on
 * referenceId, same as the original debit.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensateRequest {
    @NotBlank
    private String accountId;
    @NotNull
    @Positive
    private BigDecimal amount;
    @NotBlank
    private String referenceId;
    private String reason;
}
