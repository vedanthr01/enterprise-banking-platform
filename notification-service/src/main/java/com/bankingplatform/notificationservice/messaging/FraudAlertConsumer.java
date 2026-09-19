package com.bankingplatform.notificationservice.messaging;

import com.bankingplatform.common.constants.KafkaTopics;
import com.bankingplatform.common.event.FraudAlertEvent;
import com.bankingplatform.notificationservice.entity.NotificationType;
import com.bankingplatform.notificationservice.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FraudAlertConsumer {

    private final NotificationDispatcher dispatcher;

    @KafkaListener(topics = KafkaTopics.FRAUD_ALERTS, groupId = KafkaTopics.FRAUD_ALERTS_GROUP_NOTIFICATION,
            containerFactory = "kafkaListenerContainerFactory")
    public void onFraudAlert(FraudAlertEvent event) {
        String subject = "Security alert: unusual activity on your account";
        String body = "We flagged a transaction on your account for review.\n"
                + "Rule triggered: " + event.getRuleTriggered() + "\n"
                + "Risk score: " + event.getRiskScore() + "\n"
                + "Amount: " + event.getAmount() + "\n"
                + "Details: " + event.getDescription() + "\n\n"
                + "If this was you, no action is needed. If you don't recognize this activity, "
                + "please contact support immediately.";
        dispatcher.dispatchByUserId(event.getEventId(), event.getUserId(), NotificationType.FRAUD_ALERT, subject, body);
    }
}
