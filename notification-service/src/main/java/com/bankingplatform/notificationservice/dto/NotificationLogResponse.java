package com.bankingplatform.notificationservice.dto;

import com.bankingplatform.notificationservice.entity.NotificationStatus;
import com.bankingplatform.notificationservice.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLogResponse {
    private String id;
    private NotificationType type;
    private String subject;
    private NotificationStatus status;
    private String errorMessage;
    private Instant createdAt;
    private Instant sentAt;
}
