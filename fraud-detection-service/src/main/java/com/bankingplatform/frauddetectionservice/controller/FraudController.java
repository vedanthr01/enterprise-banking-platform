package com.bankingplatform.frauddetectionservice.controller;

import com.bankingplatform.frauddetectionservice.config.FraudRuleProperties;
import com.bankingplatform.frauddetectionservice.dto.TransferHistoryResponse;
import com.bankingplatform.frauddetectionservice.service.TransferHistoryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Deliberately does not expose a "list fraud alerts" endpoint — Audit
 * Service's generic GET /audit/events?domain=FRAUD search already answers
 * "what fraud alerts have fired," since FraudAuditConsumer records every
 * one of them. Duplicating that query here would just be a second,
 * divergent source of truth for the same facts.
 */
@RestController
@RequestMapping("/fraud")
@RequiredArgsConstructor
@Tag(name = "Fraud", description = "Admin-only fraud rule configuration and transfer-history ledger")
public class FraudController {

    private final FraudRuleProperties fraudRuleProperties;
    private final TransferHistoryQueryService transferHistoryQueryService;

    @GetMapping("/rules/config")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "The fraud rule thresholds currently active on this environment")
    public ResponseEntity<FraudRuleProperties> getRuleConfig() {
        return ResponseEntity.ok(fraudRuleProperties);
    }

    @GetMapping("/history/{accountId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Admin-only: the local transfer-history ledger this service screens against for a given account")
    public ResponseEntity<List<TransferHistoryResponse>> getHistory(@PathVariable String accountId) {
        return ResponseEntity.ok(transferHistoryQueryService.getForAccount(accountId));
    }
}
