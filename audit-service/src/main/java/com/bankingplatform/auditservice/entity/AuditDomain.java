package com.bankingplatform.auditservice.entity;

/** Which upstream service/Kafka topic an audit record originated from. */
public enum AuditDomain {
    ACCOUNT,
    TRANSACTION,
    USER,
    LOAN,
    FRAUD
}
