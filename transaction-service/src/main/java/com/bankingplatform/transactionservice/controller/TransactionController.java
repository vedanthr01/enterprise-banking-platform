package com.bankingplatform.transactionservice.controller;

import com.bankingplatform.transactionservice.dto.TransactionResponse;
import com.bankingplatform.transactionservice.dto.TransferRequest;
import com.bankingplatform.transactionservice.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Fund transfers and transaction history")
public class TransactionController {

    private final TransferService transferService;

    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds between two accounts (Saga-orchestrated, idempotent)")
    public ResponseEntity<TransactionResponse> transfer(@AuthenticationPrincipal Jwt jwt,
                                                          @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(transferService.transfer(jwt.getSubject(), request));
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get a transaction (initiator or admin only)")
    public ResponseEntity<TransactionResponse> getTransaction(@AuthenticationPrincipal Jwt jwt,
                                                                @PathVariable String transactionId) {
        boolean isAdmin = isAdmin(jwt);
        return ResponseEntity.ok(transferService.getTransaction(transactionId, jwt.getSubject(), isAdmin));
    }

    @GetMapping("/history")
    @Operation(summary = "List transfers you've initiated")
    public ResponseEntity<List<TransactionResponse>> history(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(transferService.getHistory(jwt.getSubject()));
    }

    private boolean isAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("ROLE_ADMIN");
    }
}
