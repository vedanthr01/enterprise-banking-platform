package com.bankingplatform.notificationservice.service;

import com.bankingplatform.notificationservice.dto.NotificationLogResponse;

import java.util.List;

public interface NotificationQueryService {
    List<NotificationLogResponse> getForUser(String userId);
}
