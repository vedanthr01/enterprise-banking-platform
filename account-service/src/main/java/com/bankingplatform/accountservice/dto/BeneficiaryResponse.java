package com.bankingplatform.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryResponse {
    private String id;
    private String beneficiaryAccountNumber;
    private String beneficiaryName;
    private String nickname;
    private Instant createdAt;
}
