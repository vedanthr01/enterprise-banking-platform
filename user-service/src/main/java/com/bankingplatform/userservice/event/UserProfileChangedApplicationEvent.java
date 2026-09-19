package com.bankingplatform.userservice.event;

import com.bankingplatform.common.event.UserEventType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * In-process Spring event published inside a UserProfileServiceImpl
 * transaction (KYC verification or a profile edit) and picked up after
 * commit by UserProfileEventListener, which publishes the Kafka-facing
 * UserRegisteredEvent — the same publish-inside-transaction /
 * send-after-commit split Authentication Service uses for USER_REGISTERED
 * (see RegistrationEventListener). One class covers both cases since the
 * payload shape — a profile snapshot plus who triggered the change — is
 * identical; only eventType differs.
 */
@Getter
public class UserProfileChangedApplicationEvent extends ApplicationEvent {

    private final UserEventType eventType;
    private final String userId;
    private final String username;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String phone;
    private final String performedByUserId;

    public UserProfileChangedApplicationEvent(UserEventType eventType, String userId, String username,
                                               String email, String firstName, String lastName, String phone,
                                               String performedByUserId) {
        super(userId);
        this.eventType = eventType;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.performedByUserId = performedByUserId;
    }
}
