package com.bankingplatform.auditservice.service;

import com.bankingplatform.auditservice.dto.AuditIntegrityCheckResponse;
import com.bankingplatform.auditservice.dto.AuditSearchCriteria;
import com.bankingplatform.auditservice.entity.AuditDomain;
import com.bankingplatform.auditservice.entity.AuditEvent;
import com.bankingplatform.auditservice.entity.AuditSeverity;
import com.bankingplatform.auditservice.repository.AuditEventRepository;
import com.bankingplatform.auditservice.util.AuditHashUtil;
import com.bankingplatform.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditQueryServiceImplTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    private AuditQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuditQueryServiceImpl(auditEventRepository);
    }

    private AuditEvent sampleEvent() {
        Instant now = Instant.parse("2026-08-03T10:15:30Z");
        AuditEvent event = AuditEvent.builder()
                .id("audit-1")
                .sourceEventId("evt-1")
                .domain(AuditDomain.FRAUD)
                .eventType("FRAUD_ALERT_RAISED")
                .aggregateId("txn-1")
                .actorUserId("user-1")
                .severity(AuditSeverity.CRITICAL)
                .description("Fraud rule VELOCITY_CHECK triggered")
                .metadata("{}")
                .occurredAt(now)
                .recordedAt(now)
                .build();
        event.setRecordHash(AuditHashUtil.computeHash(event));
        return event;
    }

    @Test
    void getById_returnsMappedResponse_whenFound() {
        when(auditEventRepository.findById("audit-1")).thenReturn(Optional.of(sampleEvent()));

        var response = service.getById("audit-1");

        assertThat(response.getId()).isEqualTo("audit-1");
        assertThat(response.getDomain()).isEqualTo(AuditDomain.FRAUD);
        assertThat(response.getSeverity()).isEqualTo(AuditSeverity.CRITICAL);
    }

    @Test
    void getById_throwsResourceNotFound_whenMissing() {
        when(auditEventRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("missing")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void verifyIntegrity_reportsIntact_whenStoredHashMatchesRecomputedHash() {
        when(auditEventRepository.findById("audit-1")).thenReturn(Optional.of(sampleEvent()));

        AuditIntegrityCheckResponse result = service.verifyIntegrity("audit-1");

        assertThat(result.isIntact()).isTrue();
        assertThat(result.getStoredHash()).isEqualTo(result.getRecomputedHash());
    }

    @Test
    void verifyIntegrity_reportsTampered_whenStoredHashNoLongerMatches() {
        AuditEvent event = sampleEvent();
        // Simulate a row edited directly in the database, bypassing
        // AuditEventRecorder — the stored hash now reflects a description
        // that no longer matches the (tampered) row.
        event.setDescription("Fraud rule VELOCITY_CHECK triggered".replace("VELOCITY_CHECK", "NONE"));
        when(auditEventRepository.findById("audit-1")).thenReturn(Optional.of(event));

        AuditIntegrityCheckResponse result = service.verifyIntegrity("audit-1");

        assertThat(result.isIntact()).isFalse();
        assertThat(result.getStoredHash()).isNotEqualTo(result.getRecomputedHash());
    }

    @Test
    void search_delegatesToRepositoryWithPageableAndMapsResults() {
        Pageable pageable = PageRequest.of(0, 50);
        Page<AuditEvent> page = new PageImpl<>(List.of(sampleEvent()), pageable, 1);
        when(auditEventRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        AuditSearchCriteria criteria = new AuditSearchCriteria(AuditDomain.FRAUD, null, null, null, null, null, null);
        Page<?> result = service.search(criteria, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
    }
}
