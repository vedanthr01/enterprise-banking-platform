package com.bankingplatform.transactionservice.service;

import com.bankingplatform.common.constants.KafkaTopics;
import com.bankingplatform.common.dto.CompensateRequest;
import com.bankingplatform.common.dto.CreditRequest;
import com.bankingplatform.common.dto.DebitRequest;
import com.bankingplatform.common.event.TransactionEvent;
import com.bankingplatform.common.event.TransactionEventType;
import com.bankingplatform.common.exception.BusinessException;
import com.bankingplatform.common.exception.ResourceNotFoundException;
import com.bankingplatform.common.exception.UnauthorizedException;
import com.bankingplatform.transactionservice.client.AccountServiceGateway;
import com.bankingplatform.transactionservice.dto.TransactionResponse;
import com.bankingplatform.transactionservice.dto.TransferRequest;
import com.bankingplatform.transactionservice.entity.OutboxEvent;
import com.bankingplatform.transactionservice.entity.Transaction;
import com.bankingplatform.transactionservice.entity.TransactionStatus;
import com.bankingplatform.transactionservice.repository.OutboxEventRepository;
import com.bankingplatform.transactionservice.repository.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrated Saga for a two-account transfer: debit the source, then
 * credit the destination. If the credit leg fails after the debit already
 * succeeded, this issues an explicit compensating transaction to reverse
 * the debit — money never silently vanishes. Every outcome (including the
 * rare case where the compensation itself fails) is persisted and recorded
 * as an outbox event before returning.
 *
 * Orchestration (a central coordinator making the calls, as opposed to
 * choreography via events) was chosen deliberately here: it keeps the
 * "what happens next" logic in one place, in plain code, that's easy to
 * reason about and unit test — worth the trade-off of Transaction Service
 * knowing about Account Service, given only two participants are involved.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AccountServiceGateway accountServiceGateway;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public TransactionResponse transfer(String initiatedByUserId, TransferRequest request) {
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            log.info("Idempotent replay for key {} — returning existing transaction {}",
                    request.getIdempotencyKey(), existing.get().getId());
            return toResponse(existing.get());
        }

        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new BusinessException("Cannot transfer to the same account");
        }

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .idempotencyKey(request.getIdempotencyKey())
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(TransactionStatus.INITIATED)
                .initiatedByUserId(initiatedByUserId)
                .build();
        transactionRepository.save(transaction);
        writeOutboxEvent(transaction, TransactionEventType.TRANSFER_INITIATED, null);

        // -- Saga step 1: debit the source account -------------------------
        try {
            accountServiceGateway.debit(DebitRequest.builder()
                    .accountId(request.getFromAccountId())
                    .amount(request.getAmount())
                    .referenceId(transaction.getId())
                    .build());
        } catch (Exception ex) {
            return failTransaction(transaction, "Debit failed: " + rootMessage(ex));
        }

        // -- Saga step 2: credit the destination account -------------------
        try {
            accountServiceGateway.credit(CreditRequest.builder()
                    .accountId(request.getToAccountId())
                    .amount(request.getAmount())
                    .referenceId(transaction.getId())
                    .build());
        } catch (Exception ex) {
            return compensateTransaction(transaction, "Credit failed: " + rootMessage(ex));
        }

        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);
        writeOutboxEvent(transaction, TransactionEventType.TRANSFER_COMPLETED, null);

        return toResponse(transaction);
    }

    @Override
    public TransactionResponse getTransaction(String transactionId, String requesterUserId, boolean isAdmin) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
        if (!isAdmin && !transaction.getInitiatedByUserId().equals(requesterUserId)) {
            throw new UnauthorizedException("You do not have access to this transaction");
        }
        return toResponse(transaction);
    }

    @Override
    public List<TransactionResponse> getHistory(String requesterUserId) {
        // Scoped to transfers the caller themselves initiated. A fuller
        // implementation would also surface incoming transfers to the
        // receiving party, which needs an account-ownership check against
        // Account Service — left as a documented follow-up (see README).
        return transactionRepository.findByInitiatedByUserIdOrderByCreatedAtDesc(requesterUserId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // -- Saga failure paths --------------------------------------------------

    private TransactionResponse failTransaction(Transaction transaction, String reason) {
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setFailureReason(reason);
        transactionRepository.save(transaction);
        writeOutboxEvent(transaction, TransactionEventType.TRANSFER_FAILED, reason);
        return toResponse(transaction);
    }

    private TransactionResponse compensateTransaction(Transaction transaction, String creditFailureReason) {
        try {
            accountServiceGateway.compensate(CompensateRequest.builder()
                    .accountId(transaction.getFromAccountId())
                    .amount(transaction.getAmount())
                    .referenceId(transaction.getId())
                    .reason(creditFailureReason)
                    .build());

            transaction.setStatus(TransactionStatus.COMPENSATED);
            transaction.setFailureReason(creditFailureReason);
            transactionRepository.save(transaction);
            writeOutboxEvent(transaction, TransactionEventType.TRANSFER_COMPENSATED, creditFailureReason);
            return toResponse(transaction);

        } catch (Exception compensateEx) {
            // Worst case in the whole saga: the debit succeeded, the credit
            // failed, and reversing the debit ALSO failed. Money is now
            // stuck mid-flight. Don't paper over it — mark it loudly so an
            // operator has to look, rather than silently losing track.
            String criticalReason = "CRITICAL: credit failed (" + creditFailureReason
                    + ") and the compensating reversal ALSO failed (" + rootMessage(compensateEx)
                    + ") — requires manual reconciliation";
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(criticalReason);
            transactionRepository.save(transaction);
            writeOutboxEvent(transaction, TransactionEventType.TRANSFER_FAILED, criticalReason);
            log.error("UNRECOVERED SAGA FAILURE for transaction {}: {}", transaction.getId(), criticalReason);
            return toResponse(transaction);
        }
    }

    // -- outbox + mapping ------------------------------------------------------

    private void writeOutboxEvent(Transaction transaction, TransactionEventType eventType, String reason) {
        TransactionEvent payload = TransactionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .transactionId(transaction.getId())
                .idempotencyKey(transaction.getIdempotencyKey())
                .initiatedByUserId(transaction.getInitiatedByUserId())
                .eventType(eventType)
                .fromAccountId(transaction.getFromAccountId())
                .toAccountId(transaction.getToAccountId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus().name())
                .reason(reason)
                .occurredAt(Instant.now())
                .build();
        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(transaction.getId())
                    .eventType(eventType.name())
                    .topic(KafkaTopics.TRANSACTION_EVENTS)
                    .payload(objectMapper.writeValueAsString(payload))
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            // Serialization of our own DTO failing means a programming error,
            // not a transient issue — fail loudly rather than silently drop the event.
            throw new IllegalStateException("Failed to serialize outbox payload for transaction " + transaction.getId(), e);
        }
    }

    private String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }

    private TransactionResponse toResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .fromAccountId(t.getFromAccountId())
                .toAccountId(t.getToAccountId())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .status(t.getStatus())
                .failureReason(t.getFailureReason())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
