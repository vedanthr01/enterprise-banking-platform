package com.bankingplatform.auditservice.entity;

/**
 * A queryable "how noteworthy is this" dimension, independent of domain —
 * lets an admin ask for "every CRITICAL event across the whole platform"
 * without needing to know which domains or event types might contain one.
 */
public enum AuditSeverity {
    INFO,
    WARNING,
    CRITICAL
}
