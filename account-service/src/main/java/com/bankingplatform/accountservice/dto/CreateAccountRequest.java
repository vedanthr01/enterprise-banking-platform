package com.bankingplatform.accountservice.dto;

import com.bankingplatform.accountservice.entity.AccountType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {
    @NotNull
    private AccountType accountType;

    @Builder.Default
    private String currency = "INR";
}
