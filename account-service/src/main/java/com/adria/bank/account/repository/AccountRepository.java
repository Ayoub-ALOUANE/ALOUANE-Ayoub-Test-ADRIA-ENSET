package com.adria.bank.account.repository;

import com.adria.bank.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByCustomerReference(String customerReference);
    List<Account> findByCustomerReferenceContainingIgnoreCase(String customerReference);
}
