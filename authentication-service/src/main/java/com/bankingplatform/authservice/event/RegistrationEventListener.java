package com.bankingplatform.authservice.event;

import com.bankingplatform.common.constants.KafkaTopics;
import com.bankingplatform.common.event.UserEventType;
import com.bankingplatform.common.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Publishes to Kafka only after the registration transaction has actually
 * committed — avoids emitting an event for a database write that later
 * rolled back. This is a lighter-weight alternative to the full
 * transactional outbox used in Transaction Service (where money is on the
 * line); here, at-least-once delivery with brief eventual consistency is an
 * acceptable trade-off since a slightly-delayed profile row is harmless.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationEventListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredApplicationEvent event) {
        UserRegisteredEvent payload = UserRegisteredEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(UserEventType.USER_REGISTERED)
                .userId(event.getUserId())
                .username(event.getUsername())
                .email(event.getEmail())
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .phone(event.getPhone())
                .performedByUserId(event.getUserId())
                .occurredAt(Instant.now())
                .build();

        kafkaTemplate.send(KafkaTopics.USER_EVENTS, event.getUserId(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish UserRegisteredEvent for user {}: {}",
                                event.getUserId(), ex.getMessage(), ex);
                    } else {
                        log.info("Published UserRegisteredEvent for user {} to partition {}",
                                event.getUserId(), result.getRecordMetadata().partition());
                    }
                });
    }
}
