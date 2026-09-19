package com.bankingplatform.accountservice.service;

import com.bankingplatform.accountservice.dto.AccountResponse;
import com.bankingplatform.accountservice.dto.AddBeneficiaryRequest;
import com.bankingplatform.accountservice.dto.BeneficiaryResponse;
import com.bankingplatform.accountservice.dto.CreateAccountRequest;
import com.bankingplatform.accountservice.dto.CreateFixedDepositRequest;
import com.bankingplatform.common.dto.AccountBalanceResponse;
import com.bankingplatform.common.dto.CompensateRequest;
import com.bankingplatform.common.dto.CreditRequest;
import com.bankingplatform.common.dto.DebitRequest;

import java.util.List;

public interface AccountService {

    AccountResponse createAccount(String ownerUserId, CreateAccountRequest request);

    AccountResponse createFixedDeposit(String ownerUserId, CreateFixedDepositRequest request);

    List<AccountResponse> listAccounts(String ownerUserId);

    AccountResponse getAccount(String accountId);

    AccountBalanceResponse getCachedBalance(String accountId);

    AccountBalanceResponse debit(DebitRequest request);

    AccountBalanceResponse credit(CreditRequest request);

    AccountBalanceResponse compensate(CompensateRequest request);

    BeneficiaryResponse addBeneficiary(String ownerUserId, AddBeneficiaryRequest request);

    List<BeneficiaryResponse> listBeneficiaries(String ownerUserId);

    void removeBeneficiary(String ownerUserId, String beneficiaryId);
}
