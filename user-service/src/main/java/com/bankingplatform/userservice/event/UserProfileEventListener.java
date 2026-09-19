package com.bankingplatform.userservice.event;

import com.bankingplatform.common.constants.KafkaTopics;
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
 * Publishes to Kafka only after the KYC/profile-update transaction has
 * actually committed — mirrors Authentication Service's
 * RegistrationEventListener exactly, for the same reason: never emit an
 * event for a database write that later rolled back.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileEventListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProfileChanged(UserProfileChangedApplicationEvent event) {
        UserRegisteredEvent payload = UserRegisteredEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(event.getEventType())
                .userId(event.getUserId())
                .username(event.getUsername())
                .email(event.getEmail())
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .phone(event.getPhone())
                .performedByUserId(event.getPerformedByUserId())
                .occurredAt(Instant.now())
                .build();

        kafkaTemplate.send(KafkaTopics.USER_EVENTS, event.getUserId(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} for user {}: {}",
                                event.getEventType(), event.getUserId(), ex.getMessage(), ex);
                    } else {
                        log.info("Published {} for user {} to partition {}",
                                event.getEventType(), event.getUserId(), result.getRecordMetadata().partition());
                    }
                });
    }
}
