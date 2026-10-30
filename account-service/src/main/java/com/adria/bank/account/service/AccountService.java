package com.adria.bank.account.service;

import com.adria.bank.account.domain.Account;
import com.adria.bank.account.domain.AccountStatus;
import com.adria.bank.account.dto.AccountRequest;
import com.adria.bank.account.dto.AccountResponse;
import com.adria.bank.account.dto.BalanceUpdateRequest;
import com.adria.bank.account.repository.AccountRepository;
import com.adria.bank.account.service.exception.AccountNotFoundException;
import com.adria.bank.account.service.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountResponse create(AccountRequest request) {
        Account account = Account.builder()
                .customerReference(request.customerReference())
                .status(request.status() != null ? request.status() : AccountStatus.ACTIVE)
                .currency(request.currency().toUpperCase())
                .balance(request.balance())
                .build();
        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public AccountResponse getById(UUID id) {
        return AccountResponse.from(findEntity(id));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAll() {
        return accountRepository.findAll().stream().map(AccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> searchByCustomer(String customerReference) {
        return accountRepository.findByCustomerReferenceContainingIgnoreCase(customerReference)
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    public AccountResponse updateStatus(UUID id, AccountStatus status) {
        Account account = findEntity(id);
        account.setStatus(status);
        return AccountResponse.from(account);
    }

    public AccountResponse updateBalance(UUID id, BalanceUpdateRequest request) {
        Account account = findEntity(id);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Account is not ACTIVE");
        }
        double delta = request.amount();
        String op = request.operation().toUpperCase();
        return switch (op) {
            case "DEBIT" -> {
                if (account.getBalance() < delta) {
                    throw new BusinessException("Insufficient balance");
                }
                account.setBalance(account.getBalance() - delta);
                yield AccountResponse.from(account);
            }
            case "CREDIT" -> {
                account.setBalance(account.getBalance() + delta);
                yield AccountResponse.from(account);
            }
            default -> throw new BusinessException("Unsupported operation: " + request.operation());
        };
    }

    public AccountResponse transfer(UUID fromId, UUID toId, Double amount) {
        if (amount == null || amount <= 0) {
            throw new BusinessException("Transfer amount must be strictly positive");
        }
        if (fromId.equals(toId)) {
            throw new BusinessException("Cannot transfer to the same account");
        }
        Account from = findEntity(fromId);
        Account to = findEntity(toId);
        if (from.getStatus() != AccountStatus.ACTIVE || to.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Both accounts must be ACTIVE");
        }
        if (!from.getCurrency().equalsIgnoreCase(to.getCurrency())) {
            throw new BusinessException("Currency mismatch between accounts");
        }
        if (from.getBalance() < amount) {
            throw new BusinessException("Insufficient balance on source account");
        }
        from.setBalance(from.getBalance() - amount);
        to.setBalance(to.getBalance() + amount);
        return AccountResponse.from(from);
    }

    private Account findEntity(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}
