package com.bankingplatform.accountservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddBeneficiaryRequest {
    @NotBlank
    private String beneficiaryAccountNumber;

    @NotBlank
    private String beneficiaryName;

    private String nickname;
}
