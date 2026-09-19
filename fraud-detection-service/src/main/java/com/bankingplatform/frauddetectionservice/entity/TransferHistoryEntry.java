package com.bankingplatform.frauddetectionservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * This service's own local read model of completed transfers, built
 * entirely from the transaction-events it consumes — not a copy of
 * Transaction Service's database and not fetched from it synchronously.
 * Fraud checks run on the hot path of every completed transfer, so a
 * cross-service call (or worse, a shared database) here would couple this
 * service's availability and latency to Transaction Service's; a locally
 * owned projection avoids that entirely, at the cost of eventual
 * consistency, which is an acceptable trade-off for a detective control
 * (see TransactionFraudConsumer's Javadoc).
 *
 * Retained indefinitely, on purpose: unlike a velocity-only ledger (which
 * could safely prune anything outside the rolling window), this table also
 * answers "has this account ever sent money to this beneficiary before,"
 * which needs unbounded history to be correct. Pruning old rows would
 * silently reintroduce false positives (a genuine long-standing beneficiary
 * would look "new" again the moment its oldest transfer aged out). At real
 * production volume the right answer is time-based partitioning with older
 * partitions moved to cold storage rather than deleted — noted here as a
 * scaling path, not implemented, since this project's data volume doesn't
 * need it yet.
 */
@Entity
@Table(name = "transfer_history", indexes = {
        @Index(name = "idx_fraud_history_source_event", columnList = "source_event_id", unique = true),
        @Index(name = "idx_fraud_history_from_account", columnList = "from_account_id"),
        @Index(name = "idx_fraud_history_from_to", columnList = "from_account_id, to_account_id"),
        @Index(name = "idx_fraud_history_occurred_at", columnList = "occurred_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferHistoryEntry {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "source_event_id", nullable = false, length = 64)
    private String sourceEventId;

    @Column(name = "transaction_id", nullable = false, length = 36)
    private String transactionId;

    @Column(name = "from_account_id", nullable = false, length = 36)
    private String fromAccountId;

    @Column(name = "to_account_id", nullable = false, length = 36)
    private String toAccountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
