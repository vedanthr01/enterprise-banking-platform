package com.bankingplatform.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
