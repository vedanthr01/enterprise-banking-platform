package com.bankingplatform.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Deliberately a local copy of what User Service returns, not a shared
 *  common-module DTO — keeps the two services independently deployable
 *  (same pattern as Account Service's UserKycResponse). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContactResponse {
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
}
