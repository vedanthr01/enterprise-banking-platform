package com.bankingplatform.notificationservice.service;

public interface EmailService {
    /** Returns true if the mail server accepted the message, false on any
     *  failure. Never throws — the caller decides how to record the result. */
    boolean send(String toEmail, String subject, String body);
}
