package com.bankingplatform.loanservice.controller;

import com.bankingplatform.loanservice.dto.EmiScheduleResponse;
import com.bankingplatform.loanservice.dto.LoanApplicationRequest;
import com.bankingplatform.loanservice.dto.LoanApplicationResponse;
import com.bankingplatform.loanservice.dto.LoanResponse;
import com.bankingplatform.loanservice.service.LoanApplicationService;
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
@RequestMapping("/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Loan applications, disbursement, and EMI schedules")
public class LoanController {

    private final LoanApplicationService loanApplicationService;

    @PostMapping("/applications")
    @Operation(summary = "Apply for a loan — synchronously underwritten and, if approved, disbursed")
    public ResponseEntity<LoanApplicationResponse> apply(@AuthenticationPrincipal Jwt jwt,
                                                           @Valid @RequestBody LoanApplicationRequest request) {
        return ResponseEntity.ok(loanApplicationService.apply(jwt.getSubject(), request));
    }

    @GetMapping("/applications/{applicationId}")
    @Operation(summary = "Get a loan application (applicant or admin only)")
    public ResponseEntity<LoanApplicationResponse> getApplication(@AuthenticationPrincipal Jwt jwt,
                                                                    @PathVariable String applicationId) {
        return ResponseEntity.ok(loanApplicationService.getApplication(applicationId, jwt.getSubject(), isAdmin(jwt)));
    }

    @GetMapping("/applications")
    @Operation(summary = "List your own loan applications")
    public ResponseEntity<List<LoanApplicationResponse>> myApplications(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(loanApplicationService.getMyApplications(jwt.getSubject()));
    }

    @GetMapping("/{loanId}")
    @Operation(summary = "Get a disbursed loan's details (borrower or admin only)")
    public ResponseEntity<LoanResponse> getLoan(@AuthenticationPrincipal Jwt jwt, @PathVariable String loanId) {
        return ResponseEntity.ok(loanApplicationService.getLoan(loanId, jwt.getSubject(), isAdmin(jwt)));
    }

    @GetMapping("/{loanId}/schedule")
    @Operation(summary = "Get a loan's EMI schedule (borrower or admin only)")
    public ResponseEntity<List<EmiScheduleResponse>> getSchedule(@AuthenticationPrincipal Jwt jwt, @PathVariable String loanId) {
        return ResponseEntity.ok(loanApplicationService.getSchedule(loanId, jwt.getSubject(), isAdmin(jwt)));
    }

    private boolean isAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("ROLE_ADMIN");
    }
}
