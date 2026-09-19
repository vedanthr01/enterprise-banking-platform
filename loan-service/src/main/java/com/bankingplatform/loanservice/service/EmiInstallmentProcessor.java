package com.bankingplatform.loanservice.service;

import com.bankingplatform.common.constants.KafkaTopics;
import com.bankingplatform.common.dto.DebitRequest;
import com.bankingplatform.common.event.LoanEvent;
import com.bankingplatform.common.event.LoanEventType;
import com.bankingplatform.loanservice.client.AccountServiceGateway;
import com.bankingplatform.loanservice.entity.EmiSchedule;
import com.bankingplatform.loanservice.entity.EmiStatus;
import com.bankingplatform.loanservice.entity.Loan;
import com.bankingplatform.loanservice.entity.LoanStatus;
import com.bankingplatform.loanservice.entity.OutboxEvent;
import com.bankingplatform.loanservice.repository.EmiScheduleRepository;
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
import java.util.UUID;

/**
 * One EMI installment, one transaction: each call is its own unit of work
 * so the scheduler's batch loop never rolls back already-processed
 * installments just because a later one in the same run fails. Own bean
 * (not a method on EmiAutoDebitScheduler) so @Transactional actually goes
 * through the Spring AOP proxy — the same self-invocation concern
 * documented throughout this codebase. Takes an id and re-fetches rather
 * than accepting a JPA entity from the caller, since the caller's own query
 * runs outside any transaction of its own and the entities it returns are
 * detached.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmiInstallmentProcessor {

    private final EmiScheduleRepository emiScheduleRepository;
    private final LoanRepository loanRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AccountServiceGateway accountServiceGateway;
    private final ObjectMapper objectMapper;

    @Transactional
    public void process(String emiScheduleId) {
        EmiSchedule installment = emiScheduleRepository.findById(emiScheduleId).orElse(null);
        if (installment == null || installment.getStatus() == EmiStatus.PAID) {
            return; // already handled, or vanished — nothing to do
        }

        Loan loan = loanRepository.findById(installment.getLoanId()).orElse(null);
        if (loan == null || loan.getStatus() != LoanStatus.ACTIVE) {
            log.warn("Skipping EMI {} — loan {} is not active", installment.getId(), installment.getLoanId());
            return;
        }

        try {
            accountServiceGateway.debit(DebitRequest.builder()
                    .accountId(loan.getDisbursementAccountId())
                    .amount(installment.getEmiAmount())
                    .referenceId(installment.getId())
                    .build());
        } catch (Exception ex) {
            installment.setStatus(EmiStatus.OVERDUE);
            emiScheduleRepository.save(installment);
            log.warn("EMI auto-debit failed for installment {} (loan {}): {}",
                    installment.getId(), loan.getId(), rootMessage(ex));
            writeOutboxEvent(loan, installment.getEmiAmount(), LoanEventType.EMI_PAYMENT_FAILED,
                    "EMI auto-debit failed: " + rootMessage(ex));
            return;
        }

        installment.setStatus(EmiStatus.PAID);
        installment.setPaidAt(Instant.now());
        emiScheduleRepository.save(installment);

        loan.setOutstandingPrincipal(loan.getOutstandingPrincipal().subtract(installment.getPrincipalComponent()));
        boolean justClosed = loan.getOutstandingPrincipal().signum() <= 0;
        if (justClosed) {
            loan.setStatus(LoanStatus.CLOSED);
        }
        loanRepository.save(loan);

        writeOutboxEvent(loan, installment.getEmiAmount(), LoanEventType.EMI_PAYMENT_SUCCEEDED, null);
        if (justClosed) {
            writeOutboxEvent(loan, BigDecimal.ZERO, LoanEventType.LOAN_CLOSED, "Final installment paid");
        }
    }

    private void writeOutboxEvent(Loan loan, BigDecimal amount, LoanEventType eventType, String reason) {
        LoanEvent payload = LoanEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .applicationId(loan.getApplicationId())
                .loanId(loan.getId())
                .borrowerUserId(loan.getBorrowerUserId())
                .eventType(eventType)
                .amount(amount)
                .reason(reason)
                .occurredAt(Instant.now())
                .build();
        try {
            outboxEventRepository.save(OutboxEvent.builder()
                    .aggregateId(loan.getId())
                    .eventType(eventType.name())
                    .topic(KafkaTopics.LOAN_EVENTS)
                    .payload(objectMapper.writeValueAsString(payload))
                    .build());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload for loan " + loan.getId(), e);
        }
    }

    private String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
