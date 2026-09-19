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
 * Published by Fraud Detection Service to the "fraud-alerts" topic when a
 * rule-based check flags a transaction. Consumed by Notification Service
 * (customer-facing alert) and Audit Service (immutable event log).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlertEvent implements Serializable, DomainEvent {
    private String eventId;
    private String transactionId;
    private String accountId;
    private String userId;
    private FraudRuleType ruleTriggered;
    private int riskScore;
    private BigDecimal amount;
    private String description;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occurredAt;
}
