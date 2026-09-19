package com.bankingplatform.notificationservice.service;

import com.bankingplatform.notificationservice.dto.NotificationLogResponse;
import com.bankingplatform.notificationservice.entity.NotificationLog;
import com.bankingplatform.notificationservice.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationQueryServiceImpl implements NotificationQueryService {

    private final NotificationLogRepository notificationLogRepository;

    @Override
    public List<NotificationLogResponse> getForUser(String userId) {
        return notificationLogRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private NotificationLogResponse toResponse(NotificationLog n) {
        return NotificationLogResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .subject(n.getSubject())
                .status(n.getStatus())
                .errorMessage(n.getErrorMessage())
                .createdAt(n.getCreatedAt())
                .sentAt(n.getSentAt())
                .build();
    }
}
