package com.bankingplatform.frauddetectionservice.messaging;

import com.bankingplatform.common.constants.KafkaTopics;
import com.bankingplatform.common.event.FraudAlertEvent;
import com.bankingplatform.common.event.TransactionEvent;
import com.bankingplatform.common.event.TransactionEventType;
import com.bankingplatform.frauddetectionservice.service.FraudRuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A detective control, not a preventive one — worth being explicit about.
 * This consumer sees a transfer only after Transaction Service has already
 * committed it (via the outbox relay) and Kafka has delivered the event, so
 * it can never block the transfer it's screening; it can only flag it for
 * follow-up (customer notification via Notification Service, a permanent
 * audit trail entry via Audit Service, or a human fraud-ops review). A
 * synchronous, in-line check called from inside TransferServiceImpl before
 * the debit leg is the alternative — real-time blocking — architecture; not
 * used here on purpose, to keep the money-movement path free of a
 * dependency on this service's availability and latency. That trade-off
 * (asynchronous/eventually-consistent screening vs. synchronous/blocking
 * screening) is a deliberate, discussable choice, not an oversight.
 *
 * Screens only TRANSFER_COMPLETED — the other TransactionEventType values
 * either never moved money (INITIATED) or already didn't complete (FAILED,
 * COMPENSATED), so there is nothing for a fraud rule to evaluate. Extending
 * velocity-checking to include failed *attempts* (a classic account-takeover
 * signal — many rapid failures, not just successes) is a natural follow-up
 * and explicitly out of scope here.
 *
 * Publishes directly via KafkaTemplate rather than the transactional outbox
 * pattern Transaction/Loan Service use — deliberately: outbox exists to
 * solve the dual-write problem for operations where a DB write and an event
 * *must* be atomic, because losing one would mean losing or double-counting
 * money. No money moves here; a fraud alert that fails to publish is a
 * degraded outcome (backed by producer retries, and this same pattern would
 * likely resurface on the *next* transfer from the same account, or is
 * still visible via Audit Service once caught up), not a lost financial
 * fact, so outbox's added complexity isn't justified for this write.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionFraudConsumer {

    private final FraudRuleEngine fraudRuleEngine;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = KafkaTopics.TRANSACTION_EVENTS, groupId = KafkaTopics.TRANSACTION_EVENTS_GROUP_FRAUD,
            containerFactory = "kafkaListenerContainerFactory")
    public void onTransactionEvent(TransactionEvent event) {
        if (event.getEventType() != TransactionEventType.TRANSFER_COMPLETED) {
            return;
        }

        List<FraudRuleEngine.RuleFinding> findings = fraudRuleEngine.evaluate(event);
        findings.forEach(finding -> publishAlert(event, finding));
    }

    private void publishAlert(TransactionEvent event, FraudRuleEngine.RuleFinding finding) {
        FraudAlertEvent alert = FraudAlertEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .transactionId(event.getTransactionId())
                .accountId(event.getFromAccountId())
                .userId(event.getInitiatedByUserId())
                .ruleTriggered(finding.ruleType())
                .riskScore(finding.riskScore())
                .amount(event.getAmount())
                .description(finding.description())
                .occurredAt(Instant.now())
                .build();

        kafkaTemplate.send(KafkaTopics.FRAUD_ALERTS, event.getTransactionId(), alert)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish fraud alert for transaction {} (rule {}): {}",
                                event.getTransactionId(), finding.ruleType(), ex.getMessage(), ex);
                    } else {
                        log.warn("Fraud alert published: rule={} riskScore={} transaction={} fromAccount={}",
                                finding.ruleType(), finding.riskScore(), event.getTransactionId(), event.getFromAccountId());
                    }
                });
    }
}
