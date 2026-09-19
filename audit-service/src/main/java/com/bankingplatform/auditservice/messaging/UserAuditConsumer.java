package com.bankingplatform.auditservice.messaging;

import com.bankingplatform.auditservice.entity.AuditDomain;
import com.bankingplatform.auditservice.entity.AuditSeverity;
import com.bankingplatform.auditservice.service.AuditEventRecorder;
import com.bankingplatform.common.constants.KafkaTopics;
import com.bankingplatform.common.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Despite the class name (kept as-is — see its Javadoc), UserRegisteredEvent
 * is the generic envelope for every "user-events" type, so this one
 * consumer covers USER_REGISTERED, KYC_VERIFIED, and PROFILE_UPDATED alike.
 * actorUserId is performedByUserId rather than the profile owner's userId
 * whenever they differ — e.g. an admin verifying someone else's KYC —
 * which is exactly the distinction an audit trail exists to preserve.
 */
@Component
@RequiredArgsConstructor
public class UserAuditConsumer {

    private final AuditEventRecorder recorder;

    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = KafkaTopics.USER_EVENTS_GROUP_AUDIT,
            containerFactory = "kafkaListenerContainerFactory")
    public void onUserEvent(UserRegisteredEvent event) {
        String description = switch (event.getEventType()) {
            case USER_REGISTERED -> "User " + event.getUsername() + " (" + event.getUserId() + ") registered";
            case KYC_VERIFIED -> "KYC verified for user " + event.getUserId();
            case PROFILE_UPDATED -> "Profile updated for user " + event.getUserId();
        };
        String actorUserId = event.getPerformedByUserId() != null ? event.getPerformedByUserId() : event.getUserId();
        recorder.record(event, AuditDomain.USER, event.getEventType().name(), event.getUserId(),
                actorUserId, AuditSeverity.INFO, description);
    }
}
