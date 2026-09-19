package com.bankingplatform.userservice.messaging;

import com.bankingplatform.common.constants.KafkaTopics;
import com.bankingplatform.common.event.UserEventType;
import com.bankingplatform.common.event.UserRegisteredEvent;
import com.bankingplatform.userservice.entity.KycStatus;
import com.bankingplatform.userservice.entity.UserProfile;
import com.bankingplatform.userservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Eventually-consistent profile creation: Authentication Service already
 * committed the credential and answered the client before this runs. Guards
 * against duplicate delivery (Kafka is at-least-once) with a plain
 * existence check rather than assuming exactly-once.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventConsumer {

    private final UserProfileRepository userProfileRepository;

    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = KafkaTopics.USER_EVENTS_GROUP_USER_SERVICE,
            containerFactory = "kafkaListenerContainerFactory")
    public void onUserRegistered(UserRegisteredEvent event) {
        if (event.getEventType() != UserEventType.USER_REGISTERED) {
            return;
        }
        if (userProfileRepository.existsById(event.getUserId())) {
            log.info("Profile for user {} already exists — skipping duplicate delivery", event.getUserId());
            return;
        }

        UserProfile profile = UserProfile.builder()
                .userId(event.getUserId())
                .username(event.getUsername())
                .email(event.getEmail())
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .phone(event.getPhone())
                .kycStatus(KycStatus.PENDING)
                .build();
        userProfileRepository.save(profile);
        log.info("Created profile for user {} (KYC pending)", event.getUserId());
    }
}
