package com.bankingplatform.loanservice.repository;

import com.bankingplatform.loanservice.entity.EmiSchedule;
import com.bankingplatform.loanservice.entity.EmiStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EmiScheduleRepository extends JpaRepository<EmiSchedule, String> {
    List<EmiSchedule> findByLoanIdOrderByInstallmentNumberAsc(String loanId);

    List<EmiSchedule> findTop100ByStatusInAndDueDateLessThanEqualOrderByDueDateAsc(
            List<EmiStatus> statuses, LocalDate date);
}
