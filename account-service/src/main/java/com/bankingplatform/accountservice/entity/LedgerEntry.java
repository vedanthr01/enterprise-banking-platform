package com.bankingplatform.accountservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Append-only ledger. Every balance mutation writes one row here in the
 * same DB transaction as the balance update, and the (account_id,
 * reference_id, entry_type) unique constraint is what makes debit/credit/
 * compensate idempotent — a retried call with the same referenceId hits the
 * constraint (or finds the row already there) instead of applying twice.
 */
@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_ledger_account", columnList = "account_id"),
        @Index(name = "idx_ledger_idempotency", columnList = "account_id,reference_id,entry_type", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Column(name = "reference_id", nullable = false, length = 64)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private LedgerEntryType entryType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
