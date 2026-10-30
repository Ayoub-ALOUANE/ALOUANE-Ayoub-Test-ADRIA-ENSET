package com.adria.bank.account.config;

import com.adria.bank.account.domain.Account;
import com.adria.bank.account.domain.AccountStatus;
import com.adria.bank.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final AccountRepository accountRepository;

    @Override
    public void run(String... args) {
        if (accountRepository.count() == 0) {
            accountRepository.save(Account.builder()
                    .customerReference("CUST-001")
                    .status(AccountStatus.ACTIVE)
                    .currency("EUR")
                    .balance(5000.0)
                    .build());
            accountRepository.save(Account.builder()
                    .customerReference("CUST-002")
                    .status(AccountStatus.ACTIVE)
                    .currency("EUR")
                    .balance(2500.0)
                    .build());
            accountRepository.save(Account.builder()
                    .customerReference("CUST-003")
                    .status(AccountStatus.BLOCKED)
                    .currency("USD")
                    .balance(1000.0)
                    .build());
            log.info("Seeded {} demo accounts", accountRepository.count());
        }
    }
}
