package com.bankingplatform.loanservice.scheduler;

import com.bankingplatform.loanservice.entity.EmiSchedule;
import com.bankingplatform.loanservice.entity.EmiStatus;
import com.bankingplatform.loanservice.repository.EmiScheduleRepository;
import com.bankingplatform.loanservice.service.EmiInstallmentProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Cron entry point only — the actual per-installment work lives in
 * EmiInstallmentProcessor, a separate bean so each installment gets its own
 * @Transactional boundary (see the class-level note there). Capped at the
 * 100 earliest-due installments per run rather than an unbounded batch in
 * one pass; anything left over stays PENDING/OVERDUE and is picked up on
 * the next run.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmiAutoDebitScheduler {

    private final EmiScheduleRepository emiScheduleRepository;
    private final EmiInstallmentProcessor emiInstallmentProcessor;

    @Scheduled(cron = "${loan.emi.auto-debit-cron:0 0 6 * * *}")
    public void runDueEmiDebits() {
        List<EmiSchedule> due = emiScheduleRepository.findTop100ByStatusInAndDueDateLessThanEqualOrderByDueDateAsc(
                List.of(EmiStatus.PENDING, EmiStatus.OVERDUE), LocalDate.now());

        if (due.isEmpty()) {
            return;
        }
        log.info("EMI auto-debit run: {} installment(s) due", due.size());
        for (EmiSchedule installment : due) {
            emiInstallmentProcessor.process(installment.getId());
        }
    }
}
