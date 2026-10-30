package com.adria.bank.account.service;

import com.adria.bank.account.domain.Account;
import com.adria.bank.account.domain.AccountStatus;
import com.adria.bank.account.dto.AccountRequest;
import com.adria.bank.account.dto.AccountResponse;
import com.adria.bank.account.dto.BalanceUpdateRequest;
import com.adria.bank.account.repository.AccountRepository;
import com.adria.bank.account.service.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    AccountRepository accountRepository;

    @InjectMocks
    AccountService accountService;

    Account account;

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .id(UUID.randomUUID())
                .customerReference("CUST-T")
                .status(AccountStatus.ACTIVE)
                .currency("EUR")
                .balance(1000.0)
                .build();
    }

    @Test
    void createAccount_ok() {
        when(accountRepository.save(any())).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        AccountResponse res = accountService.create(
                new AccountRequest("CUST-1", AccountStatus.ACTIVE, "eur", 100.0)
        );
        assertEquals("EUR", res.currency());
        assertEquals(100.0, res.balance());
    }

    @Test
    void debit_insufficientBalance_throws() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        assertThrows(BusinessException.class, () ->
                accountService.updateBalance(account.getId(), new BalanceUpdateRequest(5000.0, "DEBIT"))
        );
    }

    @Test
    void transfer_ok() {
        Account to = Account.builder()
                .id(UUID.randomUUID())
                .customerReference("CUST-2")
                .status(AccountStatus.ACTIVE)
                .currency("EUR")
                .balance(100.0)
                .build();
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.findById(to.getId())).thenReturn(Optional.of(to));

        accountService.transfer(account.getId(), to.getId(), 200.0);
        assertEquals(800.0, account.getBalance());
        assertEquals(300.0, to.getBalance());
    }
}
