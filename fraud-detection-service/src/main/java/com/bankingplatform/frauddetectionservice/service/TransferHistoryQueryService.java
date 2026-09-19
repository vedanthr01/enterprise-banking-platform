package com.bankingplatform.frauddetectionservice.service;

import com.bankingplatform.frauddetectionservice.dto.TransferHistoryResponse;

import java.util.List;

public interface TransferHistoryQueryService {
    /** Most recent 50 transfers this service has recorded from the given
     *  account — the same local ledger the rule engine screens against,
     *  surfaced for fraud analysts investigating why an alert fired. */
    List<TransferHistoryResponse> getForAccount(String accountId);
}
