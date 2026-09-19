package com.bankingplatform.auditservice.util;

import com.bankingplatform.auditservice.entity.AuditDomain;
import com.bankingplatform.auditservice.entity.AuditEvent;
import com.bankingplatform.auditservice.entity.AuditSeverity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AuditHashUtilTest {

    private AuditEvent.AuditEventBuilder baseEvent() {
        Instant now = Instant.parse("2026-08-03T10:15:30Z");
        return AuditEvent.builder()
                .id("audit-1")
                .sourceEventId("evt-1")
                .domain(AuditDomain.TRANSACTION)
                .eventType("TRANSFER_COMPLETED")
                .aggregateId("txn-1")
                .actorUserId("user-1")
                .severity(AuditSeverity.INFO)
                .description("Transfer txn-1 completed successfully")
                .metadata("{\"amount\":100}")
                .occurredAt(now)
                .recordedAt(now);
    }

    @Test
    void computeHash_isDeterministic_forIdenticalInput() {
        AuditEvent event = baseEvent().build();

        String hash1 = AuditHashUtil.computeHash(event);
        String hash2 = AuditHashUtil.computeHash(event);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void computeHash_isSixtyFourHexCharacters() {
        String hash = AuditHashUtil.computeHash(baseEvent().build());

        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void computeHash_changes_whenDescriptionIsTampered() {
        AuditEvent original = baseEvent().build();
        AuditEvent tampered = baseEvent().description("Transfer txn-1 completed successfully — but altered").build();

        assertThat(AuditHashUtil.computeHash(tampered)).isNotEqualTo(AuditHashUtil.computeHash(original));
    }

    @Test
    void computeHash_changes_whenSeverityIsTampered() {
        // A row's severity is exactly the kind of field someone might quietly
        // downgrade (CRITICAL -> INFO) to bury a fraud alert — confirming the
        // hash reacts to it is the whole point of hashing every column.
        AuditEvent original = baseEvent().severity(AuditSeverity.CRITICAL).build();
        AuditEvent tampered = baseEvent().severity(AuditSeverity.INFO).build();

        assertThat(AuditHashUtil.computeHash(tampered)).isNotEqualTo(AuditHashUtil.computeHash(original));
    }
}
