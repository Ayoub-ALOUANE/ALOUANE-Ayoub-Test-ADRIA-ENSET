package com.adria.bank.account.web.graphql;

import com.adria.bank.account.domain.AccountStatus;
import com.adria.bank.account.dto.AccountRequest;
import com.adria.bank.account.dto.AccountResponse;
import com.adria.bank.account.dto.BalanceUpdateRequest;
import com.adria.bank.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AccountGraphQlController {

    private final AccountService accountService;

    @QueryMapping
    public AccountResponse account(@Argument UUID id) {
        return accountService.getById(id);
    }

    @QueryMapping
    public List<AccountResponse> accounts() {
        return accountService.getAll();
    }

    @MutationMapping
    public AccountResponse createAccount(@Argument AccountRequest input) {
        return accountService.create(input);
    }

    @MutationMapping
    public AccountResponse updateAccountStatus(@Argument UUID id, @Argument AccountStatus status) {
        return accountService.updateStatus(id, status);
    }

    @MutationMapping
    public AccountResponse updateAccountBalance(@Argument UUID id,
                                                @Argument Double amount,
                                                @Argument String operation) {
        return accountService.updateBalance(id, new BalanceUpdateRequest(amount, operation));
    }

    @MutationMapping
    public AccountResponse transfer(@Argument UUID fromAccountId,
                                    @Argument UUID toAccountId,
                                    @Argument Double amount) {
        return accountService.transfer(fromAccountId, toAccountId, amount);
    }
}
