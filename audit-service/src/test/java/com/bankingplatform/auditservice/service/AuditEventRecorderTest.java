package com.bankingplatform.auditservice.service;

import com.bankingplatform.auditservice.entity.AuditDomain;
import com.bankingplatform.auditservice.entity.AuditEvent;
import com.bankingplatform.auditservice.entity.AuditSeverity;
import com.bankingplatform.auditservice.repository.AuditEventRepository;
import com.bankingplatform.auditservice.util.AuditHashUtil;
import com.bankingplatform.common.event.TransactionEvent;
import com.bankingplatform.common.event.TransactionEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventRecorderTest {

    @Mock
    private AuditEventRepository auditEventRepository;
    @Mock
    private ObjectMapper objectMapper;

    private AuditEventRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new AuditEventRecorder(auditEventRepository, objectMapper);
    }

    private TransactionEvent sourceEvent() {
        return TransactionEvent.builder()
                .eventId("evt-1")
                .transactionId("txn-1")
                .initiatedByUserId("user-1")
                .eventType(TransactionEventType.TRANSFER_COMPLETED)
                .amount(new BigDecimal("250.00"))
                .occurredAt(Instant.parse("2026-08-03T10:15:30Z"))
                .build();
    }

    @Test
    void record_persistsRow_withFieldsMapped_andASelfConsistentHash() throws Exception {
        when(auditEventRepository.existsBySourceEventId("evt-1")).thenReturn(false);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"transactionId\":\"txn-1\"}");

        recorder.record(sourceEvent(), AuditDomain.TRANSACTION, "TRANSFER_COMPLETED", "txn-1", "user-1",
                AuditSeverity.INFO, "Transfer txn-1 completed successfully");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent saved = captor.getValue();

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getSourceEventId()).isEqualTo("evt-1");
        assertThat(saved.getDomain()).isEqualTo(AuditDomain.TRANSACTION);
        assertThat(saved.getEventType()).isEqualTo("TRANSFER_COMPLETED");
        assertThat(saved.getAggregateId()).isEqualTo("txn-1");
        assertThat(saved.getActorUserId()).isEqualTo("user-1");
        assertThat(saved.getSeverity()).isEqualTo(AuditSeverity.INFO);
        assertThat(saved.getDescription()).isEqualTo("Transfer txn-1 completed successfully");
        assertThat(saved.getMetadata()).isEqualTo("{\"transactionId\":\"txn-1\"}");
        assertThat(saved.getOccurredAt()).isEqualTo(Instant.parse("2026-08-03T10:15:30Z"));
        assertThat(saved.getRecordedAt()).isNotNull();
        // The hash stored alongside the row must match what re-deriving it
        // from that same row's columns produces — the exact check
        // AuditQueryServiceImpl#verifyIntegrity performs later.
        assertThat(saved.getRecordHash()).isEqualTo(AuditHashUtil.computeHash(saved));
    }

    @Test
    void record_skipsPersistence_whenSourceEventAlreadyRecorded() {
        when(auditEventRepository.existsBySourceEventId("evt-1")).thenReturn(true);

        recorder.record(sourceEvent(), AuditDomain.TRANSACTION, "TRANSFER_COMPLETED", "txn-1", "user-1",
                AuditSeverity.INFO, "Transfer txn-1 completed successfully");

        verify(auditEventRepository, never()).save(any());
    }

    @Test
    void record_storesPlaceholderMetadata_ratherThanFailing_whenSerializationErrors() throws Exception {
        when(auditEventRepository.existsBySourceEventId("evt-1")).thenReturn(false);
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});

        recorder.record(sourceEvent(), AuditDomain.TRANSACTION, "TRANSFER_COMPLETED", "txn-1", "user-1",
                AuditSeverity.INFO, "Transfer txn-1 completed successfully");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        // A broken serializer must not stop the audit row itself from being
        // written — losing the summary fields (domain/type/aggregate/actor)
        // over a metadata-blob problem would be a worse outcome than a
        // placeholder blob.
        assertThat(captor.getValue().getMetadata()).contains("serialization failed");
    }
}
