package com.bankingplatform.auditservice.repository;

import com.bankingplatform.auditservice.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * JpaSpecificationExecutor backs the admin search endpoint's dynamic,
 * arbitrary-combination filtering (see AuditEventSpecifications) — a
 * hand-written findByXAndYAndZ... method per filter combination would need
 * one method per subset of six optional filters (2^6 = 64).
 */
public interface AuditEventRepository extends JpaRepository<AuditEvent, String>, JpaSpecificationExecutor<AuditEvent> {
    boolean existsBySourceEventId(String sourceEventId);
}
