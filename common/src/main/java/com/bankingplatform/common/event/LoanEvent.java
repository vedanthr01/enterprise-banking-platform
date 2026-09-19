package com.bankingplatform.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/** Published by Loan Service to the "loan-events" topic across the whole
 *  application/disbursement/repayment lifecycle. Consumed by Notification
 *  Service (customer alerts) and Audit Service. loanId is null for
 *  events raised before disbursement (a loan doesn't exist yet at
 *  application-rejection time). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanEvent implements Serializable, DomainEvent {
    private String eventId;
    private String applicationId;
    private String loanId;
    private String borrowerUserId;
    private LoanEventType eventType;
    private BigDecimal amount;
    private String reason;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occurredAt;
}
