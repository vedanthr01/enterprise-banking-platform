package com.bankingplatform.transactionservice.client;

import com.bankingplatform.common.dto.AccountBalanceResponse;
import com.bankingplatform.common.dto.CompensateRequest;
import com.bankingplatform.common.dto.CreditRequest;
import com.bankingplatform.common.dto.DebitRequest;
import com.bankingplatform.transactionservice.config.InternalFeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "account-service", contextId = "accountServiceInternalClient",
        path = "/accounts/internal", configuration = InternalFeignClientConfig.class)
public interface AccountServiceClient {

    @PostMapping("/debit")
    AccountBalanceResponse debit(@RequestBody DebitRequest request);

    @PostMapping("/credit")
    AccountBalanceResponse credit(@RequestBody CreditRequest request);

    @PostMapping("/compensate")
    AccountBalanceResponse compensate(@RequestBody CompensateRequest request);

    @GetMapping("/{accountId}")
    AccountBalanceResponse getAccount(@PathVariable("accountId") String accountId);
}
