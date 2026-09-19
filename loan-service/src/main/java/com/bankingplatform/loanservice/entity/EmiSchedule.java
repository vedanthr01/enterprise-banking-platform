package com.bankingplatform.loanservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(name = "emi_schedules", indexes = {
        @Index(name = "idx_emi_loan", columnList = "loan_id"),
        @Index(name = "idx_emi_due_status", columnList = "due_date, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmiSchedule {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "loan_id", nullable = false, length = 36)
    private String loanId;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "emi_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal emiAmount;

    @Column(name = "principal_component", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalComponent;

    @Column(name = "interest_component", nullable = false, precision = 19, scale = 4)
    private BigDecimal interestComponent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EmiStatus status;

    @Column(name = "paid_at")
    private Instant paidAt;
}
