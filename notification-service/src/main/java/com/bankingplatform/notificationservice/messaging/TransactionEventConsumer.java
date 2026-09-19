package com.bankingplatform.notificationservice.messaging;

import com.bankingplatform.common.constants.KafkaTopics;
import com.bankingplatform.common.event.TransactionEvent;
import com.bankingplatform.notificationservice.entity.NotificationType;
import com.bankingplatform.notificationservice.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Only the two terminal states with NO equivalent on account-events are
 * handled here: a FAILED transfer never touched a balance at all (no
 * account-event exists for it), and a COMPENSATED transfer needs the "why"
 * that a bare BALANCE_CREDITED can't express. A COMPLETED transfer is
 * already fully covered by the debit + credit account-events the customer
 * receives — repeating it here would just be a duplicate email.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final NotificationDispatcher dispatcher;

    @KafkaListener(topics = KafkaTopics.TRANSACTION_EVENTS, groupId = KafkaTopics.TRANSACTION_EVENTS_GROUP_NOTIFICATION,
            containerFactory = "kafkaListenerContainerFactory")
    public void onTransactionEvent(TransactionEvent event) {
        switch (event.getEventType()) {
            case TRANSFER_FAILED -> dispatcher.dispatchByUserId(event.getEventId(), event.getInitiatedByUserId(),
                    NotificationType.TRANSFER_FAILED, "Your transfer did not go through",
                    "Your transfer of " + event.getAmount() + " " + event.getCurrency()
                            + " could not be completed. Reason: " + event.getReason());

            case TRANSFER_COMPENSATED -> dispatcher.dispatchByUserId(event.getEventId(), event.getInitiatedByUserId(),
                    NotificationType.TRANSFER_REVERSED, "Your transfer was reversed",
                    "Your transfer of " + event.getAmount() + " " + event.getCurrency()
                            + " could not be delivered and has been automatically reversed back into your account. "
                            + "Reason: " + event.getReason());

            case TRANSFER_INITIATED, TRANSFER_COMPLETED -> log.debug(
                    "Skipping notification for {} on transaction {} — already covered by the account-events balance-change stream",
                    event.getEventType(), event.getTransactionId());

            default -> log.debug("No notification mapping for transaction event type {}", event.getEventType());
        }
    }
}
