package com.bankingplatform.loanservice.service;

import com.bankingplatform.common.dto.AccountBalanceResponse;
import com.bankingplatform.common.exception.BusinessException;
import com.bankingplatform.loanservice.client.AccountServiceGateway;
import com.bankingplatform.loanservice.client.UserKycGateway;
import com.bankingplatform.loanservice.dto.LoanApplicationRequest;
import com.bankingplatform.loanservice.dto.LoanApplicationResponse;
import com.bankingplatform.loanservice.entity.LoanApplicationStatus;
import com.bankingplatform.loanservice.entity.LoanType;
import com.bankingplatform.loanservice.repository.EmiScheduleRepository;
import com.bankingplatform.loanservice.repository.LoanApplicationRepository;
import com.bankingplatform.loanservice.repository.LoanRepository;
import com.bankingplatform.loanservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Account Service and User Service are entirely mocked out (via
 * AccountServiceGateway / UserKycGateway), and the rule engine's decision
 * is stubbed directly rather than exercised through real rule logic — that
 * combinatorial space is LoanApprovalEngineTest's job. What's left to test
 * here is the orchestration: does the right thing happen, in the right
 * order, for each branch (KYC failure / rule rejection / disbursement
 * failure / full success)?
 */
@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceImplTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private EmiScheduleRepository emiScheduleRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private UserKycGateway userKycGateway;
    @Mock
    private LoanApprovalEngine loanApprovalEngine;
    @Mock
    private AccountServiceGateway accountServiceGateway;
    @Mock
    private ObjectMapper objectMapper;

    private LoanApplicationServiceImpl loanApplicationService;

    @BeforeEach
    void setUp() throws Exception {
        // Real calculator: pure math, no I/O — worth exercising for real
        // rather than mocking away the one interesting computation here.
        EmiCalculator emiCalculator = new EmiCalculator();
        loanApplicationService = new LoanApplicationServiceImpl(loanApplicationRepository, loanRepository,
                emiScheduleRepository, outboxEventRepository, userKycGateway, loanApprovalEngine, emiCalculator,
                accountServiceGateway, objectMapper);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
    }

    private LoanApplicationRequest request() {
        return LoanApplicationRequest.builder()
                .loanType(LoanType.PERSONAL)
                .principalAmount(new BigDecimal("100000"))
                .interestRateAnnual(new BigDecimal("12.00"))
                .tenureMonths(12)
                .disbursementAccountId("acc-1")
                .purpose("Home renovation")
                .build();
    }

    @Test
    void apply_disbursesAndCreatesLoan_whenKycVerifiedAndRulesApprove() {
        when(loanApprovalEngine.evaluate(any(), any())).thenReturn(LoanApprovalEngine.Decision.approve());
        when(accountServiceGateway.credit(any())).thenReturn(AccountBalanceResponse.builder().accountId("acc-1").build());

        LoanApplicationResponse response = loanApplicationService.apply("user-1", request());

        assertThat(response.getStatus()).isEqualTo(LoanApplicationStatus.DISBURSED);
        assertThat(response.getLoanId()).isNotBlank();
        verify(loanRepository).save(any());
        // 12 monthly installments generated for a 12-month tenure.
        verify(emiScheduleRepository, times(12)).save(any());
        // LOAN_APPLICATION_APPROVED, then LOAN_DISBURSED.
        verify(outboxEventRepository, times(2)).save(any());
    }

    @Test
    void apply_rejects_whenKycNotVerified() {
        doThrow(new BusinessException("KYC must be verified before applying for a loan",
                HttpStatus.FORBIDDEN, "KYC_NOT_VERIFIED"))
                .when(userKycGateway).ensureVerified("user-1");

        LoanApplicationResponse response = loanApplicationService.apply("user-1", request());

        assertThat(response.getStatus()).isEqualTo(LoanApplicationStatus.REJECTED);
        assertThat(response.getRejectionReason()).contains("KYC must be verified");
        verify(loanApprovalEngine, never()).evaluate(any(), any());
        verify(accountServiceGateway, never()).credit(any());
    }

    @Test
    void apply_rejects_whenRuleEngineRejects() {
        when(loanApprovalEngine.evaluate(any(), any()))
                .thenReturn(LoanApprovalEngine.Decision.reject("Requested amount exceeds the maximum"));

        LoanApplicationResponse response = loanApplicationService.apply("user-1", request());

        assertThat(response.getStatus()).isEqualTo(LoanApplicationStatus.REJECTED);
        assertThat(response.getRejectionReason()).contains("exceeds the maximum");
        verify(accountServiceGateway, never()).credit(any());
        verify(loanRepository, never()).save(any());
    }

    @Test
    void apply_marksDisbursementFailed_whenCreditCallFails() {
        when(loanApprovalEngine.evaluate(any(), any())).thenReturn(LoanApprovalEngine.Decision.approve());
        when(accountServiceGateway.credit(any())).thenThrow(new RuntimeException("account service unavailable"));

        LoanApplicationResponse response = loanApplicationService.apply("user-1", request());

        assertThat(response.getStatus()).isEqualTo(LoanApplicationStatus.DISBURSEMENT_FAILED);
        verify(loanRepository, never()).save(any());
        verify(emiScheduleRepository, never()).save(any());
    }
}
