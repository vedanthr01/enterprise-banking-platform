package com.bankingplatform.frauddetectionservice.repository;

import com.bankingplatform.frauddetectionservice.entity.TransferHistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TransferHistoryRepository extends JpaRepository<TransferHistoryEntry, String> {
    boolean existsBySourceEventId(String sourceEventId);
    boolean existsByFromAccountIdAndToAccountId(String fromAccountId, String toAccountId);
    long countByFromAccountIdAndOccurredAtAfter(String fromAccountId, Instant after);
    List<TransferHistoryEntry> findTop50ByFromAccountIdOrderByOccurredAtDesc(String fromAccountId);
}
