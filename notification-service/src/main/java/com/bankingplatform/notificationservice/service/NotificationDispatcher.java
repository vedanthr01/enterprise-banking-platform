package com.bankingplatform.notificationservice.service;

import com.bankingplatform.notificationservice.client.UserContactGateway;
import com.bankingplatform.notificationservice.dto.UserContactResponse;
import com.bankingplatform.notificationservice.entity.NotificationLog;
import com.bankingplatform.notificationservice.entity.NotificationStatus;
import com.bankingplatform.notificationservice.entity.NotificationType;
import com.bankingplatform.notificationservice.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

/**
 * Single funnel every consumer routes through: idempotency guard, contact
 * resolution (when the caller doesn't already have an email), sending, and
 * delivery logging all happen in exactly one place so each of those
 * concerns is implemented — and tested — once.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationLogRepository notificationLogRepository;
    private final UserContactGateway userContactGateway;
    private final EmailService emailService;

    /**
     * For events that only carry a userId (account/transaction/fraud
     * events) — resolves the recipient's email via User Service first.
     */
    public void dispatchByUserId(String sourceEventId, String userId, NotificationType type,
                                  String subject, String body) {
        // Cheap early check to skip a wasted Feign call on Kafka replay;
        // dispatch() repeats the check right before the write, which is the
        // one that actually guards against a duplicate send.
        if (notificationLogRepository.existsBySourceEventId(sourceEventId)) {
            log.info("Notification for event {} already processed — skipping duplicate delivery", sourceEventId);
            return;
        }
        UserContactResponse contact = userContactGateway.getContact(userId);
        dispatch(sourceEventId, userId, contact != null ? contact.getEmail() : null, type, subject, body);
    }

    /**
     * For events that already carry the recipient's email inline (e.g.
     * UserRegisteredEvent) — no cross-service lookup, which also sidesteps
     * a real race: User Service's own consumer may not have finished
     * creating the profile from this very same event yet.
     */
    public void dispatch(String sourceEventId, String userId, String recipientEmail,
                          NotificationType type, String subject, String body) {
        if (notificationLogRepository.existsBySourceEventId(sourceEventId)) {
            log.info("Notification for event {} already processed — skipping duplicate delivery", sourceEventId);
            return;
        }

        if (!StringUtils.hasText(recipientEmail)) {
            log.warn("No email available for user {} (event {}) — logging as SKIPPED", userId, sourceEventId);
            saveLog(sourceEventId, userId, null, type, subject, body,
                    NotificationStatus.SKIPPED, "Recipient email unavailable");
            return;
        }

        boolean sent = emailService.send(recipientEmail, subject, body);
        saveLog(sourceEventId, userId, recipientEmail, type, subject, body,
                sent ? NotificationStatus.SENT : NotificationStatus.FAILED,
                sent ? null : "Email dispatch failed — see application logs for the underlying mail server error");
    }

    private void saveLog(String sourceEventId, String userId, String email, NotificationType type,
                          String subject, String body, NotificationStatus status, String errorMessage) {
        NotificationLog entry = NotificationLog.builder()
                .id(UUID.randomUUID().toString())
                .sourceEventId(sourceEventId)
                .recipientUserId(userId)
                .recipientEmail(email)
                .type(type)
                .subject(subject)
                .body(body)
                .status(status)
                .errorMessage(errorMessage)
                .sentAt(status == NotificationStatus.SENT ? Instant.now() : null)
                .build();
        notificationLogRepository.save(entry);
    }
}
