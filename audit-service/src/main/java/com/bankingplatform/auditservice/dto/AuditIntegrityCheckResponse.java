package com.bankingplatform.auditservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditIntegrityCheckResponse {
    private String id;
    private boolean intact;
    private String storedHash;
    private String recomputedHash;
}
