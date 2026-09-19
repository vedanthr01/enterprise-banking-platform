package com.bankingplatform.notificationservice.controller;

import com.bankingplatform.notificationservice.dto.NotificationLogResponse;
import com.bankingplatform.notificationservice.service.NotificationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification delivery log")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    @GetMapping("/my")
    @Operation(summary = "List notifications sent to the caller")
    public ResponseEntity<List<NotificationLogResponse>> myNotifications(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(notificationQueryService.getForUser(jwt.getSubject()));
    }

    @GetMapping("/admin/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Admin-only: list notifications sent to any user")
    public ResponseEntity<List<NotificationLogResponse>> forUser(@PathVariable String userId) {
        return ResponseEntity.ok(notificationQueryService.getForUser(userId));
    }
}
