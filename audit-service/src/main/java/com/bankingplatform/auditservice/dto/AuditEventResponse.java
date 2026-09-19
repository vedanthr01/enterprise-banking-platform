package com.bankingplatform.auditservice.dto;

import com.bankingplatform.auditservice.entity.AuditDomain;
import com.bankingplatform.auditservice.entity.AuditSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventResponse {
    private String id;
    private String sourceEventId;
    private AuditDomain domain;
    private String eventType;
    private String aggregateId;
    private String actorUserId;
    private AuditSeverity severity;
    private String description;
    private String metadata;
    private Instant occurredAt;
    private Instant recordedAt;
    private String recordHash;
}
