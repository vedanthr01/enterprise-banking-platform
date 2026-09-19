package com.bankingplatform.loanservice.repository;

import com.bankingplatform.loanservice.entity.OutboxEvent;
import com.bankingplatform.loanservice.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
