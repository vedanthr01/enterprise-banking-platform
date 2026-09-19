package com.bankingplatform.authservice.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/** In-process Spring event, published inside the register() transaction and
 *  picked up (after commit) by {@link RegistrationEventListener} to publish
 *  the Kafka-facing {@code UserRegisteredEvent}. Kept separate from the
 *  Kafka DTO on purpose — this one carries whatever the service layer knows
 *  right now, and is free to evolve independently of the wire contract. */
@Getter
public class UserRegisteredApplicationEvent extends ApplicationEvent {

    private final String userId;
    private final String username;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String phone;

    public UserRegisteredApplicationEvent(String userId, String username, String email,
                                           String firstName, String lastName, String phone) {
        super(userId);
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
    }
}
