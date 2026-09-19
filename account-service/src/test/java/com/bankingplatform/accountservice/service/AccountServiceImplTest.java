package com.bankingplatform.accountservice.service;

import com.bankingplatform.accountservice.entity.Account;
import com.bankingplatform.accountservice.entity.AccountStatus;
import com.bankingplatform.accountservice.entity.AccountType;
import com.bankingplatform.accountservice.entity.LedgerEntry;
import com.bankingplatform.accountservice.entity.LedgerEntryType;
import com.bankingplatform.accountservice.repository.AccountRepository;
import com.bankingplatform.accountservice.repository.BeneficiaryRepository;
import com.bankingplatform.accountservice.repository.LedgerEntryRepository;
import com.bankingplatform.common.dto.AccountBalanceResponse;
import com.bankingplatform.common.dto.DebitRequest;
import com.bankingplatform.common.exception.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private LedgerEntryRepository ledgerEntryRepository;
    @Mock
    private BeneficiaryRepository beneficiaryRepository;
    @Mock
    private AccountNumberGenerator accountNumberGenerator;
    @Mock
    private KycVerificationGateway kycVerificationGateway;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AccountServiceImpl accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(accountRepository, ledgerEntryRepository, beneficiaryRepository,
                accountNumberGenerator, kycVerificationGateway, eventPublisher);
    }

    private Account activeAccount(BigDecimal balance) {
        return Account.builder()
                .id("acc-1")
                .accountNumber("123456789012")
                .ownerUserId("u-1")
                .accountType(AccountType.SAVINGS)
                .balance(balance)
                .currency("INR")
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void debit_succeeds_andPublishesEvent_whenBalanceIsSufficient() {
        Account account = activeAccount(new BigDecimal("100.00"));
        when(accountRepository.findByIdForUpdate("acc-1")).thenReturn(Optional.of(account));
        when(ledgerEntryRepository.findByAccountIdAndReferenceIdAndEntryType("acc-1", "ref-1", LedgerEntryType.DEBIT))
                .thenReturn(Optional.empty());

        DebitRequest request = DebitRequest.builder().accountId("acc-1").amount(new BigDecimal("40.00")).referenceId("ref-1").build();

        AccountBalanceResponse response = accountService.debit(request);

        assertThat(response.getBalance()).isEqualByComparingTo("60.00");
        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void debit_throwsInsufficientBalance_whenAmountExceedsBalance() {
        Account account = activeAccount(new BigDecimal("10.00"));
        when(accountRepository.findByIdForUpdate("acc-1")).thenReturn(Optional.of(account));
        when(ledgerEntryRepository.findByAccountIdAndReferenceIdAndEntryType(eq("acc-1"), eq("ref-2"), any()))
                .thenReturn(Optional.empty());

        DebitRequest request = DebitRequest.builder().accountId("acc-1").amount(new BigDecimal("50.00")).referenceId("ref-2").build();

        assertThatThrownBy(() -> accountService.debit(request)).isInstanceOf(InsufficientBalanceException.class);

        verify(ledgerEntryRepository, never()).save(any());
        // A failed debit still publishes a DEBIT_FAILED event for audit/fraud visibility.
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void debit_isIdempotent_whenReferenceIdAlreadyProcessed() {
        Account account = activeAccount(new BigDecimal("60.00"));
        LedgerEntry priorEntry = LedgerEntry.builder()
                .accountId("acc-1").referenceId("ref-1").entryType(LedgerEntryType.DEBIT)
                .amount(new BigDecimal("40.00")).balanceAfter(new BigDecimal("60.00")).build();

        when(accountRepository.findByIdForUpdate("acc-1")).thenReturn(Optional.of(account));
        when(ledgerEntryRepository.findByAccountIdAndReferenceIdAndEntryType("acc-1", "ref-1", LedgerEntryType.DEBIT))
                .thenReturn(Optional.of(priorEntry));

        DebitRequest request = DebitRequest.builder().accountId("acc-1").amount(new BigDecimal("40.00")).referenceId("ref-1").build();

        AccountBalanceResponse response = accountService.debit(request);

        assertThat(response.getBalance()).isEqualByComparingTo("60.00");
        verify(accountRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
