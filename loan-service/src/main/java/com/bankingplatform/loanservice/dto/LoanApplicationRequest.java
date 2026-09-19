package com.bankingplatform.loanservice.dto;

import com.bankingplatform.loanservice.entity.LoanType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
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
public class LoanApplicationRequest {

    @NotNull
    private LoanType loanType;

    @NotNull
    @Positive
    private BigDecimal principalAmount;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal interestRateAnnual;

    @Min(1)
    @Max(480)
    private int tenureMonths;

    private String purpose;

    @NotBlank
    private String disbursementAccountId;
}
