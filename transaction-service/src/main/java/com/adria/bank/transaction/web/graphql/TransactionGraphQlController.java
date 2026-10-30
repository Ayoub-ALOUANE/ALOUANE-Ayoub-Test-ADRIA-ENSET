package com.adria.bank.transaction.web.graphql;

import com.adria.bank.transaction.dto.TransactionRequest;
import com.adria.bank.transaction.dto.TransactionResponse;
import com.adria.bank.transaction.dto.TransferRequest;
import com.adria.bank.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TransactionGraphQlController {

    private final TransactionService transactionService;

    @QueryMapping
    public TransactionResponse transaction(@Argument UUID id) {
        return transactionService.getById(id);
    }

    @QueryMapping
    public List<TransactionResponse> transactions() {
        return transactionService.getAll();
    }

    @QueryMapping
    public List<TransactionResponse> transactionsByAccount(@Argument UUID accountId) {
        return transactionService.getByAccount(accountId);
    }

    @MutationMapping
    public TransactionResponse createTransaction(@Argument UUID accountId,
                                                 @Argument BigDecimal amount,
                                                 @Argument String currency,
                                                 @Argument String merchantCategory,
                                                 @Argument String country) {
        return transactionService.create(new TransactionRequest(
                accountId, amount, currency, merchantCategory, country
        ));
    }

    @MutationMapping
    public List<TransactionResponse> transfer(@Argument UUID fromAccountId,
                                              @Argument UUID toAccountId,
                                              @Argument BigDecimal amount,
                                              @Argument String currency,
                                              @Argument String country) {
        return transactionService.transfer(new TransferRequest(
                fromAccountId, toAccountId, amount, currency, country
        ));
    }
}
