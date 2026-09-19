package com.bankingplatform.transactionservice.outbox;

import com.bankingplatform.common.event.TransactionEvent;
import com.bankingplatform.transactionservice.entity.OutboxEvent;
import com.bankingplatform.transactionservice.entity.OutboxStatus;
import com.bankingplatform.transactionservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The other half of the outbox pattern: polls PENDING rows and actually
 * sends them to Kafka, outside the original DB transaction. A row that
 * fails to publish just stays PENDING and is retried on the next tick —
 * at-least-once delivery, which is why every consumer of these events
 * (Notification, Fraud, Audit) is written to be idempotent on eventId /
 * natural keys rather than assuming exactly-once.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        if (pending.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pending) {
            try {
                TransactionEvent payload = objectMapper.readValue(event.getPayload(), TransactionEvent.class);
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), payload).get(5, TimeUnit.SECONDS);

                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
            } catch (Exception ex) {
                event.setAttempts(event.getAttempts() + 1);
                log.error("Failed to publish outbox event {} (attempt {}): {}",
                        event.getId(), event.getAttempts(), ex.getMessage());
                // left PENDING — picked up again on the next poll
            }
            outboxEventRepository.save(event);
        }
    }
}
