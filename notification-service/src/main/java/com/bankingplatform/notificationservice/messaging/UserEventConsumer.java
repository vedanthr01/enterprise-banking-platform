package com.bankingplatform.notificationservice.messaging;

import com.bankingplatform.common.constants.KafkaTopics;
import com.bankingplatform.common.event.UserEventType;
import com.bankingplatform.common.event.UserRegisteredEvent;
import com.bankingplatform.notificationservice.entity.NotificationType;
import com.bankingplatform.notificationservice.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final NotificationDispatcher dispatcher;

    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = KafkaTopics.USER_EVENTS_GROUP_NOTIFICATION,
            containerFactory = "kafkaListenerContainerFactory")
    public void onUserEvent(UserRegisteredEvent event) {
        if (event.getEventType() != UserEventType.USER_REGISTERED) {
            return;
        }
        String subject = "Welcome to Enterprise Banking, " + event.getFirstName() + "!";
        String body = "Hi " + event.getFirstName() + ",\n\n"
                + "Your account has been created successfully (username: " + event.getUsername() + ").\n"
                + "Log in to complete your KYC verification and open your first account.\n\n"
                + "— Enterprise Distributed Banking Transaction Platform";
        // Email comes straight off the event — resolving it via User Service
        // instead would race that service's own consumer processing this
        // same event to create the profile row in the first place.
        dispatcher.dispatch(event.getEventId(), event.getUserId(), event.getEmail(),
                NotificationType.WELCOME, subject, body);
    }
}
