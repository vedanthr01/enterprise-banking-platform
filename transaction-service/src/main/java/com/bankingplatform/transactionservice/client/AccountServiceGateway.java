package com.bankingplatform.transactionservice.client;

import com.bankingplatform.common.dto.AccountBalanceResponse;
import com.bankingplatform.common.dto.CompensateRequest;
import com.bankingplatform.common.dto.CreditRequest;
import com.bankingplatform.common.dto.DebitRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Thin resilience layer around the Feign client, in its own bean so the
 * @Retry / @CircuitBreaker annotations go through the Spring AOP proxy
 * (self-invocation from within TransferServiceImpl would silently skip
 * them). Retrying a timed-out debit/credit/compensate call is only safe
 * because the Account Service endpoints underneath are idempotent on
 * referenceId — a retry after a lost response re-applies nothing and just
 * returns the original outcome.
 */
@Component
@RequiredArgsConstructor
public class AccountServiceGateway {

    private final AccountServiceClient accountServiceClient;

    @Retry(name = "accountServiceRetry")
    @CircuitBreaker(name = "accountServiceCB")
    public AccountBalanceResponse debit(DebitRequest request) {
        return accountServiceClient.debit(request);
    }

    @Retry(name = "accountServiceRetry")
    @CircuitBreaker(name = "accountServiceCB")
    public AccountBalanceResponse credit(CreditRequest request) {
        return accountServiceClient.credit(request);
    }

    @Retry(name = "accountServiceRetry")
    @CircuitBreaker(name = "accountServiceCB")
    public AccountBalanceResponse compensate(CompensateRequest request) {
        return accountServiceClient.compensate(request);
    }
}
