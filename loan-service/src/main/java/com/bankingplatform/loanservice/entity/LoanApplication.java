package com.bankingplatform.loanservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "loan_applications", indexes = {
        @Index(name = "idx_loan_app_applicant", columnList = "applicant_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplication {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "applicant_user_id", nullable = false, length = 36)
    private String applicantUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 16)
    private LoanType loanType;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate_annual", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRateAnnual;

    @Column(name = "tenure_months", nullable = false)
    private int tenureMonths;

    @Column(length = 255)
    private String purpose;

    @Column(name = "disbursement_account_id", nullable = false, length = 36)
    private String disbursementAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private LoanApplicationStatus status;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
