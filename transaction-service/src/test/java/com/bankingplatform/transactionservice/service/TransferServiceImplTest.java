package com.bankingplatform.transactionservice.service;

import com.bankingplatform.common.dto.AccountBalanceResponse;
import com.bankingplatform.common.exception.BusinessException;
import com.bankingplatform.transactionservice.client.AccountServiceGateway;
import com.bankingplatform.transactionservice.dto.TransactionResponse;
import com.bankingplatform.transactionservice.dto.TransferRequest;
import com.bankingplatform.transactionservice.entity.Transaction;
import com.bankingplatform.transactionservice.entity.TransactionStatus;
import com.bankingplatform.transactionservice.repository.OutboxEventRepository;
import com.bankingplatform.transactionservice.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the fund-transfer Saga. Account Service is entirely mocked
 * out via AccountServiceGateway, so these tests exercise only the
 * orchestration logic in TransferServiceImpl: idempotent replay, the
 * happy path, each failure branch (debit fails / credit fails then
 * compensation succeeds / credit fails then compensation ALSO fails), and
 * the same-account guard. The Testcontainers-backed integration test
 * (task #9) exercises the real HTTP + Kafka + MySQL wiring end to end.
 */
@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private AccountServiceGateway accountServiceGateway;
    @Mock
    private ObjectMapper objectMapper;

    private TransferServiceImpl transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferServiceImpl(transactionRepository, outboxEventRepository,
                accountServiceGateway, objectMapper);
    }

    private TransferRequest request(String idempotencyKey) {
        return TransferRequest.builder()
                .fromAccountId("acc-from")
                .toAccountId("acc-to")
                .amount(new BigDecimal("100.00"))
                .currency("INR")
                .idempotencyKey(idempotencyKey)
                .build();
    }

    private AccountBalanceResponse balanceResponse(String accountId, String balance) {
        return AccountBalanceResponse.builder()
                .accountId(accountId)
                .balance(new BigDecimal(balance))
                .currency("INR")
                .status("ACTIVE")
                .build();
    }

    @Test
    void transfer_completesSuccessfully_whenDebitAndCreditBothSucceed() throws Exception {
        when(transactionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(accountServiceGateway.debit(any())).thenReturn(balanceResponse("acc-from", "900.00"));
        when(accountServiceGateway.credit(any())).thenReturn(balanceResponse("acc-to", "1100.00"));

        TransactionResponse response = transferService.transfer("user-1", request("key-1"));

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.getFailureReason()).isNull();
        verify(accountServiceGateway).debit(any());
        verify(accountServiceGateway).credit(any());
        verify(accountServiceGateway, never()).compensate(any());
        // One outbox write for TRANSFER_INITIATED, one for TRANSFER_COMPLETED.
        verify(outboxEventRepository, times(2)).save(any());
        // One save for the initial INITIATED row, one for the final COMPLETED update.
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transfer_marksFailed_whenDebitFails() throws Exception {
        when(transactionRepository.findByIdempotencyKey("key-2")).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(accountServiceGateway.debit(any())).thenThrow(new RuntimeException("insufficient funds"));

        TransactionResponse response = transferService.transfer("user-1", request("key-2"));

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(response.getFailureReason()).contains("Debit failed").contains("insufficient funds");
        verify(accountServiceGateway, never()).credit(any());
        verify(accountServiceGateway, never()).compensate(any());
    }

    @Test
    void transfer_compensatesDebit_whenCreditFailsButReversalSucceeds() throws Exception {
        when(transactionRepository.findByIdempotencyKey("key-3")).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(accountServiceGateway.debit(any())).thenReturn(balanceResponse("acc-from", "900.00"));
        when(accountServiceGateway.credit(any())).thenThrow(new RuntimeException("account frozen"));
        when(accountServiceGateway.compensate(any())).thenReturn(balanceResponse("acc-from", "1000.00"));

        TransactionResponse response = transferService.transfer("user-1", request("key-3"));

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPENSATED);
        assertThat(response.getFailureReason()).contains("Credit failed").contains("account frozen");
        verify(accountServiceGateway).compensate(any());
    }

    @Test
    void transfer_marksFailedCritical_whenCreditFailsAndCompensationAlsoFails() throws Exception {
        when(transactionRepository.findByIdempotencyKey("key-4")).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(accountServiceGateway.debit(any())).thenReturn(balanceResponse("acc-from", "900.00"));
        when(accountServiceGateway.credit(any())).thenThrow(new RuntimeException("account frozen"));
        when(accountServiceGateway.compensate(any())).thenThrow(new RuntimeException("account service down"));

        TransactionResponse response = transferService.transfer("user-1", request("key-4"));

        // Debit succeeded, credit failed, and reversing the debit ALSO
        // failed — this must never look like a normal failure. It has to
        // surface loudly for manual reconciliation, not get silently
        // swallowed or misreported as merely FAILED-and-safe.
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(response.getFailureReason())
                .contains("CRITICAL")
                .contains("account frozen")
                .contains("account service down")
                .contains("manual reconciliation");
    }

    @Test
    void transfer_returnsExistingResult_whenIdempotencyKeyAlreadyProcessed() {
        Transaction existing = Transaction.builder()
                .id("txn-1")
                .idempotencyKey("key-5")
                .fromAccountId("acc-from")
                .toAccountId("acc-to")
                .amount(new BigDecimal("100.00"))
                .currency("INR")
                .status(TransactionStatus.COMPLETED)
                .initiatedByUserId("user-1")
                .build();
        when(transactionRepository.findByIdempotencyKey("key-5")).thenReturn(Optional.of(existing));

        TransactionResponse response = transferService.transfer("user-1", request("key-5"));

        assertThat(response.getId()).isEqualTo("txn-1");
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        // A replayed request must never re-trigger money movement.
        verifyNoInteractions(accountServiceGateway);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_throwsBusinessException_whenTransferringToSameAccount() {
        TransferRequest sameAccountRequest = TransferRequest.builder()
                .fromAccountId("acc-1")
                .toAccountId("acc-1")
                .amount(new BigDecimal("50.00"))
                .currency("INR")
                .idempotencyKey("key-6")
                .build();
        when(transactionRepository.findByIdempotencyKey("key-6")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer("user-1", sameAccountRequest))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(accountServiceGateway);
    }
}
