package com.adria.bank.transaction.dto;

import com.adria.bank.transaction.domain.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID accountId,
        BigDecimal amount,
        String currency,
        String merchantCategory,
        String country,
        Instant createdAt
) {
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getAccountId(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getMerchantCategory(),
                tx.getCountry(),
                tx.getCreatedAt()
        );
    }
}
