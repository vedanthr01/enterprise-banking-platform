package com.bankingplatform.accountservice.repository;

import com.bankingplatform.accountservice.entity.LedgerEntry;
import com.bankingplatform.accountservice.entity.LedgerEntryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    Optional<LedgerEntry> findByAccountIdAndReferenceIdAndEntryType(
            String accountId, String referenceId, LedgerEntryType entryType);

    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(String accountId);
}
