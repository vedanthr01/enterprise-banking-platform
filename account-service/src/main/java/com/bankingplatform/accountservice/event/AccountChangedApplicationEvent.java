package com.bankingplatform.accountservice.event;

import com.bankingplatform.common.event.AccountEventType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

@Getter
public class AccountChangedApplicationEvent extends ApplicationEvent {

    private final String accountId;
    private final String ownerUserId;
    private final AccountEventType eventType;
    private final BigDecimal amount;
    private final BigDecimal resultingBalance;
    private final String referenceId;

    public AccountChangedApplicationEvent(String accountId, String ownerUserId, AccountEventType eventType,
                                           BigDecimal amount, BigDecimal resultingBalance, String referenceId) {
        super(accountId);
        this.accountId = accountId;
        this.ownerUserId = ownerUserId;
        this.eventType = eventType;
        this.amount = amount;
        this.resultingBalance = resultingBalance;
        this.referenceId = referenceId;
    }
}
