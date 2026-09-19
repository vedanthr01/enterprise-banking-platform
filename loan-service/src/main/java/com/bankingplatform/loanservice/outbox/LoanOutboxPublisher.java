package com.bankingplatform.loanservice.outbox;

import com.bankingplatform.common.event.LoanEvent;
import com.bankingplatform.loanservice.entity.OutboxEvent;
import com.bankingplatform.loanservice.entity.OutboxStatus;
import com.bankingplatform.loanservice.repository.OutboxEventRepository;
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
 * The other half of the outbox pattern — same shape as Transaction
 * Service's OutboxPublisher: polls PENDING rows and sends them to Kafka
 * outside the original DB transaction. A row that fails to publish just
 * stays PENDING and is retried next tick — at-least-once delivery, which is
 * why loan-events consumers (Notification, Audit) are idempotent on
 * eventId rather than assuming exactly-once.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoanOutboxPublisher {

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
                LoanEvent payload = objectMapper.readValue(event.getPayload(), LoanEvent.class);
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
