package com.adria.bank.account.dto;

import com.adria.bank.account.domain.AccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AccountRequest(
        @NotBlank String customerReference,
        AccountStatus status,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull @PositiveOrZero Double balance
) {}
