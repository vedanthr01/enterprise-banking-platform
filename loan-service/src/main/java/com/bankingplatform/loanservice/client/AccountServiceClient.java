package com.bankingplatform.loanservice.client;

import com.bankingplatform.common.dto.AccountBalanceResponse;
import com.bankingplatform.common.dto.CreditRequest;
import com.bankingplatform.common.dto.DebitRequest;
import com.bankingplatform.loanservice.config.InternalFeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "account-service", path = "/accounts/internal", configuration = InternalFeignClientConfig.class)
public interface AccountServiceClient {

    @PostMapping("/credit")
    AccountBalanceResponse credit(@RequestBody CreditRequest request);

    @PostMapping("/debit")
    AccountBalanceResponse debit(@RequestBody DebitRequest request);
}
