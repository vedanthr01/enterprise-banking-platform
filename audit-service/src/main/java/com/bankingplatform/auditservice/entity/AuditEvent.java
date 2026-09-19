package com.bankingplatform.auditservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One immutable row per platform domain event. Unlike every other entity in
 * this codebase, this one has no @PreUpdate and no update path anywhere in
 * the service layer at all — AuditEventRecorder only ever inserts, never
 * updates, by design (see its Javadoc). The V2 migration backs that
 * application-layer invariant with a database-level one: MySQL triggers
 * reject any UPDATE or DELETE against this table outright, so the guarantee
 * holds even against a direct database connection that bypasses this
 * service entirely.
 *
 * recordHash is a SHA-256 fingerprint over this row's other columns (see
 * AuditHashUtil) — recomputing it and comparing against the stored value
 * (GET /audit/events/{id}/verify) detects any tampering that happened
 * despite the trigger, e.g. from a direct edit executed with elevated
 * privileges that temporarily disabled it.
 */
@Entity
@Table(name = "audit_events", indexes = {
        @Index(name = "idx_audit_source_event", columnList = "source_event_id", unique = true),
        @Index(name = "idx_audit_domain", columnList = "domain"),
        @Index(name = "idx_audit_event_type", columnList = "event_type"),
        @Index(name = "idx_audit_aggregate", columnList = "aggregate_id"),
        @Index(name = "idx_audit_actor", columnList = "actor_user_id"),
        @Index(name = "idx_audit_occurred_at", columnList = "occurred_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "source_event_id", nullable = false, length = 64)
    private String sourceEventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AuditDomain domain;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "aggregate_id", length = 36)
    private String aggregateId;

    @Column(name = "actor_user_id", length = 36)
    private String actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AuditSeverity severity;

    @Column(nullable = false, length = 500)
    private String description;

    /** Full JSON of the source Kafka event, for complete forensic detail
     *  beyond the indexed summary columns above. */
    @Lob
    @Column(nullable = false)
    private String metadata;

    /** When the underlying fact actually happened, per the source event. */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** When Audit Service itself persisted the row — kept distinct from
     *  occurredAt on purpose: consumer lag, replays, and DLT recovery can
     *  all make these differ, and that gap is itself sometimes the thing an
     *  investigator needs to see. */
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "record_hash", nullable = false, length = 64)
    private String recordHash;
}
