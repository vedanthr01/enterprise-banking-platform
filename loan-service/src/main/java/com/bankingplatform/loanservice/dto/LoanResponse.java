package com.bankingplatform.loanservice.dto;

import com.bankingplatform.loanservice.entity.LoanStatus;
import com.bankingplatform.loanservice.entity.LoanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponse {
    private String id;
    private LoanType loanType;
    private BigDecimal principalAmount;
    private BigDecimal interestRateAnnual;
    private int tenureMonths;
    private BigDecimal emiAmount;
    private BigDecimal outstandingPrincipal;
    private LoanStatus status;
    private LocalDate startDate;
}
