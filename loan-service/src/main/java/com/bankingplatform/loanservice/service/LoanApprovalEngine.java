package com.bankingplatform.loanservice.service;

import com.bankingplatform.loanservice.dto.LoanApplicationRequest;
import com.bankingplatform.loanservice.entity.LoanStatus;
import com.bankingplatform.loanservice.entity.LoanType;
import com.bankingplatform.loanservice.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * Deliberately simple, explainable, deterministic underwriting rules — the
 * same philosophy as Fraud Detection Service's rule engine. In a regulated
 * lending domain, a decision that can't be explained to a rejected
 * applicant is a compliance liability, not just an engineering shortcut
 * standing in for "add a credit-scoring model later" — this is the
 * permanent design, not a placeholder.
 */
@Component
@RequiredArgsConstructor
public class LoanApprovalEngine {

    private static final Map<LoanType, BigDecimal> MAX_AMOUNT = new EnumMap<>(LoanType.class);
    private static final Map<LoanType, int[]> TENURE_BOUNDS_MONTHS = new EnumMap<>(LoanType.class);

    static {
        MAX_AMOUNT.put(LoanType.PERSONAL, new BigDecimal("500000"));
        MAX_AMOUNT.put(LoanType.AUTO, new BigDecimal("1500000"));
        MAX_AMOUNT.put(LoanType.EDUCATION, new BigDecimal("2000000"));
        MAX_AMOUNT.put(LoanType.HOME, new BigDecimal("5000000"));

        TENURE_BOUNDS_MONTHS.put(LoanType.PERSONAL, new int[]{6, 60});
        TENURE_BOUNDS_MONTHS.put(LoanType.AUTO, new int[]{12, 84});
        TENURE_BOUNDS_MONTHS.put(LoanType.EDUCATION, new int[]{12, 120});
        TENURE_BOUNDS_MONTHS.put(LoanType.HOME, new int[]{12, 360});
    }

    private final LoanRepository loanRepository;

    public Decision evaluate(String applicantUserId, LoanApplicationRequest request) {
        BigDecimal maxAmount = MAX_AMOUNT.get(request.getLoanType());
        if (request.getPrincipalAmount().compareTo(maxAmount) > 0) {
            return Decision.reject("Requested amount exceeds the maximum of " + maxAmount
                    + " for " + request.getLoanType() + " loans");
        }

        int[] bounds = TENURE_BOUNDS_MONTHS.get(request.getLoanType());
        if (request.getTenureMonths() < bounds[0] || request.getTenureMonths() > bounds[1]) {
            return Decision.reject("Tenure must be between " + bounds[0] + " and " + bounds[1]
                    + " months for " + request.getLoanType() + " loans");
        }

        boolean hasActiveLoanOfSameType = loanRepository.existsByBorrowerUserIdAndLoanTypeAndStatus(
                applicantUserId, request.getLoanType(), LoanStatus.ACTIVE);
        if (hasActiveLoanOfSameType) {
            return Decision.reject("An active " + request.getLoanType() + " loan already exists for this applicant");
        }

        return Decision.approve();
    }

    public record Decision(boolean approved, String reason) {
        public static Decision approve() {
            return new Decision(true, null);
        }

        public static Decision reject(String reason) {
            return new Decision(false, reason);
        }
    }
}
