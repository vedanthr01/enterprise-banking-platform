package com.bankingplatform.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Published to the "user-events" topic for every UserEventType, despite the
 * class name (kept as-is since it was introduced for USER_REGISTERED first
 * and renaming now would touch every consumer for no functional gain). User
 * Service creates the extended profile asynchronously off USER_REGISTERED —
 * registration itself stays fast and self-contained inside Authentication
 * Service, and the profile is eventually consistent, an acceptable
 * trade-off since nothing money-related depends on it being immediately
 * present. KYC_VERIFIED and PROFILE_UPDATED reuse this same envelope
 * (published by User Service) so every consumer of this topic — including
 * Audit Service — only ever needs to handle one payload class.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent implements Serializable, DomainEvent {
    private String eventId;
    private UserEventType eventType;
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    /** Who caused this change — the new user themselves for USER_REGISTERED
     *  (self-service registration), but for KYC_VERIFIED / PROFILE_UPDATED
     *  this can be an admin acting on the user's profile rather than the
     *  user, which is exactly the distinction an audit trail needs to
     *  preserve. */
    private String performedByUserId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occurredAt;
}
