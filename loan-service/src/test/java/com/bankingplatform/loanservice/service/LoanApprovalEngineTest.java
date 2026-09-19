package com.bankingplatform.loanservice.service;

import com.bankingplatform.loanservice.dto.LoanApplicationRequest;
import com.bankingplatform.loanservice.entity.LoanStatus;
import com.bankingplatform.loanservice.entity.LoanType;
import com.bankingplatform.loanservice.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanApprovalEngineTest {

    @Mock
    private LoanRepository loanRepository;

    private LoanApprovalEngine loanApprovalEngine;

    @BeforeEach
    void setUp() {
        loanApprovalEngine = new LoanApprovalEngine(loanRepository);
    }

    private LoanApplicationRequest request(LoanType type, String amount, int tenureMonths) {
        return LoanApplicationRequest.builder()
                .loanType(type)
                .principalAmount(new BigDecimal(amount))
                .interestRateAnnual(new BigDecimal("10.00"))
                .tenureMonths(tenureMonths)
                .disbursementAccountId("acc-1")
                .build();
    }

    @Test
    void evaluate_approves_whenWithinLimitsAndNoExistingActiveLoan() {
        when(loanRepository.existsByBorrowerUserIdAndLoanTypeAndStatus("user-1", LoanType.PERSONAL, LoanStatus.ACTIVE))
                .thenReturn(false);

        LoanApprovalEngine.Decision decision = loanApprovalEngine.evaluate("user-1", request(LoanType.PERSONAL, "200000", 24));

        assertThat(decision.approved()).isTrue();
        assertThat(decision.reason()).isNull();
    }

    @Test
    void evaluate_rejects_whenAmountExceedsTypeMaximum() {
        LoanApprovalEngine.Decision decision = loanApprovalEngine.evaluate("user-1", request(LoanType.PERSONAL, "600000", 24));

        assertThat(decision.approved()).isFalse();
        assertThat(decision.reason()).contains("exceeds the maximum");
    }

    @Test
    void evaluate_rejects_whenTenureBelowMinimumForType() {
        LoanApprovalEngine.Decision decision = loanApprovalEngine.evaluate("user-1", request(LoanType.PERSONAL, "50000", 3));

        assertThat(decision.approved()).isFalse();
        assertThat(decision.reason()).contains("Tenure must be between");
    }

    @Test
    void evaluate_rejects_whenTenureAboveMaximumForType() {
        LoanApprovalEngine.Decision decision = loanApprovalEngine.evaluate("user-1", request(LoanType.PERSONAL, "50000", 72));

        assertThat(decision.approved()).isFalse();
        assertThat(decision.reason()).contains("Tenure must be between");
    }

    @Test
    void evaluate_rejects_whenApplicantAlreadyHasAnActiveLoanOfSameType() {
        when(loanRepository.existsByBorrowerUserIdAndLoanTypeAndStatus("user-1", LoanType.AUTO, LoanStatus.ACTIVE))
                .thenReturn(true);

        LoanApprovalEngine.Decision decision = loanApprovalEngine.evaluate("user-1", request(LoanType.AUTO, "500000", 36));

        assertThat(decision.approved()).isFalse();
        assertThat(decision.reason()).contains("already exists");
    }
}
