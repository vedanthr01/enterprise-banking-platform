package com.bankingplatform.userservice.dto;

import com.bankingplatform.userservice.entity.KycStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycStatusUpdateRequest {
    @NotNull
    private KycStatus status;
}
