package com.bankingplatform.notificationservice.service;

import com.bankingplatform.notificationservice.client.UserContactGateway;
import com.bankingplatform.notificationservice.dto.UserContactResponse;
import com.bankingplatform.notificationservice.entity.NotificationLog;
import com.bankingplatform.notificationservice.entity.NotificationStatus;
import com.bankingplatform.notificationservice.entity.NotificationType;
import com.bankingplatform.notificationservice.repository.NotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NotificationDispatcher is the one place idempotency, contact resolution,
 * sending, and logging all meet — so it's the one class in this service
 * worth unit testing in depth. The four Kafka @KafkaListener consumers are
 * intentionally thin (event -> dispatcher call) and are exercised by the
 * Testcontainers integration suite instead.
 */
@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;
    @Mock
    private UserContactGateway userContactGateway;
    @Mock
    private EmailService emailService;

    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new NotificationDispatcher(notificationLogRepository, userContactGateway, emailService);
    }

    @Test
    void dispatch_sendsEmailAndLogsSent_whenRecipientEmailKnown() {
        when(notificationLogRepository.existsBySourceEventId("evt-1")).thenReturn(false);
        when(emailService.send("user@example.com", "Subj", "Body")).thenReturn(true);

        dispatcher.dispatch("evt-1", "user-1", "user@example.com", NotificationType.WELCOME, "Subj", "Body");

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(captor.getValue().getRecipientEmail()).isEqualTo("user@example.com");
        assertThat(captor.getValue().getSentAt()).isNotNull();
    }

    @Test
    void dispatch_logsFailed_whenEmailSendFails() {
        when(notificationLogRepository.existsBySourceEventId("evt-2")).thenReturn(false);
        when(emailService.send(anyString(), anyString(), anyString())).thenReturn(false);

        dispatcher.dispatch("evt-2", "user-1", "user@example.com", NotificationType.ACCOUNT_DEBITED, "Subj", "Body");

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(captor.getValue().getErrorMessage()).isNotBlank();
        assertThat(captor.getValue().getSentAt()).isNull();
    }

    @Test
    void dispatch_logsSkipped_whenNoRecipientEmail() {
        when(notificationLogRepository.existsBySourceEventId("evt-3")).thenReturn(false);

        dispatcher.dispatch("evt-3", "user-1", null, NotificationType.ACCOUNT_CREDITED, "Subj", "Body");

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SKIPPED);
        verify(emailService, never()).send(any(), any(), any());
    }

    @Test
    void dispatch_isIdempotent_whenSourceEventAlreadyProcessed() {
        when(notificationLogRepository.existsBySourceEventId("evt-4")).thenReturn(true);

        dispatcher.dispatch("evt-4", "user-1", "user@example.com", NotificationType.WELCOME, "Subj", "Body");

        verify(notificationLogRepository, never()).save(any());
        verify(emailService, never()).send(any(), any(), any());
    }

    @Test
    void dispatchByUserId_resolvesContactThenSends_whenLookupSucceeds() {
        when(notificationLogRepository.existsBySourceEventId("evt-5")).thenReturn(false);
        when(userContactGateway.getContact("user-1")).thenReturn(
                UserContactResponse.builder().userId("user-1").email("resolved@example.com").build());
        when(emailService.send("resolved@example.com", "Subj", "Body")).thenReturn(true);

        dispatcher.dispatchByUserId("evt-5", "user-1", NotificationType.FRAUD_ALERT, "Subj", "Body");

        verify(emailService).send("resolved@example.com", "Subj", "Body");
    }

    @Test
    void dispatchByUserId_logsSkipped_whenContactLookupReturnsNull() {
        when(notificationLogRepository.existsBySourceEventId("evt-6")).thenReturn(false);
        when(userContactGateway.getContact("user-1")).thenReturn(null);

        dispatcher.dispatchByUserId("evt-6", "user-1", NotificationType.FRAUD_ALERT, "Subj", "Body");

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SKIPPED);
        verify(emailService, never()).send(any(), any(), any());
    }

    @Test
    void dispatchByUserId_skipsContactLookup_whenSourceEventAlreadyProcessed() {
        when(notificationLogRepository.existsBySourceEventId("evt-7")).thenReturn(true);

        dispatcher.dispatchByUserId("evt-7", "user-1", NotificationType.FRAUD_ALERT, "Subj", "Body");

        verify(userContactGateway, never()).getContact(any());
    }
}
