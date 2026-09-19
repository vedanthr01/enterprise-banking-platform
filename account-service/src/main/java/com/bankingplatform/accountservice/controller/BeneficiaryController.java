package com.bankingplatform.accountservice.controller;

import com.bankingplatform.accountservice.dto.AddBeneficiaryRequest;
import com.bankingplatform.accountservice.dto.BeneficiaryResponse;
import com.bankingplatform.accountservice.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/accounts/beneficiaries")
@RequiredArgsConstructor
@Tag(name = "Beneficiaries", description = "Saved transfer payees")
public class BeneficiaryController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Add a beneficiary")
    public ResponseEntity<BeneficiaryResponse> add(@AuthenticationPrincipal Jwt jwt,
                                                     @Valid @RequestBody AddBeneficiaryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.addBeneficiary(jwt.getSubject(), request));
    }

    @GetMapping
    @Operation(summary = "List your beneficiaries")
    public ResponseEntity<List<BeneficiaryResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(accountService.listBeneficiaries(jwt.getSubject()));
    }

    @DeleteMapping("/{beneficiaryId}")
    @Operation(summary = "Remove a beneficiary")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable String beneficiaryId) {
        accountService.removeBeneficiary(jwt.getSubject(), beneficiaryId);
        return ResponseEntity.noContent().build();
    }
}
