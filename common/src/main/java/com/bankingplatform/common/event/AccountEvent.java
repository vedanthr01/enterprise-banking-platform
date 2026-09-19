package com.bankingplatform.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/** Published by Account Service to the "account-events" Kafka topic. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountEvent implements Serializable, DomainEvent {
    private String eventId;
    private String accountId;
    private String ownerUserId;
    private AccountEventType eventType;
    private BigDecimal amount;
    private BigDecimal resultingBalance;
    private String referenceId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occurredAt;
}
