package com.bankingplatform.loanservice.client;

import com.bankingplatform.common.dto.AccountBalanceResponse;
import com.bankingplatform.common.dto.CreditRequest;
import com.bankingplatform.common.dto.DebitRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Own bean so @Retry/@CircuitBreaker go through the Spring AOP proxy (see
 *  the same self-invocation note used throughout this codebase). Retrying a
 *  timed-out call is safe because Account Service's debit/credit endpoints
 *  are idempotent on referenceId — disbursement uses the application id,
 *  EMI auto-debit uses the schedule row id. */
@Component
@RequiredArgsConstructor
public class AccountServiceGateway {

    private final AccountServiceClient accountServiceClient;

    @Retry(name = "accountServiceRetry")
    @CircuitBreaker(name = "accountServiceCB")
    public AccountBalanceResponse credit(CreditRequest request) {
        return accountServiceClient.credit(request);
    }

    @Retry(name = "accountServiceRetry")
    @CircuitBreaker(name = "accountServiceCB")
    public AccountBalanceResponse debit(DebitRequest request) {
        return accountServiceClient.debit(request);
    }
}
