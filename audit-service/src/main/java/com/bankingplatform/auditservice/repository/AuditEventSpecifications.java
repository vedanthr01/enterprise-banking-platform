package com.bankingplatform.auditservice.repository;

import com.bankingplatform.auditservice.entity.AuditDomain;
import com.bankingplatform.auditservice.entity.AuditEvent;
import com.bankingplatform.auditservice.entity.AuditSeverity;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

/**
 * Each factory returns a Specification that contributes one predicate, or
 * null (Spring Data's documented "no-op" convention for Specification.and())
 * when its filter value wasn't supplied. AuditQueryServiceImpl chains all
 * six with .and(...) regardless of which are null, so a search with zero,
 * one, or all six filters set is handled by the exact same code path.
 */
public final class AuditEventSpecifications {

    private AuditEventSpecifications() {}

    public static Specification<AuditEvent> domainIs(AuditDomain domain) {
        return (root, query, cb) -> domain == null ? null : cb.equal(root.get("domain"), domain);
    }

    public static Specification<AuditEvent> eventTypeIs(String eventType) {
        return (root, query, cb) -> eventType == null ? null : cb.equal(root.get("eventType"), eventType);
    }

    public static Specification<AuditEvent> aggregateIdIs(String aggregateId) {
        return (root, query, cb) -> aggregateId == null ? null : cb.equal(root.get("aggregateId"), aggregateId);
    }

    public static Specification<AuditEvent> actorUserIdIs(String actorUserId) {
        return (root, query, cb) -> actorUserId == null ? null : cb.equal(root.get("actorUserId"), actorUserId);
    }

    public static Specification<AuditEvent> severityIs(AuditSeverity severity) {
        return (root, query, cb) -> severity == null ? null : cb.equal(root.get("severity"), severity);
    }

    public static Specification<AuditEvent> occurredAtFrom(Instant from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("occurredAt"), from);
    }

    public static Specification<AuditEvent> occurredAtTo(Instant to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("occurredAt"), to);
    }
}
