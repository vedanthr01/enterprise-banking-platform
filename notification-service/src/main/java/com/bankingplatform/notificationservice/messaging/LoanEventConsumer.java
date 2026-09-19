package com.bankingplatform.notificationservice.messaging;

import com.bankingplatform.common.constants.KafkaTopics;
import com.bankingplatform.common.event.LoanEvent;
import com.bankingplatform.notificationservice.entity.NotificationType;
import com.bankingplatform.notificationservice.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * LOAN_APPLICATION_APPROVED and EMI_PAYMENT_SUCCEEDED are deliberately not
 * surfaced here: APPROVED is a transient pre-disbursement state (the
 * customer cares about the terminal DISBURSED/REJECTED outcome, not this
 * intermediate step), and a successful EMI debit already generates a
 * BALANCE_DEBITED account-event — the same "don't double-notify for one
 * real-world event" rule AccountEventConsumer and TransactionEventConsumer
 * follow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoanEventConsumer {

    private final NotificationDispatcher dispatcher;

    @KafkaListener(topics = KafkaTopics.LOAN_EVENTS, groupId = KafkaTopics.LOAN_EVENTS_GROUP_NOTIFICATION,
            containerFactory = "kafkaListenerContainerFactory")
    public void onLoanEvent(LoanEvent event) {
        switch (event.getEventType()) {
            case LOAN_DISBURSED -> dispatcher.dispatchByUserId(event.getEventId(), event.getBorrowerUserId(),
                    NotificationType.LOAN_DISBURSED, "Your loan has been disbursed",
                    "Congratulations — your loan of " + event.getAmount() + " has been approved and disbursed "
                            + "to your account. Check your loan schedule for the EMI due dates.");

            case LOAN_APPLICATION_REJECTED -> dispatcher.dispatchByUserId(event.getEventId(), event.getBorrowerUserId(),
                    NotificationType.LOAN_APPLICATION_REJECTED, "Your loan application was not approved",
                    "Your loan application could not be approved. Reason: " + event.getReason());

            case EMI_PAYMENT_FAILED -> dispatcher.dispatchByUserId(event.getEventId(), event.getBorrowerUserId(),
                    NotificationType.EMI_PAYMENT_FAILED, "Your EMI payment failed",
                    "We were unable to collect your EMI of " + event.getAmount() + ". Reason: " + event.getReason()
                            + ". Please ensure sufficient balance — we'll retry automatically.");

            case LOAN_CLOSED -> dispatcher.dispatchByUserId(event.getEventId(), event.getBorrowerUserId(),
                    NotificationType.LOAN_CLOSED, "Your loan is fully repaid",
                    "Congratulations — you've made your final EMI payment and your loan is now fully closed.");

            case LOAN_APPLICATION_APPROVED, EMI_PAYMENT_SUCCEEDED -> log.debug(
                    "Skipping notification for {} on loan/application {} — not independently customer-facing",
                    event.getEventType(), event.getLoanId() != null ? event.getLoanId() : event.getApplicationId());

            default -> log.debug("No notification mapping for loan event type {}", event.getEventType());
        }
    }
}
