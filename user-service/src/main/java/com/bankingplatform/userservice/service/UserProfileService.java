package com.bankingplatform.userservice.service;

import com.bankingplatform.userservice.dto.UpdateProfileRequest;
import com.bankingplatform.userservice.dto.UserContactResponse;
import com.bankingplatform.userservice.dto.UserProfileResponse;
import com.bankingplatform.userservice.entity.KycStatus;

public interface UserProfileService {
    UserProfileResponse getProfile(String userId);
    UserProfileResponse updateProfile(String userId, UpdateProfileRequest request, String actorUserId);
    UserProfileResponse updateKycStatus(String userId, KycStatus status, String actorUserId);
    boolean isKycVerified(String userId);
    UserContactResponse getContact(String userId);
}
