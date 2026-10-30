package com.adria.bank.account.dto;

import jakarta.validation.constraints.NotNull;

public record BalanceUpdateRequest(
        @NotNull Double amount,
        @NotNull String operation
) {}
