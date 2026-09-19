package com.bankingplatform.auditservice.dto;

import com.bankingplatform.auditservice.entity.AuditDomain;
import com.bankingplatform.auditservice.entity.AuditSeverity;

import java.time.Instant;

/** Every field is optional — an admin can filter by any combination, or
 *  none at all (which just returns everything, paginated). Built by
 *  AuditController from request query parameters. */
public record AuditSearchCriteria(
        AuditDomain domain,
        String eventType,
        String aggregateId,
        String actorUserId,
        AuditSeverity severity,
        Instant from,
        Instant to) {
}
