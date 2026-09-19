package com.bankingplatform.common.event;

import java.time.Instant;

/**
 * Common shape every Kafka event payload in this platform follows: a unique
 * event id (for consumer-side idempotency/deduplication) and the instant the
 * source fact actually occurred (as opposed to whenever a consumer happens
 * to process it). Every event class in this package implements it so
 * cross-cutting consumers — notably Audit Service, which persists every
 * event type on every topic uniformly — can depend on one contract instead
 * of switching on concrete classes.
 */
public interface DomainEvent {
    String getEventId();
    Instant getOccurredAt();
}
