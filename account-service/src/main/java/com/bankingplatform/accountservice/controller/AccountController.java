package com.bankingplatform.accountservice.controller;

import com.bankingplatform.accountservice.dto.AccountResponse;
import com.bankingplatform.accountservice.dto.CreateAccountRequest;
import com.bankingplatform.accountservice.dto.CreateFixedDepositRequest;
import com.bankingplatform.accountservice.service.AccountService;
import com.bankingplatform.common.dto.AccountBalanceResponse;
import com.bankingplatform.common.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account creation, balances and fixed deposits")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Open a new savings or current account (KYC must be verified)")
    public ResponseEntity<AccountResponse> createAccount(@AuthenticationPrincipal Jwt jwt,
                                                           @Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(jwt.getSubject(), request));
    }

    @PostMapping("/fixed-deposits")
    @Operation(summary = "Open a fixed deposit, funded from one of your existing accounts")
    public ResponseEntity<AccountResponse> createFixedDeposit(@AuthenticationPrincipal Jwt jwt,
                                                                @Valid @RequestBody CreateFixedDepositRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createFixedDeposit(jwt.getSubject(), request));
    }

    @GetMapping
    @Operation(summary = "List your own accounts")
    public ResponseEntity<List<AccountResponse>> listAccounts(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(accountService.listAccounts(jwt.getSubject()));
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account details (owner or admin only)")
    public ResponseEntity<AccountResponse> getAccount(@AuthenticationPrincipal Jwt jwt, @PathVariable String accountId) {
        AccountResponse account = accountService.getAccount(accountId);
        requireOwnerOrAdmin(jwt, account.getOwnerUserId());
        return ResponseEntity.ok(account);
    }

    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Get current balance (cached briefly in Redis)")
    public ResponseEntity<AccountBalanceResponse> getBalance(@AuthenticationPrincipal Jwt jwt, @PathVariable String accountId) {
        AccountResponse account = accountService.getAccount(accountId);
        requireOwnerOrAdmin(jwt, account.getOwnerUserId());
        return ResponseEntity.ok(accountService.getCachedBalance(accountId));
    }

    private void requireOwnerOrAdmin(Jwt jwt, String ownerUserId) {
        boolean isOwner = jwt.getSubject().equals(ownerUserId);
        List<String> roles = jwt.getClaimAsStringList("roles");
        boolean isAdmin = roles != null && roles.contains("ROLE_ADMIN");
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You do not have access to this account");
        }
    }
}
