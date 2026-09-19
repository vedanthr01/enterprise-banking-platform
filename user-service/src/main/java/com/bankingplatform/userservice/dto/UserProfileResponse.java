package com.bankingplatform.userservice.dto;

import com.bankingplatform.userservice.entity.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String addressLine;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private LocalDate dateOfBirth;
    private KycStatus kycStatus;
}
