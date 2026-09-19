package com.bankingplatform.loanservice.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Standard reducing-balance EMI amortization. The compounding term is
 * computed in double precision — there's no closed-form BigDecimal power
 * operation without pulling in a big-math library — and the results are
 * rounded back to currency precision immediately after. The final
 * installment absorbs any residual rounding drift so the schedule always
 * sums to exactly the principal, the same simplification real EMI
 * calculators make.
 */
@Component
public class EmiCalculator {

    public BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualRatePercent, int tenureMonths) {
        double monthlyRate = annualRatePercent.doubleValue() / 12.0 / 100.0;
        if (monthlyRate == 0.0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }
        double p = principal.doubleValue();
        double factor = Math.pow(1 + monthlyRate, tenureMonths);
        double emi = p * monthlyRate * factor / (factor - 1);
        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }

    public List<InstallmentBreakdown> generateSchedule(BigDecimal principal, BigDecimal annualRatePercent, int tenureMonths) {
        BigDecimal emi = calculateEmi(principal, annualRatePercent, tenureMonths);
        double monthlyRate = annualRatePercent.doubleValue() / 12.0 / 100.0;

        List<InstallmentBreakdown> schedule = new ArrayList<>(tenureMonths);
        BigDecimal outstanding = principal;
        for (int i = 1; i <= tenureMonths; i++) {
            BigDecimal interestComponent = outstanding.multiply(BigDecimal.valueOf(monthlyRate))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalComponent;
            BigDecimal emiThisInstallment;
            if (i == tenureMonths) {
                // Last installment pays off exactly what's left, absorbing
                // any rounding drift accumulated over the schedule.
                principalComponent = outstanding;
                emiThisInstallment = principalComponent.add(interestComponent);
            } else {
                principalComponent = emi.subtract(interestComponent);
                emiThisInstallment = emi;
            }
            outstanding = outstanding.subtract(principalComponent);
            schedule.add(new InstallmentBreakdown(i, emiThisInstallment, principalComponent, interestComponent));
        }
        return schedule;
    }

    public record InstallmentBreakdown(int installmentNumber, BigDecimal emiAmount,
                                        BigDecimal principalComponent, BigDecimal interestComponent) {
    }
}
