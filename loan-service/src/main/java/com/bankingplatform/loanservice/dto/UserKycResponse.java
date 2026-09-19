package com.bankingplatform.loanservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Loan Service's own minimal view of a user's KYC state — deliberately not
 *  shared with User Service's or Account Service's own copies of the same
 *  shape (see the same pattern in Account Service's UserKycResponse). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserKycResponse {
    private String userId;
    private String kycStatus;
}
