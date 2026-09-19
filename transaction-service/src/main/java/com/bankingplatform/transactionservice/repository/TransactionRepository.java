package com.bankingplatform.transactionservice.repository;

import com.bankingplatform.transactionservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    List<Transaction> findByInitiatedByUserIdOrderByCreatedAtDesc(String initiatedByUserId);
    List<Transaction> findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(String fromAccountId, String toAccountId);
}
