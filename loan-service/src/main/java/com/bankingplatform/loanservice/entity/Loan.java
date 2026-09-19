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
import java.time.LocalDate;

@Entity
@Table(name = "loans", indexes = {
        @Index(name = "idx_loan_borrower", columnList = "borrower_user_id"),
        @Index(name = "idx_loan_application", columnList = "application_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "application_id", nullable = false, unique = true, length = 36)
    private String applicationId;

    @Column(name = "borrower_user_id", nullable = false, length = 36)
    private String borrowerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 16)
    private LoanType loanType;

    @Column(name = "disbursement_account_id", nullable = false, length = 36)
    private String disbursementAccountId;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate_annual", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRateAnnual;

    @Column(name = "tenure_months", nullable = false)
    private int tenureMonths;

    @Column(name = "emi_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal emiAmount;

    @Column(name = "outstanding_principal", nullable = false, precision = 19, scale = 4)
    private BigDecimal outstandingPrincipal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LoanStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
