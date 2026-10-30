package com.adria.bank.transaction.service;

import com.adria.bank.transaction.client.AccountClient;
import com.adria.bank.transaction.config.TransactionProperties;
import com.adria.bank.transaction.dto.AccountDto;
import com.adria.bank.transaction.dto.TransactionRequest;
import com.adria.bank.transaction.event.TransactionEventPublisher;
import com.adria.bank.transaction.repository.TransactionRepository;
import com.adria.bank.transaction.service.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock AccountClient accountClient;
    @Mock TransactionProperties properties;
    @Mock TransactionEventPublisher eventPublisher;

    @InjectMocks TransactionService transactionService;

    UUID accountId;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        when(properties.getMaxAmount()).thenReturn(new BigDecimal("1000"));
    }

    @Test
    void create_rejectsAmountAboveMax() {
        assertThrows(BusinessException.class, () ->
                transactionService.create(new TransactionRequest(
                        accountId, new BigDecimal("1500"), "EUR", "SHOP", "FR"
                ))
        );
    }

    @Test
    void create_rejectsBlockedAccount() {
        when(accountClient.getAccount(accountId)).thenReturn(
                new AccountDto(accountId, "C1", "BLOCKED", "EUR", 5000.0)
        );
        assertThrows(BusinessException.class, () ->
                transactionService.create(new TransactionRequest(
                        accountId, new BigDecimal("10"), "EUR", "SHOP", "FR"
                ))
        );
    }

    @Test
    void create_rejectsCurrencyMismatch() {
        when(accountClient.getAccount(accountId)).thenReturn(
                new AccountDto(accountId, "C1", "ACTIVE", "USD", 5000.0)
        );
        assertThrows(BusinessException.class, () ->
                transactionService.create(new TransactionRequest(
                        accountId, new BigDecimal("10"), "EUR", "SHOP", "FR"
                ))
        );
    }
}
