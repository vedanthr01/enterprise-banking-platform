package com.bankingplatform.auditservice.controller;

import com.bankingplatform.auditservice.dto.AuditEventResponse;
import com.bankingplatform.auditservice.dto.AuditIntegrityCheckResponse;
import com.bankingplatform.auditservice.dto.AuditSearchCriteria;
import com.bankingplatform.auditservice.entity.AuditDomain;
import com.bankingplatform.auditservice.entity.AuditSeverity;
import com.bankingplatform.auditservice.service.AuditQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Every endpoint here is admin-only — enforced twice, deliberately: the
 * SecurityFilterChain already rejects any non-ROLE_ADMIN caller for this
 * whole service, and @PreAuthorize repeats the same rule at the method
 * level so the intent reads correctly from this class alone too, matching
 * every other admin endpoint in the platform (see e.g. NotificationController).
 */
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Admin-only, immutable platform-wide audit trail")
public class AuditController {

    private final AuditQueryService auditQueryService;

    @GetMapping("/events")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Search the audit trail by any combination of domain, event type, aggregate id, actor, severity, and time range")
    public ResponseEntity<PagedModel<AuditEventResponse>> search(
            @RequestParam(required = false) AuditDomain domain,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String aggregateId,
            @RequestParam(required = false) String actorUserId,
            @RequestParam(required = false) AuditSeverity severity,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 50, sort = "occurredAt") Pageable pageable) {
        AuditSearchCriteria criteria = new AuditSearchCriteria(domain, eventType, aggregateId, actorUserId, severity, from, to);
        Page<AuditEventResponse> page = auditQueryService.search(criteria, pageable);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @GetMapping("/events/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Fetch a single audit record by id")
    public ResponseEntity<AuditEventResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(auditQueryService.getById(id));
    }

    @GetMapping("/events/{id}/verify")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Recompute a record's SHA-256 integrity hash and compare it against the stored value")
    public ResponseEntity<AuditIntegrityCheckResponse> verifyIntegrity(@PathVariable String id) {
        return ResponseEntity.ok(auditQueryService.verifyIntegrity(id));
    }
}
