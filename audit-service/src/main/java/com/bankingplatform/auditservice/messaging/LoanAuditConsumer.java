package com.bankingplatform.auditservice.messaging;

import com.bankingplatform.auditservice.entity.AuditDomain;
import com.bankingplatform.auditservice.entity.AuditSeverity;
import com.bankingplatform.auditservice.service.AuditEventRecorder;
import com.bankingplatform.common.constants.KafkaTopics;
import com.bankingplatform.common.event.LoanEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoanAuditConsumer {

    private final AuditEventRecorder recorder;

    @KafkaListener(topics = KafkaTopics.LOAN_EVENTS, groupId = KafkaTopics.LOAN_EVENTS_GROUP_AUDIT,
            containerFactory = "kafkaListenerContainerFactory")
    public void onLoanEvent(LoanEvent event) {
        AuditSeverity severity = switch (event.getEventType()) {
            case LOAN_APPLICATION_APPROVED, LOAN_DISBURSED, EMI_PAYMENT_SUCCEEDED, LOAN_CLOSED -> AuditSeverity.INFO;
            case LOAN_APPLICATION_REJECTED, EMI_PAYMENT_FAILED -> AuditSeverity.WARNING;
        };
        String description = switch (event.getEventType()) {
            case LOAN_APPLICATION_APPROVED -> "Loan application " + event.getApplicationId()
                    + " approved by underwriting for " + event.getAmount();
            case LOAN_APPLICATION_REJECTED -> "Loan application " + event.getApplicationId()
                    + " rejected: " + event.getReason();
            case LOAN_DISBURSED -> "Loan " + event.getLoanId() + " disbursed for " + event.getAmount()
                    + " (application " + event.getApplicationId() + ")";
            case EMI_PAYMENT_SUCCEEDED -> "EMI of " + event.getAmount() + " collected for loan " + event.getLoanId();
            case EMI_PAYMENT_FAILED -> "EMI of " + event.getAmount() + " failed for loan " + event.getLoanId()
                    + ": " + event.getReason();
            case LOAN_CLOSED -> "Loan " + event.getLoanId() + " fully repaid and closed";
        };
        // loanId doesn't exist yet for application-stage events (a loan
        // record isn't created until disbursement) — fall back to
        // applicationId so every audit row still has a non-null aggregate
        // to search by.
        String aggregateId = event.getLoanId() != null ? event.getLoanId() : event.getApplicationId();
        recorder.record(event, AuditDomain.LOAN, event.getEventType().name(), aggregateId,
                event.getBorrowerUserId(), severity, description);
    }
}
