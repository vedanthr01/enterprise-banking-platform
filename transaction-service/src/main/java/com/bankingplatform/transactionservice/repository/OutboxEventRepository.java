package com.bankingplatform.transactionservice.repository;

import com.bankingplatform.transactionservice.entity.OutboxEvent;
import com.bankingplatform.transactionservice.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
