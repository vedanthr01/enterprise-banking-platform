package com.bankingplatform.loanservice.entity;

public enum LoanApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    DISBURSED,
    /** Underwriting approved it, but the disbursement credit call to
     *  Account Service failed — distinct from REJECTED on purpose: this is
     *  an execution failure that needs an ops retry, not a "no" decision on
     *  the merits. */
    DISBURSEMENT_FAILED
}
