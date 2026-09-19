package com.bankingplatform.accountservice.controller;

import com.bankingplatform.accountservice.service.AccountService;
import com.bankingplatform.common.dto.AccountBalanceResponse;
import com.bankingplatform.common.dto.CompensateRequest;
import com.bankingplatform.common.dto.CreditRequest;
import com.bankingplatform.common.dto.DebitRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Service-to-service only — see InternalApiKeyFilter. Called by Transaction
 *  Service (transfer saga) and Loan Service (disbursement/EMI debit). */
@RestController
@RequestMapping("/accounts/internal")
@RequiredArgsConstructor
@Tag(name = "Accounts (internal)", description = "Requires X-Internal-Api-Key — not reachable with a user JWT")
public class InternalAccountController {

    private final AccountService accountService;

    @PostMapping("/debit")
    @Operation(summary = "Debit an account as the first leg of a transfer saga")
    public ResponseEntity<AccountBalanceResponse> debit(@Valid @RequestBody DebitRequest request) {
        return ResponseEntity.ok(accountService.debit(request));
    }

    @PostMapping("/credit")
    @Operation(summary = "Credit an account as the second leg of a transfer saga")
    public ResponseEntity<AccountBalanceResponse> credit(@Valid @RequestBody CreditRequest request) {
        return ResponseEntity.ok(accountService.credit(request));
    }

    @PostMapping("/compensate")
    @Operation(summary = "Reverse a prior debit - the saga's compensating transaction")
    public ResponseEntity<AccountBalanceResponse> compensate(@Valid @RequestBody CompensateRequest request) {
        return ResponseEntity.ok(accountService.compensate(request));
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Look up an account's balance/status without the owner's JWT")
    public ResponseEntity<AccountBalanceResponse> getAccount(@PathVariable String accountId) {
        return ResponseEntity.ok(accountService.getCachedBalance(accountId));
    }
}
