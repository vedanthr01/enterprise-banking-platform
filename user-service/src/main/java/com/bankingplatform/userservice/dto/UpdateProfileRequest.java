package com.bankingplatform.userservice.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 64)
    private String firstName;

    @Size(max = 64)
    private String lastName;

    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String addressLine;

    @Size(max = 64)
    private String city;

    @Size(max = 64)
    private String state;

    @Size(max = 16)
    private String postalCode;

    @Size(max = 64)
    private String country;

    private LocalDate dateOfBirth;
}
