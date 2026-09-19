package com.bankingplatform.accountservice.event;

import com.bankingplatform.common.constants.KafkaTopics;
import com.bankingplatform.common.event.AccountEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountEventListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountChanged(AccountChangedApplicationEvent event) {
        AccountEvent payload = AccountEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .accountId(event.getAccountId())
                .ownerUserId(event.getOwnerUserId())
                .eventType(event.getEventType())
                .amount(event.getAmount())
                .resultingBalance(event.getResultingBalance())
                .referenceId(event.getReferenceId())
                .occurredAt(Instant.now())
                .build();

        kafkaTemplate.send(KafkaTopics.ACCOUNT_EVENTS, event.getAccountId(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish AccountEvent for account {}: {}",
                                event.getAccountId(), ex.getMessage(), ex);
                    }
                });
    }
}
