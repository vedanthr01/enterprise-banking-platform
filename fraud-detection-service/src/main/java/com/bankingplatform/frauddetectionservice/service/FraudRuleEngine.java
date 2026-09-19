package com.bankingplatform.frauddetectionservice.service;

import com.bankingplatform.common.event.FraudRuleType;
import com.bankingplatform.common.event.TransactionEvent;
import com.bankingplatform.frauddetectionservice.config.FraudRuleProperties;
import com.bankingplatform.frauddetectionservice.entity.TransferHistoryEntry;
import com.bankingplatform.frauddetectionservice.repository.TransferHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Three static, hand-written rules — deliberately not a trained model, for
 * the same reason LoanApprovalEngine is rule-based rather than ML-based:
 * every decision this engine makes has to be explainable in plain language
 * to a fraud analyst, an auditor, or the customer who got flagged ("why was
 * I flagged?" has to have an answer more specific than "the model said
 * so"). GET /fraud/rules/config exposes the exact thresholds in effect, so
 * the rules are inspectable, not just explainable in principle.
 *
 * Each rule can fire independently, so evaluate() returns a list, not a
 * single decision — a transfer can be both unusually large AND to a new
 * beneficiary at the same time, and both are independently worth surfacing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudRuleEngine {

    private final TransferHistoryRepository historyRepository;
    private final FraudRuleProperties properties;

    public record RuleFinding(FraudRuleType ruleType, int riskScore, String description) {}

    @Transactional
    public List<RuleFinding> evaluate(TransactionEvent event) {
        if (historyRepository.existsBySourceEventId(event.getEventId())) {
            log.debug("Transaction {} (event {}) already screened for fraud — skipping duplicate (at-least-once replay)",
                    event.getTransactionId(), event.getEventId());
            return List.of();
        }

        List<RuleFinding> findings = new ArrayList<>();

        // Must run *before* this transfer is recorded below — it asks
        // "has this pair transacted before *this* transfer," not counting
        // the one currently being screened.
        boolean isNewBeneficiary = !historyRepository.existsByFromAccountIdAndToAccountId(
                event.getFromAccountId(), event.getToAccountId());
        if (isNewBeneficiary) {
            findings.add(new RuleFinding(FraudRuleType.NEW_BENEFICIARY, 40,
                    "First recorded transfer from account " + event.getFromAccountId() + " to account "
                            + event.getToAccountId()));
        }

        if (event.getAmount().compareTo(properties.getLargeAmountThreshold()) > 0) {
            double overRatio = event.getAmount().doubleValue() / properties.getLargeAmountThreshold().doubleValue() - 1.0;
            int riskScore = (int) Math.min(100, 70 + overRatio * 30);
            findings.add(new RuleFinding(FraudRuleType.LARGE_AMOUNT, riskScore,
                    "Transfer amount " + event.getAmount() + " exceeds the large-amount threshold of "
                            + properties.getLargeAmountThreshold()));
        }

        recordHistory(event);

        // Counted *after* recording, so this transfer counts toward its own
        // velocity total — the Nth transfer in the window is the one that
        // trips the threshold, not the (N-1)th.
        Instant windowStart = event.getOccurredAt().minus(Duration.ofMinutes(properties.getVelocityWindowMinutes()));
        long recentCount = historyRepository.countByFromAccountIdAndOccurredAtAfter(event.getFromAccountId(), windowStart);
        if (recentCount > properties.getVelocityMaxTransfers()) {
            int riskScore = (int) Math.min(100, 60 + (recentCount - properties.getVelocityMaxTransfers()) * 5);
            findings.add(new RuleFinding(FraudRuleType.VELOCITY_CHECK, riskScore,
                    recentCount + " transfers from account " + event.getFromAccountId() + " within the last "
                            + properties.getVelocityWindowMinutes() + " minutes (threshold "
                            + properties.getVelocityMaxTransfers() + ")"));
        }

        return findings;
    }

    private void recordHistory(TransactionEvent event) {
        historyRepository.save(TransferHistoryEntry.builder()
                .id(UUID.randomUUID().toString())
                .sourceEventId(event.getEventId())
                .transactionId(event.getTransactionId())
                .fromAccountId(event.getFromAccountId())
                .toAccountId(event.getToAccountId())
                .amount(event.getAmount())
                .occurredAt(event.getOccurredAt())
                .build());
    }
}
