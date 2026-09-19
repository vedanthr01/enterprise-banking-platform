package com.bankingplatform.accountservice.service;

import com.bankingplatform.accountservice.dto.AccountResponse;
import com.bankingplatform.accountservice.dto.AddBeneficiaryRequest;
import com.bankingplatform.accountservice.dto.BeneficiaryResponse;
import com.bankingplatform.accountservice.dto.CreateAccountRequest;
import com.bankingplatform.accountservice.dto.CreateFixedDepositRequest;
import com.bankingplatform.accountservice.entity.Account;
import com.bankingplatform.accountservice.entity.AccountStatus;
import com.bankingplatform.accountservice.entity.AccountType;
import com.bankingplatform.accountservice.entity.Beneficiary;
import com.bankingplatform.accountservice.entity.LedgerEntry;
import com.bankingplatform.accountservice.entity.LedgerEntryType;
import com.bankingplatform.accountservice.event.AccountChangedApplicationEvent;
import com.bankingplatform.accountservice.repository.AccountRepository;
import com.bankingplatform.accountservice.repository.BeneficiaryRepository;
import com.bankingplatform.accountservice.repository.LedgerEntryRepository;
import com.bankingplatform.common.dto.AccountBalanceResponse;
import com.bankingplatform.common.dto.CompensateRequest;
import com.bankingplatform.common.dto.CreditRequest;
import com.bankingplatform.common.dto.DebitRequest;
import com.bankingplatform.common.event.AccountEventType;
import com.bankingplatform.common.exception.BusinessException;
import com.bankingplatform.common.exception.InsufficientBalanceException;
import com.bankingplatform.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final KycVerificationGateway kycVerificationGateway;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public AccountResponse createAccount(String ownerUserId, CreateAccountRequest request) {
        if (request.getAccountType() == AccountType.FIXED_DEPOSIT) {
            throw new BusinessException("Use POST /accounts/fixed-deposits to open a fixed deposit");
        }

        kycVerificationGateway.ensureVerified(ownerUserId);

        Account account = Account.builder()
                .id(UUID.randomUUID().toString())
                .accountNumber(accountNumberGenerator.generate())
                .ownerUserId(ownerUserId)
                .accountType(request.getAccountType())
                .balance(BigDecimal.ZERO)
                .currency(request.getCurrency() == null ? "INR" : request.getCurrency())
                .status(AccountStatus.ACTIVE)
                .build();
        accountRepository.save(account);

        eventPublisher.publishEvent(new AccountChangedApplicationEvent(
                account.getId(), ownerUserId, AccountEventType.ACCOUNT_CREATED,
                BigDecimal.ZERO, BigDecimal.ZERO, account.getId()));

        return toResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse createFixedDeposit(String ownerUserId, CreateFixedDepositRequest request) {
        Account source = accountRepository.findByIdForUpdate(request.getSourceAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));

        if (!source.getOwnerUserId().equals(ownerUserId)) {
            throw new BusinessException("Source account does not belong to you", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
        if (source.getBalance().compareTo(request.getPrincipalAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance to open this fixed deposit");
        }

        String referenceId = "FD-" + UUID.randomUUID();
        applyDebit(source, request.getPrincipalAmount(), referenceId);
        eventPublisher.publishEvent(new AccountChangedApplicationEvent(
                source.getId(), ownerUserId, AccountEventType.BALANCE_DEBITED,
                request.getPrincipalAmount(), source.getBalance(), referenceId));

        Account fd = Account.builder()
                .id(UUID.randomUUID().toString())
                .accountNumber(accountNumberGenerator.generate())
                .ownerUserId(ownerUserId)
                .accountType(AccountType.FIXED_DEPOSIT)
                .balance(request.getPrincipalAmount())
                .currency(source.getCurrency())
                .status(AccountStatus.ACTIVE)
                .tenureMonths(request.getTenureMonths())
                .maturityDate(LocalDate.now().plusMonths(request.getTenureMonths()))
                .interestRate(interestRateFor(request.getTenureMonths()))
                .build();
        accountRepository.save(fd);

        eventPublisher.publishEvent(new AccountChangedApplicationEvent(
                fd.getId(), ownerUserId, AccountEventType.ACCOUNT_CREATED,
                request.getPrincipalAmount(), request.getPrincipalAmount(), fd.getId()));

        return toResponse(fd);
    }

    @Override
    public List<AccountResponse> listAccounts(String ownerUserId) {
        return accountRepository.findByOwnerUserId(ownerUserId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AccountResponse getAccount(String accountId) {
        return toResponse(findAccountOrThrow(accountId));
    }

    @Override
    @Cacheable(value = "accountBalance", key = "#accountId")
    public AccountBalanceResponse getCachedBalance(String accountId) {
        return toBalanceResponse(findAccountOrThrow(accountId));
    }

    @Override
    @Transactional
    @CacheEvict(value = "accountBalance", key = "#request.accountId")
    public AccountBalanceResponse debit(DebitRequest request) {
        Account account = accountRepository.findByIdForUpdate(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.getAccountId()));

        Optional<LedgerEntry> existing = ledgerEntryRepository.findByAccountIdAndReferenceIdAndEntryType(
                request.getAccountId(), request.getReferenceId(), LedgerEntryType.DEBIT);
        if (existing.isPresent()) {
            log.info("Duplicate debit for reference {} on account {} — returning prior result (idempotent)",
                    request.getReferenceId(), request.getAccountId());
            return toBalanceResponse(account);
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Account " + account.getId() + " is not active");
        }
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            eventPublisher.publishEvent(new AccountChangedApplicationEvent(
                    account.getId(), account.getOwnerUserId(), AccountEventType.DEBIT_FAILED,
                    request.getAmount(), account.getBalance(), request.getReferenceId()));
            throw new InsufficientBalanceException("Insufficient balance in account " + account.getAccountNumber());
        }

        applyDebit(account, request.getAmount(), request.getReferenceId());

        eventPublisher.publishEvent(new AccountChangedApplicationEvent(
                account.getId(), account.getOwnerUserId(), AccountEventType.BALANCE_DEBITED,
                request.getAmount(), account.getBalance(), request.getReferenceId()));

        return toBalanceResponse(account);
    }

    @Override
    @Transactional
    @CacheEvict(value = "accountBalance", key = "#request.accountId")
    public AccountBalanceResponse credit(CreditRequest request) {
        Account account = accountRepository.findByIdForUpdate(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.getAccountId()));

        Optional<LedgerEntry> existing = ledgerEntryRepository.findByAccountIdAndReferenceIdAndEntryType(
                request.getAccountId(), request.getReferenceId(), LedgerEntryType.CREDIT);
        if (existing.isPresent()) {
            log.info("Duplicate credit for reference {} on account {} — returning prior result (idempotent)",
                    request.getReferenceId(), request.getAccountId());
            return toBalanceResponse(account);
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Account " + account.getId() + " is not active");
        }

        applyCredit(account, request.getAmount(), request.getReferenceId(), LedgerEntryType.CREDIT);

        eventPublisher.publishEvent(new AccountChangedApplicationEvent(
                account.getId(), account.getOwnerUserId(), AccountEventType.BALANCE_CREDITED,
                request.getAmount(), account.getBalance(), request.getReferenceId()));

        return toBalanceResponse(account);
    }

    @Override
    @Transactional
    @CacheEvict(value = "accountBalance", key = "#request.accountId")
    public AccountBalanceResponse compensate(CompensateRequest request) {
        Account account = accountRepository.findByIdForUpdate(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.getAccountId()));

        Optional<LedgerEntry> existing = ledgerEntryRepository.findByAccountIdAndReferenceIdAndEntryType(
                request.getAccountId(), request.getReferenceId(), LedgerEntryType.COMPENSATE_DEBIT);
        if (existing.isPresent()) {
            log.info("Duplicate compensation for reference {} on account {} — returning prior result (idempotent)",
                    request.getReferenceId(), request.getAccountId());
            return toBalanceResponse(account);
        }

        applyCredit(account, request.getAmount(), request.getReferenceId(), LedgerEntryType.COMPENSATE_DEBIT);

        eventPublisher.publishEvent(new AccountChangedApplicationEvent(
                account.getId(), account.getOwnerUserId(), AccountEventType.DEBIT_COMPENSATED,
                request.getAmount(), account.getBalance(), request.getReferenceId()));

        log.warn("Compensated {} on account {} for reference {} ({})",
                request.getAmount(), account.getId(), request.getReferenceId(), request.getReason());

        return toBalanceResponse(account);
    }

    @Override
    @Transactional
    public BeneficiaryResponse addBeneficiary(String ownerUserId, AddBeneficiaryRequest request) {
        if (!accountRepository.existsByAccountNumber(request.getBeneficiaryAccountNumber())) {
            throw new ResourceNotFoundException("No account found with number " + request.getBeneficiaryAccountNumber());
        }

        Beneficiary beneficiary = Beneficiary.builder()
                .id(UUID.randomUUID().toString())
                .ownerUserId(ownerUserId)
                .beneficiaryAccountNumber(request.getBeneficiaryAccountNumber())
                .beneficiaryName(request.getBeneficiaryName())
                .nickname(request.getNickname())
                .build();
        beneficiaryRepository.save(beneficiary);

        return toBeneficiaryResponse(beneficiary);
    }

    @Override
    public List<BeneficiaryResponse> listBeneficiaries(String ownerUserId) {
        return beneficiaryRepository.findByOwnerUserId(ownerUserId).stream()
                .map(this::toBeneficiaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeBeneficiary(String ownerUserId, String beneficiaryId) {
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndOwnerUserId(beneficiaryId, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found"));
        beneficiaryRepository.delete(beneficiary);
    }

    // -- internal helpers --------------------------------------------------

    private void applyDebit(Account account, BigDecimal amount, String referenceId) {
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        ledgerEntryRepository.save(LedgerEntry.builder()
                .accountId(account.getId())
                .referenceId(referenceId)
                .entryType(LedgerEntryType.DEBIT)
                .amount(amount)
                .balanceAfter(account.getBalance())
                .build());
    }

    private void applyCredit(Account account, BigDecimal amount, String referenceId, LedgerEntryType type) {
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        ledgerEntryRepository.save(LedgerEntry.builder()
                .accountId(account.getId())
                .referenceId(referenceId)
                .entryType(type)
                .amount(amount)
                .balanceAfter(account.getBalance())
                .build());
    }

    private BigDecimal interestRateFor(int tenureMonths) {
        if (tenureMonths < 6) return new BigDecimal("5.50");
        if (tenureMonths < 12) return new BigDecimal("6.00");
        if (tenureMonths < 24) return new BigDecimal("6.50");
        return new BigDecimal("7.00");
    }

    private Account findAccountOrThrow(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
    }

    private AccountResponse toResponse(Account a) {
        return AccountResponse.builder()
                .id(a.getId())
                .accountNumber(a.getAccountNumber())
                .ownerUserId(a.getOwnerUserId())
                .accountType(a.getAccountType())
                .balance(a.getBalance())
                .currency(a.getCurrency())
                .status(a.getStatus())
                .maturityDate(a.getMaturityDate())
                .interestRate(a.getInterestRate())
                .tenureMonths(a.getTenureMonths())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private AccountBalanceResponse toBalanceResponse(Account a) {
        return AccountBalanceResponse.builder()
                .accountId(a.getId())
                .accountNumber(a.getAccountNumber())
                .ownerUserId(a.getOwnerUserId())
                .balance(a.getBalance())
                .currency(a.getCurrency())
                .status(a.getStatus().name())
                .build();
    }

    private BeneficiaryResponse toBeneficiaryResponse(Beneficiary b) {
        return BeneficiaryResponse.builder()
                .id(b.getId())
                .beneficiaryAccountNumber(b.getBeneficiaryAccountNumber())
                .beneficiaryName(b.getBeneficiaryName())
                .nickname(b.getNickname())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
