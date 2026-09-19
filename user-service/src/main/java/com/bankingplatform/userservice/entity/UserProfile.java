package com.bankingplatform.userservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/** Extended profile, keyed by the same userId Authentication Service
 *  generates. Created asynchronously off the "user-events" Kafka topic —
 *  it may not exist for the first second or two after registration, which
 *  is an accepted trade-off (see UserRegisteredEventConsumer). */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 128)
    private String email;

    @Column(name = "first_name", nullable = false, length = 64)
    private String firstName;

    @Column(name = "last_name", length = 64)
    private String lastName;

    @Column(length = 20)
    private String phone;

    @Column(name = "address_line", length = 255)
    private String addressLine;

    @Column(length = 64)
    private String city;

    @Column(length = 64)
    private String state;

    @Column(name = "postal_code", length = 16)
    private String postalCode;

    @Column(length = 64)
    private String country;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 16)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
