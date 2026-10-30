package com.adria.bank.transaction.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionCreatedEvent(
        UUID eventId,
        String eventType,
        UUID transactionId,
        UUID accountId,
        BigDecimal amount,
        String currency,
        Instant occurredAt
) {
    public static TransactionCreatedEvent of(UUID transactionId, UUID accountId,
                                             BigDecimal amount, String currency) {
        return new TransactionCreatedEvent(
                UUID.randomUUID(),
                "TransactionCreated",
                transactionId,
                accountId,
                amount,
                currency,
                Instant.now()
        );
    }
}
