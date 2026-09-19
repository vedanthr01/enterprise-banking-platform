package com.bankingplatform.accountservice.dto;

import jakarta.validation.constraints.Min;
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
public class CreateFixedDepositRequest {
    @NotBlank
    private String sourceAccountId;

    @NotNull
    @Positive
    private BigDecimal principalAmount;

    @NotNull
    @Min(3)
    private Integer tenureMonths;
}
