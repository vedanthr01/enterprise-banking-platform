package com.bankingplatform.userservice.controller;

import com.bankingplatform.userservice.dto.UserContactResponse;
import com.bankingplatform.userservice.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Service-to-service only — see InternalApiKeyFilter. Called by
 *  Notification Service to resolve who to email for an account/transaction
 *  event that only carries a userId. */
@RestController
@RequestMapping("/users/internal")
@RequiredArgsConstructor
@Tag(name = "Users (internal)", description = "Requires X-Internal-Api-Key — not reachable with a user JWT")
public class InternalUserController {

    private final UserProfileService userProfileService;

    @GetMapping("/{userId}/contact")
    @Operation(summary = "Resolve a user's contact details for notification dispatch")
    public ResponseEntity<UserContactResponse> getContact(@PathVariable String userId) {
        return ResponseEntity.ok(userProfileService.getContact(userId));
    }
}
