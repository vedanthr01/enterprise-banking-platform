package com.bankingplatform.userservice.service;

import com.bankingplatform.common.event.UserEventType;
import com.bankingplatform.common.exception.ResourceNotFoundException;
import com.bankingplatform.userservice.dto.UpdateProfileRequest;
import com.bankingplatform.userservice.dto.UserProfileResponse;
import com.bankingplatform.userservice.entity.KycStatus;
import com.bankingplatform.userservice.entity.UserProfile;
import com.bankingplatform.userservice.event.UserProfileChangedApplicationEvent;
import com.bankingplatform.userservice.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private UserProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserProfileServiceImpl(userProfileRepository, eventPublisher);
    }

    private UserProfile sampleProfile() {
        return UserProfile.builder()
                .userId("u-1")
                .username("vedanth")
                .email("vedanth@example.com")
                .firstName("Vedanth")
                .lastName("R")
                .kycStatus(KycStatus.PENDING)
                .build();
    }

    @Test
    void getProfile_throwsResourceNotFound_whenProfileMissing() {
        when(userProfileRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_onlyOverwritesSuppliedFields() {
        UserProfile existing = sampleProfile();
        when(userProfileRepository.findById("u-1")).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .city("Bengaluru")
                .country("India")
                .build();

        UserProfileResponse response = service.updateProfile("u-1", request, "u-1");

        assertThat(response.getCity()).isEqualTo("Bengaluru");
        assertThat(response.getCountry()).isEqualTo("India");
        assertThat(response.getFirstName()).isEqualTo("Vedanth");
        assertThat(response.getLastName()).isEqualTo("R");
    }

    @Test
    void updateProfile_publishesProfileUpdatedEvent_attributedToTheActor() {
        when(userProfileRepository.findById("u-1")).thenReturn(Optional.of(sampleProfile()));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateProfile("u-1", UpdateProfileRequest.builder().city("Bengaluru").build(), "admin-9");

        ArgumentCaptor<UserProfileChangedApplicationEvent> captor =
                ArgumentCaptor.forClass(UserProfileChangedApplicationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(UserEventType.PROFILE_UPDATED);
        assertThat(captor.getValue().getUserId()).isEqualTo("u-1");
        // The admin performed the edit, not the user themselves — the audit
        // trail needs to be able to tell those two apart.
        assertThat(captor.getValue().getPerformedByUserId()).isEqualTo("admin-9");
    }

    @Test
    void isKycVerified_returnsFalse_whenStatusIsPending() {
        when(userProfileRepository.findById("u-1")).thenReturn(Optional.of(sampleProfile()));

        assertThat(service.isKycVerified("u-1")).isFalse();
    }

    @Test
    void isKycVerified_returnsTrue_whenStatusIsVerified() {
        UserProfile verified = sampleProfile();
        verified.setKycStatus(KycStatus.VERIFIED);
        when(userProfileRepository.findById("u-1")).thenReturn(Optional.of(verified));

        assertThat(service.isKycVerified("u-1")).isTrue();
    }

    @Test
    void updateKycStatus_publishesKycVerifiedEvent_whenStatusBecomesVerified() {
        when(userProfileRepository.findById("u-1")).thenReturn(Optional.of(sampleProfile()));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateKycStatus("u-1", KycStatus.VERIFIED, "admin-9");

        ArgumentCaptor<UserProfileChangedApplicationEvent> captor =
                ArgumentCaptor.forClass(UserProfileChangedApplicationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(UserEventType.KYC_VERIFIED);
        assertThat(captor.getValue().getPerformedByUserId()).isEqualTo("admin-9");
    }

    @Test
    void updateKycStatus_publishesNoEvent_whenStatusIsRejected() {
        when(userProfileRepository.findById("u-1")).thenReturn(Optional.of(sampleProfile()));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateKycStatus("u-1", KycStatus.REJECTED, "admin-9");

        verify(eventPublisher, never()).publishEvent(any());
    }
}
