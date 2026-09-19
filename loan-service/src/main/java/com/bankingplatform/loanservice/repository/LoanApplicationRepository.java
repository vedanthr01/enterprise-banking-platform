package com.bankingplatform.loanservice.repository;

import com.bankingplatform.loanservice.entity.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, String> {
    List<LoanApplication> findByApplicantUserIdOrderByCreatedAtDesc(String applicantUserId);
}
