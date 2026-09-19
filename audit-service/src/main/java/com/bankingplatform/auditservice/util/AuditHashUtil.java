package com.bankingplatform.auditservice.util;

import com.bankingplatform.auditservice.entity.AuditEvent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Computes a per-record SHA-256 integrity fingerprint over every column of
 * an AuditEvent except the hash itself. Recomputing this later and
 * comparing it against the stored value (see AuditQueryServiceImpl#verifyIntegrity)
 * detects any change to that row made outside the normal insert-only
 * application path.
 *
 * This is deliberately a per-record hash, not a hash *chain* (each row also
 * folding in the previous row's hash, blockchain-style). A chain needs a
 * single, unambiguous "previous record" to fold in, which requires a global
 * serialization point across writers; this service consumes five Kafka
 * listener containers concurrently — one per topic — and is designed to
 * scale horizontally to multiple pod replicas, so there is no single writer
 * to anchor a chain to without adding a distributed lock purely to support
 * it. That would turn an append-only sink into a contention bottleneck for
 * a guarantee this project doesn't need: per-record hashing already proves
 * "this specific row is exactly as it was written," which is what the
 * verify endpoint promises. A single-writer archival/compaction job that
 * chains hashes over already-written, closed date ranges would be the
 * natural way to add chain-of-custody proof later, without that
 * concurrency cost on the hot ingestion path.
 */
public final class AuditHashUtil {

    private static final String ALGORITHM = "SHA-256";

    private AuditHashUtil() {}

    public static String computeHash(AuditEvent event) {
        String raw = String.join("|",
                nullSafe(event.getId()),
                nullSafe(event.getSourceEventId()),
                nullSafe(event.getDomain()),
                nullSafe(event.getEventType()),
                nullSafe(event.getAggregateId()),
                nullSafe(event.getActorUserId()),
                nullSafe(event.getSeverity()),
                nullSafe(event.getDescription()),
                nullSafe(event.getMetadata()),
                nullSafe(event.getOccurredAt()),
                nullSafe(event.getRecordedAt()));

        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is a JDK-guaranteed algorithm on every standard Java
            // platform (Java Cryptography Architecture standard names) —
            // this branch cannot actually be reached.
            throw new IllegalStateException("SHA-256 MessageDigest unavailable", ex);
        }
    }

    private static String nullSafe(Object value) {
        return value == null ? "" : value.toString();
    }
}
