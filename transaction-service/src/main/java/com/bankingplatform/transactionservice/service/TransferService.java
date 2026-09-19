package com.bankingplatform.transactionservice.service;

import com.bankingplatform.transactionservice.dto.TransactionResponse;
import com.bankingplatform.transactionservice.dto.TransferRequest;

import java.util.List;

public interface TransferService {
    TransactionResponse transfer(String initiatedByUserId, TransferRequest request);
    TransactionResponse getTransaction(String transactionId, String requesterUserId, boolean isAdmin);
    List<TransactionResponse> getHistory(String requesterUserId);
}
