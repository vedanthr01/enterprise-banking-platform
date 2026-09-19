package com.bankingplatform.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * A failed send here is swallowed, not propagated: this runs inside a Kafka
 * listener, and letting a mail-server outage throw would trigger the
 * consumer's retry/DLT machinery for something far less serious than a lost
 * transaction. NotificationDispatcher records the failure in
 * notification_logs (status FAILED) instead — still visible and auditable,
 * just not something that blocks Kafka progress.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${notification.mail.from}")
    private String fromAddress;

    @Override
    public boolean send(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return true;
        } catch (MailException ex) {
            log.warn("Failed to send email to {}: {}", toEmail, ex.getMessage());
            return false;
        }
    }
}
