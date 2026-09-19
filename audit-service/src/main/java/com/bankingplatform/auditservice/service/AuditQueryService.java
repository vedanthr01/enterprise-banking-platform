package com.bankingplatform.auditservice.service;

import com.bankingplatform.auditservice.dto.AuditEventResponse;
import com.bankingplatform.auditservice.dto.AuditIntegrityCheckResponse;
import com.bankingplatform.auditservice.dto.AuditSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditQueryService {
    Page<AuditEventResponse> search(AuditSearchCriteria criteria, Pageable pageable);
    AuditEventResponse getById(String id);
    AuditIntegrityCheckResponse verifyIntegrity(String id);
}
