package com.bankingplatform.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Account Service's own minimal view of a user's KYC state — deliberately
 * NOT a shared class with User Service's UserProfileResponse. Each service
 * owns its own client-side contract for what it consumes from another
 * service, so the two can evolve independently.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserKycResponse {
    private String userId;
    private String kycStatus;
}
