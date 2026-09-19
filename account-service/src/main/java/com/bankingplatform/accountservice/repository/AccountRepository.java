package com.bankingplatform.accountservice.repository;

import com.bankingplatform.accountservice.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByOwnerUserId(String ownerUserId);

    boolean existsByAccountNumber(String accountNumber);

    /** Pessimistic row lock (SELECT ... FOR UPDATE) — held for the duration
     *  of the caller's transaction so two concurrent debits/credits against
     *  the same account serialize instead of racing on a stale balance. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") String id);
}
