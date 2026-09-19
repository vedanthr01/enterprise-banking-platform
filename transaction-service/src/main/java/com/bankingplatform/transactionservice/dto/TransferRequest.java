package com.bankingplatform.transactionservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotBlank
    private String fromAccountId;

    @NotBlank
    private String toAccountId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String currency;

    /** Client-generated (e.g. a UUID minted once per user action). Retried
     *  clicks/network retries with the same key always return the original
     *  result instead of transferring twice. */
    @NotBlank
    private String idempotencyKey;
}
