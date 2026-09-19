package com.bankingplatform.notificationservice.messaging;

import com.bankingplatform.common.constants.KafkaTopics;
import com.bankingplatform.common.event.AccountEvent;
import com.bankingplatform.notificationservice.entity.NotificationType;
import com.bankingplatform.notificationservice.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * account-events is treated as the authoritative stream for "your balance
 * changed" facts. DEBIT_FAILED / DEBIT_COMPENSATED are deliberately NOT
 * surfaced from here — they're legs of a transfer Saga failure/rollback,
 * which transaction-events already reports with the fuller "why" (see
 * TransactionEventConsumer). Emailing from both streams would mean two
 * emails for one real-world event. (When Loan Service lands, EMI-debit
 * failures will be surfaced via a dedicated loan-events consumer instead of
 * being folded into this DEBIT_FAILED case, for the same reason.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountEventConsumer {

    private final NotificationDispatcher dispatcher;

    @KafkaListener(topics = KafkaTopics.ACCOUNT_EVENTS, groupId = KafkaTopics.ACCOUNT_EVENTS_GROUP_NOTIFICATION,
            containerFactory = "kafkaListenerContainerFactory")
    public void onAccountEvent(AccountEvent event) {
        switch (event.getEventType()) {
            case ACCOUNT_CREATED -> dispatcher.dispatchByUserId(event.getEventId(), event.getOwnerUserId(),
                    NotificationType.ACCOUNT_CREATED, "Your new account is ready",
                    "A new account (" + event.getAccountId() + ") has been opened on your profile.");

            case BALANCE_DEBITED -> dispatcher.dispatchByUserId(event.getEventId(), event.getOwnerUserId(),
                    NotificationType.ACCOUNT_DEBITED, "Account debited",
                    "Your account " + event.getAccountId() + " was debited " + event.getAmount()
                            + ". New balance: " + event.getResultingBalance() + ".");

            case BALANCE_CREDITED -> dispatcher.dispatchByUserId(event.getEventId(), event.getOwnerUserId(),
                    NotificationType.ACCOUNT_CREDITED, "Account credited",
                    "Your account " + event.getAccountId() + " was credited " + event.getAmount()
                            + ". New balance: " + event.getResultingBalance() + ".");

            case DEBIT_FAILED, DEBIT_COMPENSATED -> log.debug(
                    "Skipping notification for {} on account {} — the transaction-events stream reports this with more context",
                    event.getEventType(), event.getAccountId());

            default -> log.debug("No notification mapping for account event type {}", event.getEventType());
        }
    }
}
