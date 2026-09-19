package com.bankingplatform.auditservice.messaging;

import com.bankingplatform.auditservice.entity.AuditDomain;
import com.bankingplatform.auditservice.entity.AuditSeverity;
import com.bankingplatform.auditservice.service.AuditEventRecorder;
import com.bankingplatform.common.constants.KafkaTopics;
import com.bankingplatform.common.event.AccountEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountAuditConsumer {

    private final AuditEventRecorder recorder;

    @KafkaListener(topics = KafkaTopics.ACCOUNT_EVENTS, groupId = KafkaTopics.ACCOUNT_EVENTS_GROUP_AUDIT,
            containerFactory = "kafkaListenerContainerFactory")
    public void onAccountEvent(AccountEvent event) {
        AuditSeverity severity = switch (event.getEventType()) {
            case ACCOUNT_CREATED, BALANCE_DEBITED, BALANCE_CREDITED, DEBIT_COMPENSATED -> AuditSeverity.INFO;
            case DEBIT_FAILED -> AuditSeverity.WARNING;
        };
        String description = switch (event.getEventType()) {
            case ACCOUNT_CREATED -> "Account " + event.getAccountId() + " opened";
            case BALANCE_DEBITED -> "Account " + event.getAccountId() + " debited " + event.getAmount()
                    + "; resulting balance " + event.getResultingBalance();
            case BALANCE_CREDITED -> "Account " + event.getAccountId() + " credited " + event.getAmount()
                    + "; resulting balance " + event.getResultingBalance();
            case DEBIT_FAILED -> "Debit of " + event.getAmount() + " on account " + event.getAccountId() + " failed";
            case DEBIT_COMPENSATED -> "Earlier debit of " + event.getAmount() + " on account " + event.getAccountId()
                    + " was reversed (Saga compensation)";
        };
        recorder.record(event, AuditDomain.ACCOUNT, event.getEventType().name(), event.getAccountId(),
                event.getOwnerUserId(), severity, description);
    }
}
