package com.adria.bank.transaction.web.rest;

import com.adria.bank.transaction.dto.TransactionRequest;
import com.adria.bank.transaction.dto.TransactionResponse;
import com.adria.bank.transaction.dto.TransferRequest;
import com.adria.bank.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TransactionRestController {

    private final TransactionService transactionService;

    @PostMapping("/api/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody TransactionRequest request) {
        return transactionService.create(request);
    }

    @GetMapping("/api/transactions")
    public List<TransactionResponse> getAll() {
        return transactionService.getAll();
    }

    @GetMapping("/api/transactions/{id}")
    public TransactionResponse getById(@PathVariable UUID id) {
        return transactionService.getById(id);
    }

    @GetMapping("/api/transactions/account/{accountId}")
    public List<TransactionResponse> byAccount(@PathVariable UUID accountId) {
        return transactionService.getByAccount(accountId);
    }

    @PostMapping("/api/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public List<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return transactionService.transfer(request);
    }
}
