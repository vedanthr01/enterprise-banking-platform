package com.bankingplatform.frauddetectionservice.service;

import com.bankingplatform.frauddetectionservice.dto.TransferHistoryResponse;
import com.bankingplatform.frauddetectionservice.entity.TransferHistoryEntry;
import com.bankingplatform.frauddetectionservice.repository.TransferHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferHistoryQueryServiceImpl implements TransferHistoryQueryService {

    private final TransferHistoryRepository transferHistoryRepository;

    @Override
    public List<TransferHistoryResponse> getForAccount(String accountId) {
        return transferHistoryRepository.findTop50ByFromAccountIdOrderByOccurredAtDesc(accountId).stream()
                .map(this::toResponse)
                .toList();
    }

    private TransferHistoryResponse toResponse(TransferHistoryEntry entry) {
        return TransferHistoryResponse.builder()
                .transactionId(entry.getTransactionId())
                .fromAccountId(entry.getFromAccountId())
                .toAccountId(entry.getToAccountId())
                .amount(entry.getAmount())
                .occurredAt(entry.getOccurredAt())
                .build();
    }
}
