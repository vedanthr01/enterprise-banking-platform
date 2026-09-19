package com.bankingplatform.notificationservice.repository;

import com.bankingplatform.notificationservice.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, String> {
    boolean existsBySourceEventId(String sourceEventId);
    List<NotificationLog> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId);
}
