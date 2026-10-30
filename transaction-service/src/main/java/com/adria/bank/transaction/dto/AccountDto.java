package com.adria.bank.transaction.dto;

import java.util.UUID;

public record AccountDto(
        UUID id,
        String customerReference,
        String status,
        String currency,
        Double balance
) {}
