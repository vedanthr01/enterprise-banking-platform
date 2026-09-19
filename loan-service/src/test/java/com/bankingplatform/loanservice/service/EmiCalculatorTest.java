package com.bankingplatform.loanservice.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmiCalculatorTest {

    private final EmiCalculator emiCalculator = new EmiCalculator();

    @Test
    void calculateEmi_matchesKnownAmortizationResult() {
        // Textbook reducing-balance example: 100,000 principal, 12% annual,
        // 12 months -> EMI = 8,884.88 (a widely-cited reference value for
        // this exact input, useful as a fixed regression point).
        BigDecimal emi = emiCalculator.calculateEmi(new BigDecimal("100000"), new BigDecimal("12.00"), 12);
        assertThat(emi).isEqualByComparingTo("8884.88");
    }

    @Test
    void calculateEmi_isPlainDivision_whenRateIsZero() {
        BigDecimal emi = emiCalculator.calculateEmi(new BigDecimal("12000"), BigDecimal.ZERO, 12);
        assertThat(emi).isEqualByComparingTo("1000.00");
    }

    @Test
    void generateSchedule_sumsExactlyToPrincipal_withLastInstallmentAbsorbingRoundingDrift() {
        BigDecimal principal = new BigDecimal("100000");
        List<EmiCalculator.InstallmentBreakdown> schedule = emiCalculator.generateSchedule(
                principal, new BigDecimal("12.00"), 12);

        assertThat(schedule).hasSize(12);

        BigDecimal totalPrincipalPaid = schedule.stream()
                .map(EmiCalculator.InstallmentBreakdown::principalComponent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalPrincipalPaid).isEqualByComparingTo(principal);

        for (int i = 0; i < schedule.size(); i++) {
            assertThat(schedule.get(i).installmentNumber()).isEqualTo(i + 1);
        }
    }

    @Test
    void generateSchedule_interestComponentDecreasesOverTime_asPrincipalIsPaidDown() {
        List<EmiCalculator.InstallmentBreakdown> schedule = emiCalculator.generateSchedule(
                new BigDecimal("100000"), new BigDecimal("12.00"), 12);

        BigDecimal firstInterest = schedule.get(0).interestComponent();
        BigDecimal lastInterest = schedule.get(schedule.size() - 1).interestComponent();
        assertThat(firstInterest).isGreaterThan(lastInterest);
    }
}
