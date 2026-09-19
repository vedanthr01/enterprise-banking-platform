package com.bankingplatform.auditservice.service;

import com.bankingplatform.auditservice.entity.AuditDomain;
import com.bankingplatform.auditservice.entity.AuditEvent;
import com.bankingplatform.auditservice.entity.AuditSeverity;
import com.bankingplatform.auditservice.repository.AuditEventRepository;
import com.bankingplatform.auditservice.util.AuditHashUtil;
import com.bankingplatform.common.event.DomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * The single write path every one of the five domain consumers routes
 * through — mirrors NotificationDispatcher's role in Notification Service:
 * the idempotency guard, field mapping, hashing, and persistence all happen
 * exactly once, in one place, so each of those concerns is implemented —
 * and tested — once.
 *
 * Insert-only by design: there is deliberately no update/delete method
 * anywhere on this class, matching AuditEvent's own contract (see its
 * Javadoc) and backed at the database layer by the V2 migration's triggers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditEventRecorder {

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void record(DomainEvent sourceEvent, AuditDomain domain, String eventType, String aggregateId,
                        String actorUserId, AuditSeverity severity, String description) {
        if (auditEventRepository.existsBySourceEventId(sourceEvent.getEventId())) {
            log.debug("Audit record for source event {} already exists — skipping duplicate (at-least-once replay)",
                    sourceEvent.getEventId());
            return;
        }

        AuditEvent event = AuditEvent.builder()
                .id(UUID.randomUUID().toString())
                .sourceEventId(sourceEvent.getEventId())
                .domain(domain)
                .eventType(eventType)
                .aggregateId(aggregateId)
                .actorUserId(actorUserId)
                .severity(severity)
                .description(description)
                .metadata(serialize(sourceEvent))
                .occurredAt(sourceEvent.getOccurredAt())
                .recordedAt(Instant.now())
                .build();
        event.setRecordHash(AuditHashUtil.computeHash(event));

        auditEventRepository.save(event);
    }

    private String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize source event payload for audit metadata (eventId={}) — "
                    + "storing a placeholder so the audit row is still written", event.getEventId(), ex);
            return "{\"error\":\"serialization failed\"}";
        }
    }
}
