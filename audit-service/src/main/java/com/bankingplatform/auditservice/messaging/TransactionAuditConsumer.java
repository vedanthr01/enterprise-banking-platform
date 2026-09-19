package com.bankingplatform.auditservice.messaging;

import com.bankingplatform.auditservice.entity.AuditDomain;
import com.bankingplatform.auditservice.entity.AuditSeverity;
import com.bankingplatform.auditservice.service.AuditEventRecorder;
import com.bankingplatform.common.constants.KafkaTopics;
import com.bankingplatform.common.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Unlike Notification Service's consumers, nothing here is ever skipped:
 * Audit Service exists to record every event on every topic, so each switch
 * below is exhaustive with no default/"skip" branch. That also means the
 * compiler enforces completeness going forward — if TransactionEventType
 * ever grows a new constant, this class simply won't compile until it's
 * been given a severity and description, instead of silently falling
 * through unaudited.
 */
@Component
@RequiredArgsConstructor
public class TransactionAuditConsumer {

    private final AuditEventRecorder recorder;

    @KafkaListener(topics = KafkaTopics.TRANSACTION_EVENTS, groupId = KafkaTopics.TRANSACTION_EVENTS_GROUP_AUDIT,
            containerFactory = "kafkaListenerContainerFactory")
    public void onTransactionEvent(TransactionEvent event) {
        AuditSeverity severity = switch (event.getEventType()) {
            case TRANSFER_INITIATED, TRANSFER_COMPLETED -> AuditSeverity.INFO;
            case TRANSFER_FAILED, TRANSFER_COMPENSATED -> AuditSeverity.WARNING;
        };
        String description = switch (event.getEventType()) {
            case TRANSFER_INITIATED -> "Transfer " + event.getTransactionId() + " initiated: "
                    + event.getFromAccountId() + " -> " + event.getToAccountId() + " for " + event.getAmount();
            case TRANSFER_COMPLETED -> "Transfer " + event.getTransactionId() + " completed successfully";
            case TRANSFER_FAILED -> "Transfer " + event.getTransactionId() + " failed: " + event.getReason();
            case TRANSFER_COMPENSATED -> "Transfer " + event.getTransactionId()
                    + " compensated (earlier debit reversed) after a downstream failure: " + event.getReason();
        };
        recorder.record(event, AuditDomain.TRANSACTION, event.getEventType().name(), event.getTransactionId(),
                event.getInitiatedByUserId(), severity, description);
    }
}
