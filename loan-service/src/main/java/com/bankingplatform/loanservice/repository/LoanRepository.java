package com.bankingplatform.loanservice.repository;

import com.bankingplatform.loanservice.entity.Loan;
import com.bankingplatform.loanservice.entity.LoanStatus;
import com.bankingplatform.loanservice.entity.LoanType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, String> {
    boolean existsByBorrowerUserIdAndLoanTypeAndStatus(String borrowerUserId, LoanType loanType, LoanStatus status);
    List<Loan> findByBorrowerUserIdOrderByCreatedAtDesc(String borrowerUserId);
    Optional<Loan> findByApplicationId(String applicationId);
}
