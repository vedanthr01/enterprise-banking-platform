package com.bankingplatform.auditservice.messaging;

import com.bankingplatform.auditservice.entity.AuditDomain;
import com.bankingplatform.auditservice.entity.AuditSeverity;
import com.bankingplatform.auditservice.service.AuditEventRecorder;
import com.bankingplatform.common.constants.KafkaTopics;
import com.bankingplatform.common.event.FraudAlertEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Every fraud alert is CRITICAL — by the time Fraud Detection Service has
 *  published one, a rule has already fired, which is inherently the
 *  highest-severity category this platform produces from a compliance
 *  standpoint (regardless of the risk score attached). */
@Component
@RequiredArgsConstructor
public class FraudAuditConsumer {

    private final AuditEventRecorder recorder;

    @KafkaListener(topics = KafkaTopics.FRAUD_ALERTS, groupId = KafkaTopics.FRAUD_ALERTS_GROUP_AUDIT,
            containerFactory = "kafkaListenerContainerFactory")
    public void onFraudAlert(FraudAlertEvent event) {
        String description = "Fraud rule " + event.getRuleTriggered() + " triggered on transaction "
                + event.getTransactionId() + " (account " + event.getAccountId() + ", amount " + event.getAmount()
                + ", risk score " + event.getRiskScore() + "): " + event.getDescription();
        recorder.record(event, AuditDomain.FRAUD, "FRAUD_ALERT_RAISED", event.getTransactionId(),
                event.getUserId(), AuditSeverity.CRITICAL, description);
    }
}
