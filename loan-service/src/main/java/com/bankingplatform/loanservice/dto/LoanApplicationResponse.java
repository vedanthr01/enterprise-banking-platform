package com.bankingplatform.loanservice.dto;

import com.bankingplatform.loanservice.entity.LoanApplicationStatus;
import com.bankingplatform.loanservice.entity.LoanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationResponse {
    private String id;
    private LoanType loanType;
    private BigDecimal principalAmount;
    private BigDecimal interestRateAnnual;
    private int tenureMonths;
    private LoanApplicationStatus status;
    private String rejectionReason;
    private String loanId;
    private Instant createdAt;
    private Instant decidedAt;
}
