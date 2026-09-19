package com.bankingplatform.frauddetectionservice.service;

import com.bankingplatform.common.event.FraudRuleType;
import com.bankingplatform.common.event.TransactionEvent;
import com.bankingplatform.common.event.TransactionEventType;
import com.bankingplatform.frauddetectionservice.config.FraudRuleProperties;
import com.bankingplatform.frauddetectionservice.repository.TransferHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudRuleEngineTest {

    @Mock
    private TransferHistoryRepository historyRepository;

    private FraudRuleEngine engine;

    @BeforeEach
    void setUp() {
        // Real, default-valued properties object — no config server or yml
        // involved, exercising the engine against the exact defaults
        // FraudRuleProperties ships with (200000 / 10 min / 5 transfers).
        engine = new FraudRuleEngine(historyRepository, new FraudRuleProperties());
    }

    private TransactionEvent event(String eventId, BigDecimal amount) {
        return TransactionEvent.builder()
                .eventId(eventId)
                .transactionId("txn-" + eventId)
                .initiatedByUserId("user-1")
                .eventType(TransactionEventType.TRANSFER_COMPLETED)
                .fromAccountId("acc-from")
                .toAccountId("acc-to")
                .amount(amount)
                .occurredAt(Instant.parse("2026-08-03T10:00:00Z"))
                .build();
    }

    @Test
    void evaluate_flagsNothing_forAnOrdinaryTransferToAKnownBeneficiaryUnderVelocityLimits() {
        when(historyRepository.existsBySourceEventId(anyString())).thenReturn(false);
        when(historyRepository.existsByFromAccountIdAndToAccountId("acc-from", "acc-to")).thenReturn(true);
        when(historyRepository.countByFromAccountIdAndOccurredAtAfter(anyString(), any())).thenReturn(1L);

        List<FraudRuleEngine.RuleFinding> findings = engine.evaluate(event("evt-1", new BigDecimal("500")));

        assertThat(findings).isEmpty();
        // The transfer still gets recorded for future checks even though
        // nothing fired this time.
        verify(historyRepository).save(any());
    }

    @Test
    void evaluate_flagsNewBeneficiary_whenThisFromToPairHasNeverTransactedBefore() {
        when(historyRepository.existsBySourceEventId(anyString())).thenReturn(false);
        when(historyRepository.existsByFromAccountIdAndToAccountId("acc-from", "acc-to")).thenReturn(false);
        when(historyRepository.countByFromAccountIdAndOccurredAtAfter(anyString(), any())).thenReturn(1L);

        List<FraudRuleEngine.RuleFinding> findings = engine.evaluate(event("evt-1", new BigDecimal("500")));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleType()).isEqualTo(FraudRuleType.NEW_BENEFICIARY);
        assertThat(findings.get(0).riskScore()).isEqualTo(40);
    }

    @Test
    void evaluate_flagsLargeAmount_whenAboveTheConfiguredThreshold() {
        when(historyRepository.existsBySourceEventId(anyString())).thenReturn(false);
        when(historyRepository.existsByFromAccountIdAndToAccountId("acc-from", "acc-to")).thenReturn(true);
        when(historyRepository.countByFromAccountIdAndOccurredAtAfter(anyString(), any())).thenReturn(1L);

        // Double the default 200000 threshold.
        List<FraudRuleEngine.RuleFinding> findings = engine.evaluate(event("evt-1", new BigDecimal("400000")));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleType()).isEqualTo(FraudRuleType.LARGE_AMOUNT);
        // 100% over threshold -> base 70 + 1.0*30 = 100 (capped).
        assertThat(findings.get(0).riskScore()).isEqualTo(100);
    }

    @Test
    void evaluate_doesNotFlagLargeAmount_whenExactlyAtThreshold() {
        when(historyRepository.existsBySourceEventId(anyString())).thenReturn(false);
        when(historyRepository.existsByFromAccountIdAndToAccountId("acc-from", "acc-to")).thenReturn(true);
        when(historyRepository.countByFromAccountIdAndOccurredAtAfter(anyString(), any())).thenReturn(1L);

        List<FraudRuleEngine.RuleFinding> findings = engine.evaluate(event("evt-1", new BigDecimal("200000")));

        assertThat(findings).isEmpty();
    }

    @Test
    void evaluate_flagsVelocityCheck_whenMoreThanThresholdTransfersInTheWindow() {
        when(historyRepository.existsBySourceEventId(anyString())).thenReturn(false);
        when(historyRepository.existsByFromAccountIdAndToAccountId("acc-from", "acc-to")).thenReturn(true);
        // Default max is 5 — 6 recorded (including the one just inserted) trips it.
        when(historyRepository.countByFromAccountIdAndOccurredAtAfter(anyString(), any())).thenReturn(6L);

        List<FraudRuleEngine.RuleFinding> findings = engine.evaluate(event("evt-1", new BigDecimal("500")));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).ruleType()).isEqualTo(FraudRuleType.VELOCITY_CHECK);
        assertThat(findings.get(0).riskScore()).isEqualTo(65); // 60 + (6-5)*5
    }

    @Test
    void evaluate_flagsMultipleRulesIndependently_whenSeveralConditionsAreMetAtOnce() {
        when(historyRepository.existsBySourceEventId(anyString())).thenReturn(false);
        when(historyRepository.existsByFromAccountIdAndToAccountId("acc-from", "acc-to")).thenReturn(false);
        when(historyRepository.countByFromAccountIdAndOccurredAtAfter(anyString(), any())).thenReturn(7L);

        List<FraudRuleEngine.RuleFinding> findings = engine.evaluate(event("evt-1", new BigDecimal("500000")));

        assertThat(findings).extracting(FraudRuleEngine.RuleFinding::ruleType)
                .containsExactlyInAnyOrder(FraudRuleType.NEW_BENEFICIARY, FraudRuleType.LARGE_AMOUNT, FraudRuleType.VELOCITY_CHECK);
    }

    @Test
    void evaluate_returnsEmptyList_andNeverPersists_whenEventAlreadyScreened() {
        when(historyRepository.existsBySourceEventId("evt-1")).thenReturn(true);

        List<FraudRuleEngine.RuleFinding> findings = engine.evaluate(event("evt-1", new BigDecimal("999999")));

        assertThat(findings).isEmpty();
        verify(historyRepository, never()).save(any());
        // A replay must not re-derive findings from stale/partial mocking —
        // confirm the rule-lookup queries were never even reached.
        verify(historyRepository, never()).existsByFromAccountIdAndToAccountId(anyString(), anyString());
    }
}
