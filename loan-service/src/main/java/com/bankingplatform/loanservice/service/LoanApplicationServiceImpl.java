package com.bankingplatform.loanservice.service;

import com.bankingplatform.common.constants.KafkaTopics;
import com.bankingplatform.common.dto.CreditRequest;
import com.bankingplatform.common.event.LoanEvent;
import com.bankingplatform.common.event.LoanEventType;
import com.bankingplatform.common.exception.ResourceNotFoundException;
import com.bankingplatform.common.exception.UnauthorizedException;
import com.bankingplatform.loanservice.client.AccountServiceGateway;
import com.bankingplatform.loanservice.client.UserKycGateway;
import com.bankingplatform.loanservice.dto.EmiScheduleResponse;
import com.bankingplatform.loanservice.dto.LoanApplicationRequest;
import com.bankingplatform.loanservice.dto.LoanApplicationResponse;
import com.bankingplatform.loanservice.dto.LoanResponse;
import com.bankingplatform.loanservice.entity.EmiSchedule;
import com.bankingplatform.loanservice.entity.EmiStatus;
import com.bankingplatform.loanservice.entity.Loan;
import com.bankingplatform.loanservice.entity.LoanApplication;
import com.bankingplatform.loanservice.entity.LoanApplicationStatus;
import com.bankingplatform.loanservice.entity.LoanStatus;
import com.bankingplatform.loanservice.entity.OutboxEvent;
import com.bankingplatform.loanservice.repository.EmiScheduleRepository;
import com.bankingplatform.loanservice.repository.LoanApplicationRepository;
import com.bankingplatform.loanservice.repository.LoanRepository;
import com.bankingplatform.loanservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrates the full loan lifecycle from application through disbursement:
 * persist the application, check KYC, run the (deterministic, explainable)
 * approval rules, and — if approved — disburse funds and generate the EMI
 * schedule. Money movement uses plain synchronous Feign calls with
 * try/catch, the same style as Transaction Service's Saga steps; a
 * transactional outbox is used only for publishing the resulting domain
 * event to Kafka, to avoid the dual-write problem on that specific concern.
 *
 * A KYC-check failure (genuinely unverified, or User Service unreachable)
 * both currently land the application in REJECTED — a deliberate
 * simplification; the rejection reason text still distinguishes the two
 * cases for the applicant even though the status doesn't.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanApplicationServiceImpl implements LoanApplicationService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanRepository loanRepository;
    private final EmiScheduleRepository emiScheduleRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final UserKycGateway userKycGateway;
    private final LoanApprovalEngine loanApprovalEngine;
    private final EmiCalculator emiCalculator;
    private final AccountServiceGateway accountServiceGateway;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public LoanApplicationResponse apply(String applicantUserId, LoanApplicationRequest request) {
        LoanApplication application = LoanApplication.builder()
                .id(UUID.randomUUID().toString())
                .applicantUserId(applicantUserId)
                .loanType(request.getLoanType())
                .principalAmount(request.getPrincipalAmount())
                .interestRateAnnual(request.getInterestRateAnnual())
                .tenureMonths(request.getTenureMonths())
                .purpose(request.getPurpose())
                .disbursementAccountId(request.getDisbursementAccountId())
                .status(LoanApplicationStatus.PENDING)
                .build();
        loanApplicationRepository.save(application);

        try {
            userKycGateway.ensureVerified(applicantUserId);
        } catch (Exception ex) {
            return reject(application, rootMessage(ex));
        }

        LoanApprovalEngine.Decision decision = loanApprovalEngine.evaluate(applicantUserId, request);
        if (!decision.approved()) {
            return reject(application, decision.reason());
        }

        application.setStatus(LoanApplicationStatus.APPROVED);
        application.setDecidedAt(Instant.now());
        loanApplicationRepository.save(application);
        // Published even though disbursement happens immediately after, in
        // the same call: if disbursement then fails, this is the only
        // record that underwriting DID say yes — a meaningful distinction
        // for Audit Service between "rejected on the merits" and "approved
        // but the money never moved".
        writeOutboxEvent(application.getId(), null, application.getApplicantUserId(),
                LoanEventType.LOAN_APPLICATION_APPROVED, application.getPrincipalAmount(), null);

        return disburse(application);
    }

    private LoanApplicationResponse disburse(LoanApplication application) {
        try {
            accountServiceGateway.credit(CreditRequest.builder()
                    .accountId(application.getDisbursementAccountId())
                    .amount(application.getPrincipalAmount())
                    .referenceId(application.getId())
                    .build());
        } catch (Exception ex) {
            application.setStatus(LoanApplicationStatus.DISBURSEMENT_FAILED);
            loanApplicationRepository.save(application);
            log.error("Disbursement failed for approved application {}: {} — needs manual follow-up",
                    application.getId(), rootMessage(ex));
            writeOutboxEvent(application.getId(), null, application.getApplicantUserId(),
                    LoanEventType.LOAN_APPLICATION_REJECTED, application.getPrincipalAmount(),
                    "Approved but disbursement failed: " + rootMessage(ex));
            return toApplicationResponse(application, null);
        }

        application.setStatus(LoanApplicationStatus.DISBURSED);
        loanApplicationRepository.save(application);

        Loan loan = createLoanAndSchedule(application);

        writeOutboxEvent(application.getId(), loan.getId(), application.getApplicantUserId(),
                LoanEventType.LOAN_DISBURSED, application.getPrincipalAmount(), null);

        return toApplicationResponse(application, loan.getId());
    }

    private Loan createLoanAndSchedule(LoanApplication application) {
        BigDecimal emi = emiCalculator.calculateEmi(application.getPrincipalAmount(),
                application.getInterestRateAnnual(), application.getTenureMonths());

        Loan loan = Loan.builder()
                .id(UUID.randomUUID().toString())
                .applicationId(application.getId())
                .borrowerUserId(application.getApplicantUserId())
                .loanType(application.getLoanType())
                .disbursementAccountId(application.getDisbursementAccountId())
                .principalAmount(application.getPrincipalAmount())
                .interestRateAnnual(application.getInterestRateAnnual())
                .tenureMonths(application.getTenureMonths())
                .emiAmount(emi)
                .outstandingPrincipal(application.getPrincipalAmount())
                .status(LoanStatus.ACTIVE)
                .startDate(LocalDate.now())
                .build();
        loanRepository.save(loan);

        List<EmiCalculator.InstallmentBreakdown> schedule = emiCalculator.generateSchedule(
                application.getPrincipalAmount(), application.getInterestRateAnnual(), application.getTenureMonths());

        LocalDate firstDueDate = LocalDate.now().plusMonths(1);
        for (EmiCalculator.InstallmentBreakdown installment : schedule) {
            emiScheduleRepository.save(EmiSchedule.builder()
                    .id(UUID.randomUUID().toString())
                    .loanId(loan.getId())
                    .installmentNumber(installment.installmentNumber())
                    .dueDate(firstDueDate.plusMonths(installment.installmentNumber() - 1L))
                    .emiAmount(installment.emiAmount())
                    .principalComponent(installment.principalComponent())
                    .interestComponent(installment.interestComponent())
                    .status(EmiStatus.PENDING)
                    .build());
        }

        return loan;
    }

    private LoanApplicationResponse reject(LoanApplication application, String reason) {
        application.setStatus(LoanApplicationStatus.REJECTED);
        application.setRejectionReason(reason);
        application.setDecidedAt(Instant.now());
        loanApplicationRepository.save(application);
        writeOutboxEvent(application.getId(), null, application.getApplicantUserId(),
                LoanEventType.LOAN_APPLICATION_REJECTED, application.getPrincipalAmount(), reason);
        return toApplicationResponse(application, null);
    }

    @Override
    public LoanApplicationResponse getApplication(String applicationId, String requesterUserId, boolean isAdmin) {
        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found: " + applicationId));
        if (!isAdmin && !application.getApplicantUserId().equals(requesterUserId)) {
            throw new UnauthorizedException("You do not have access to this loan application");
        }
        String loanId = loanRepository.findByApplicationId(applicationId).map(Loan::getId).orElse(null);
        return toApplicationResponse(application, loanId);
    }

    @Override
    public List<LoanApplicationResponse> getMyApplications(String applicantUserId) {
        return loanApplicationRepository.findByApplicantUserIdOrderByCreatedAtDesc(applicantUserId).stream()
                .map(app -> toApplicationResponse(app,
                        loanRepository.findByApplicationId(app.getId()).map(Loan::getId).orElse(null)))
                .collect(Collectors.toList());
    }

    @Override
    public LoanResponse getLoan(String loanId, String requesterUserId, boolean isAdmin) {
        Loan loan = findLoanOrThrow(loanId);
        if (!isAdmin && !loan.getBorrowerUserId().equals(requesterUserId)) {
            throw new UnauthorizedException("You do not have access to this loan");
        }
        return toLoanResponse(loan);
    }

    @Override
    public List<EmiScheduleResponse> getSchedule(String loanId, String requesterUserId, boolean isAdmin) {
        Loan loan = findLoanOrThrow(loanId);
        if (!isAdmin && !loan.getBorrowerUserId().equals(requesterUserId)) {
            throw new UnauthorizedException("You do not have access to this loan's schedule");
        }
        return emiScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId).stream()
                .map(this::toScheduleResponse)
                .collect(Collectors.toList());
    }

    private Loan findLoanOrThrow(String loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));
    }

    // -- outbox + mapping ------------------------------------------------------

    private void writeOutboxEvent(String applicationId, String loanId, String borrowerUserId,
                                   LoanEventType eventType, BigDecimal amount, String reason) {
        LoanEvent payload = LoanEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .applicationId(applicationId)
                .loanId(loanId)
                .borrowerUserId(borrowerUserId)
                .eventType(eventType)
                .amount(amount)
                .reason(reason)
                .occurredAt(Instant.now())
                .build();
        try {
            outboxEventRepository.save(OutboxEvent.builder()
                    .aggregateId(applicationId)
                    .eventType(eventType.name())
                    .topic(KafkaTopics.LOAN_EVENTS)
                    .payload(objectMapper.writeValueAsString(payload))
                    .build());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload for application " + applicationId, e);
        }
    }

    private String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }

    private LoanApplicationResponse toApplicationResponse(LoanApplication a, String loanId) {
        return LoanApplicationResponse.builder()
                .id(a.getId())
                .loanType(a.getLoanType())
                .principalAmount(a.getPrincipalAmount())
                .interestRateAnnual(a.getInterestRateAnnual())
                .tenureMonths(a.getTenureMonths())
                .status(a.getStatus())
                .rejectionReason(a.getRejectionReason())
                .loanId(loanId)
                .createdAt(a.getCreatedAt())
                .decidedAt(a.getDecidedAt())
                .build();
    }

    private LoanResponse toLoanResponse(Loan l) {
        return LoanResponse.builder()
                .id(l.getId())
                .loanType(l.getLoanType())
                .principalAmount(l.getPrincipalAmount())
                .interestRateAnnual(l.getInterestRateAnnual())
                .tenureMonths(l.getTenureMonths())
                .emiAmount(l.getEmiAmount())
                .outstandingPrincipal(l.getOutstandingPrincipal())
                .status(l.getStatus())
                .startDate(l.getStartDate())
                .build();
    }

    private EmiScheduleResponse toScheduleResponse(EmiSchedule e) {
        return EmiScheduleResponse.builder()
                .installmentNumber(e.getInstallmentNumber())
                .dueDate(e.getDueDate())
                .emiAmount(e.getEmiAmount())
                .principalComponent(e.getPrincipalComponent())
                .interestComponent(e.getInterestComponent())
                .status(e.getStatus())
                .build();
    }
}
