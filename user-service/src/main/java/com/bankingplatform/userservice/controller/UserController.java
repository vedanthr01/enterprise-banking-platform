package com.bankingplatform.userservice.controller;

import com.bankingplatform.userservice.dto.KycStatusUpdateRequest;
import com.bankingplatform.userservice.dto.UpdateProfileRequest;
import com.bankingplatform.userservice.dto.UserProfileResponse;
import com.bankingplatform.userservice.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile and KYC status")
public class UserController {

    private final UserProfileService userProfileService;

    @GetMapping("/{userId}")
    @PreAuthorize("#userId == authentication.name or hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get a user's profile")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable String userId) {
        return ResponseEntity.ok(userProfileService.getProfile(userId));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("#userId == authentication.name or hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update a user's profile")
    public ResponseEntity<UserProfileResponse> updateProfile(@PathVariable String userId,
                                                               @Valid @RequestBody UpdateProfileRequest request,
                                                               @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userProfileService.updateProfile(userId, request, jwt.getSubject()));
    }

    @GetMapping("/{userId}/kyc-status")
    @PreAuthorize("#userId == authentication.name or hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get KYC status - also called by Account Service before allowing account creation")
    public ResponseEntity<UserProfileResponse> getKycStatus(@PathVariable String userId) {
        return ResponseEntity.ok(userProfileService.getProfile(userId));
    }

    @PatchMapping("/{userId}/kyc-status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Admin-only: approve or reject a user's KYC")
    public ResponseEntity<UserProfileResponse> updateKycStatus(@PathVariable String userId,
                                                                 @Valid @RequestBody KycStatusUpdateRequest request,
                                                                 @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userProfileService.updateKycStatus(userId, request.getStatus(), jwt.getSubject()));
    }
}
