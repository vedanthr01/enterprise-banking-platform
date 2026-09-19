package com.bankingplatform.userservice.service;

import com.bankingplatform.common.event.UserEventType;
import com.bankingplatform.common.exception.ResourceNotFoundException;
import com.bankingplatform.userservice.dto.UpdateProfileRequest;
import com.bankingplatform.userservice.dto.UserContactResponse;
import com.bankingplatform.userservice.dto.UserProfileResponse;
import com.bankingplatform.userservice.entity.KycStatus;
import com.bankingplatform.userservice.entity.UserProfile;
import com.bankingplatform.userservice.event.UserProfileChangedApplicationEvent;
import com.bankingplatform.userservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public UserProfileResponse getProfile(String userId) {
        return toResponse(findOrThrow(userId));
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request, String actorUserId) {
        UserProfile profile = findOrThrow(userId);

        if (StringUtils.hasText(request.getFirstName())) profile.setFirstName(request.getFirstName());
        if (StringUtils.hasText(request.getLastName())) profile.setLastName(request.getLastName());
        if (StringUtils.hasText(request.getPhone())) profile.setPhone(request.getPhone());
        if (StringUtils.hasText(request.getAddressLine())) profile.setAddressLine(request.getAddressLine());
        if (StringUtils.hasText(request.getCity())) profile.setCity(request.getCity());
        if (StringUtils.hasText(request.getState())) profile.setState(request.getState());
        if (StringUtils.hasText(request.getPostalCode())) profile.setPostalCode(request.getPostalCode());
        if (StringUtils.hasText(request.getCountry())) profile.setCountry(request.getCountry());
        if (request.getDateOfBirth() != null) profile.setDateOfBirth(request.getDateOfBirth());

        UserProfile saved = userProfileRepository.save(profile);
        eventPublisher.publishEvent(new UserProfileChangedApplicationEvent(UserEventType.PROFILE_UPDATED,
                saved.getUserId(), saved.getUsername(), saved.getEmail(), saved.getFirstName(),
                saved.getLastName(), saved.getPhone(), actorUserId));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public UserProfileResponse updateKycStatus(String userId, KycStatus status, String actorUserId) {
        UserProfile profile = findOrThrow(userId);
        profile.setKycStatus(status);
        UserProfile saved = userProfileRepository.save(profile);

        // Only VERIFIED is a fact worth putting on the wire — there is no
        // UserEventType for "rejected" (a rejection isn't a state other
        // services or the audit trail's user-domain consumer currently need
        // to react to beyond what the profile record itself already shows).
        if (status == KycStatus.VERIFIED) {
            eventPublisher.publishEvent(new UserProfileChangedApplicationEvent(UserEventType.KYC_VERIFIED,
                    saved.getUserId(), saved.getUsername(), saved.getEmail(), saved.getFirstName(),
                    saved.getLastName(), saved.getPhone(), actorUserId));
        }
        return toResponse(saved);
    }

    @Override
    public boolean isKycVerified(String userId) {
        return userProfileRepository.findById(userId)
                .map(p -> p.getKycStatus() == KycStatus.VERIFIED)
                .orElse(false);
    }

    @Override
    public UserContactResponse getContact(String userId) {
        UserProfile profile = findOrThrow(userId);
        return UserContactResponse.builder()
                .userId(profile.getUserId())
                .email(profile.getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .phone(profile.getPhone())
                .build();
    }

    private UserProfile findOrThrow(String userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No profile found for user " + userId));
    }

    private UserProfileResponse toResponse(UserProfile p) {
        return UserProfileResponse.builder()
                .userId(p.getUserId())
                .username(p.getUsername())
                .email(p.getEmail())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .phone(p.getPhone())
                .addressLine(p.getAddressLine())
                .city(p.getCity())
                .state(p.getState())
                .postalCode(p.getPostalCode())
                .country(p.getCountry())
                .dateOfBirth(p.getDateOfBirth())
                .kycStatus(p.getKycStatus())
                .build();
    }
}
