package com.adria.bank.transaction.service;

import com.adria.bank.transaction.client.AccountClient;
import com.adria.bank.transaction.config.TransactionProperties;
import com.adria.bank.transaction.domain.Transaction;
import com.adria.bank.transaction.dto.*;
import com.adria.bank.transaction.event.TransactionCreatedEvent;
import com.adria.bank.transaction.event.TransactionEventPublisher;
import com.adria.bank.transaction.repository.TransactionRepository;
import com.adria.bank.transaction.service.exception.BusinessException;
import com.adria.bank.transaction.service.exception.TransactionNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final TransactionProperties properties;
    private final TransactionEventPublisher eventPublisher;

    @CircuitBreaker(name = "accountService", fallbackMethod = "createFallback")
    public TransactionResponse create(TransactionRequest request) {
        validateAmount(request.amount());

        AccountDto account = accountClient.getAccount(request.accountId());
        validateAccount(account, request.currency());

        if (account.balance() < request.amount().doubleValue()) {
            throw new BusinessException("Insufficient account balance");
        }

        accountClient.updateBalance(request.accountId(), Map.of(
                "amount", request.amount().doubleValue(),
                "operation", "DEBIT"
        ));

        Transaction saved = transactionRepository.save(Transaction.builder()
                .accountId(request.accountId())
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .merchantCategory(request.merchantCategory())
                .country(request.country())
                .build());

        eventPublisher.publish(TransactionCreatedEvent.of(
                saved.getId(), saved.getAccountId(), saved.getAmount(), saved.getCurrency()
        ));

        return TransactionResponse.from(saved);
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "transferFallback")
    public List<TransactionResponse> transfer(TransferRequest request) {
        validateAmount(request.amount());

        AccountDto from = accountClient.getAccount(request.fromAccountId());
        AccountDto to = accountClient.getAccount(request.toAccountId());

        if (!"ACTIVE".equalsIgnoreCase(from.status()) || !"ACTIVE".equalsIgnoreCase(to.status())) {
            throw new BusinessException("Both accounts must be ACTIVE");
        }
        if (!from.currency().equalsIgnoreCase(request.currency())
                || !to.currency().equalsIgnoreCase(request.currency())) {
            throw new BusinessException("Currency must match both accounts");
        }
        if (from.balance() < request.amount().doubleValue()) {
            throw new BusinessException("Insufficient balance on source account");
        }

        accountClient.transfer(Map.of(
                "fromAccountId", request.fromAccountId().toString(),
                "toAccountId", request.toAccountId().toString(),
                "amount", request.amount().doubleValue()
        ));

        Transaction debit = transactionRepository.save(Transaction.builder()
                .accountId(request.fromAccountId())
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .merchantCategory("TRANSFER_OUT")
                .country(request.country())
                .build());

        Transaction credit = transactionRepository.save(Transaction.builder()
                .accountId(request.toAccountId())
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .merchantCategory("TRANSFER_IN")
                .country(request.country())
                .build());

        eventPublisher.publish(TransactionCreatedEvent.of(
                debit.getId(), debit.getAccountId(), debit.getAmount(), debit.getCurrency()
        ));
        eventPublisher.publish(TransactionCreatedEvent.of(
                credit.getId(), credit.getAccountId(), credit.getAmount(), credit.getCurrency()
        ));

        return List.of(TransactionResponse.from(debit), TransactionResponse.from(credit));
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(UUID id) {
        return TransactionResponse.from(transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id)));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAll() {
        return transactionRepository.findAll().stream().map(TransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getByAccount(UUID accountId) {
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId)
                .stream().map(TransactionResponse::from).toList();
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be strictly positive");
        }
        if (amount.compareTo(properties.getMaxAmount()) > 0) {
            throw new BusinessException("Amount exceeds configured max-amount of " + properties.getMaxAmount());
        }
    }

    private void validateAccount(AccountDto account, String currency) {
        if (account == null) {
            throw new BusinessException("Account does not exist");
        }
        if (!"ACTIVE".equalsIgnoreCase(account.status())) {
            throw new BusinessException("Account must be ACTIVE");
        }
        if (!account.currency().equalsIgnoreCase(currency)) {
            throw new BusinessException("Transaction currency must match account currency");
        }
    }

    @SuppressWarnings("unused")
    private TransactionResponse createFallback(TransactionRequest request, Throwable ex) {
        log.error("createFallback: {}", ex.getMessage());
        throw new BusinessException("Account service unavailable (circuit breaker). " + ex.getMessage());
    }

    @SuppressWarnings("unused")
    private List<TransactionResponse> transferFallback(TransferRequest request, Throwable ex) {
        log.error("transferFallback: {}", ex.getMessage());
        throw new BusinessException("Account service unavailable (circuit breaker). " + ex.getMessage());
    }
}
