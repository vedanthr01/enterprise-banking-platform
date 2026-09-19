package com.bankingplatform.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published by Transaction Service to the "transaction-events" Kafka topic
 * via the outbox relay. Consumed by Notification Service (customer-facing
 * alerts), Fraud Detection Service (rule evaluation), and Audit Service
 * (immutable event log).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent implements Serializable, DomainEvent {
    private String eventId;
    private String transactionId;
    private String idempotencyKey;
    private String initiatedByUserId;
    private TransactionEventType eventType;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String reason;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occurredAt;
}
