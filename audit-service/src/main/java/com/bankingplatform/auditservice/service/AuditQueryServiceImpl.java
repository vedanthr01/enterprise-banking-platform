package com.bankingplatform.auditservice.service;

import com.bankingplatform.auditservice.dto.AuditEventResponse;
import com.bankingplatform.auditservice.dto.AuditIntegrityCheckResponse;
import com.bankingplatform.auditservice.dto.AuditSearchCriteria;
import com.bankingplatform.auditservice.entity.AuditEvent;
import com.bankingplatform.auditservice.repository.AuditEventRepository;
import com.bankingplatform.auditservice.repository.AuditEventSpecifications;
import com.bankingplatform.auditservice.util.AuditHashUtil;
import com.bankingplatform.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditQueryServiceImpl implements AuditQueryService {

    private final AuditEventRepository auditEventRepository;

    @Override
    public Page<AuditEventResponse> search(AuditSearchCriteria criteria, Pageable pageable) {
        Specification<AuditEvent> spec = Specification
                .where(AuditEventSpecifications.domainIs(criteria.domain()))
                .and(AuditEventSpecifications.eventTypeIs(criteria.eventType()))
                .and(AuditEventSpecifications.aggregateIdIs(criteria.aggregateId()))
                .and(AuditEventSpecifications.actorUserIdIs(criteria.actorUserId()))
                .and(AuditEventSpecifications.severityIs(criteria.severity()))
                .and(AuditEventSpecifications.occurredAtFrom(criteria.from()))
                .and(AuditEventSpecifications.occurredAtTo(criteria.to()));

        return auditEventRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    public AuditEventResponse getById(String id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public AuditIntegrityCheckResponse verifyIntegrity(String id) {
        AuditEvent event = findOrThrow(id);
        String recomputedHash = AuditHashUtil.computeHash(event);
        return AuditIntegrityCheckResponse.builder()
                .id(event.getId())
                .storedHash(event.getRecordHash())
                .recomputedHash(recomputedHash)
                .intact(recomputedHash.equals(event.getRecordHash()))
                .build();
    }

    private AuditEvent findOrThrow(String id) {
        return auditEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No audit event found with id " + id));
    }

    private AuditEventResponse toResponse(AuditEvent e) {
        return AuditEventResponse.builder()
                .id(e.getId())
                .sourceEventId(e.getSourceEventId())
                .domain(e.getDomain())
                .eventType(e.getEventType())
                .aggregateId(e.getAggregateId())
                .actorUserId(e.getActorUserId())
                .severity(e.getSeverity())
                .description(e.getDescription())
                .metadata(e.getMetadata())
                .occurredAt(e.getOccurredAt())
                .recordedAt(e.getRecordedAt())
                .recordHash(e.getRecordHash())
                .build();
    }
}
