package com.bankingplatform.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One row per notification attempt, keyed for idempotency by the source
 * Kafka event's eventId (source_event_id) rather than by our own id — that
 * is what lets the at-least-once consumers safely check "have I already
 * handled this event?" before sending anything.
 */
@Entity
@Table(name = "notification_logs", indexes = {
        @Index(name = "idx_notification_source_event", columnList = "source_event_id", unique = true),
        @Index(name = "idx_notification_recipient", columnList = "recipient_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "source_event_id", nullable = false, length = 64)
    private String sourceEventId;

    @Column(name = "recipient_user_id", nullable = false, length = 36)
    private String recipientUserId;

    @Column(name = "recipient_email", length = 128)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private NotificationType type;

    @Column(nullable = false, length = 160)
    private String subject;

    @Lob
    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationStatus status;

    @Column(name = "error_message", length = 255)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
