package com.adria.bank.account.web.rest;

import com.adria.bank.account.domain.AccountStatus;
import com.adria.bank.account.dto.AccountRequest;
import com.adria.bank.account.dto.AccountResponse;
import com.adria.bank.account.dto.BalanceUpdateRequest;
import com.adria.bank.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountRestController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody AccountRequest request) {
        return accountService.create(request);
    }

    @GetMapping
    public List<AccountResponse> getAll() {
        return accountService.getAll();
    }

    @GetMapping("/{id}")
    public AccountResponse getById(@PathVariable UUID id) {
        return accountService.getById(id);
    }

    @GetMapping("/search")
    public List<AccountResponse> search(@RequestParam String customerReference) {
        return accountService.searchByCustomer(customerReference);
    }

    @PatchMapping("/{id}/status")
    public AccountResponse updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return accountService.updateStatus(id, AccountStatus.valueOf(body.get("status")));
    }

    @PostMapping("/{id}/balance")
    public AccountResponse updateBalance(@PathVariable UUID id,
                                         @Valid @RequestBody BalanceUpdateRequest request) {
        return accountService.updateBalance(id, request);
    }

    @PostMapping("/transfer")
    public AccountResponse transfer(@RequestBody Map<String, Object> body) {
        UUID fromId = UUID.fromString(body.get("fromAccountId").toString());
        UUID toId = UUID.fromString(body.get("toAccountId").toString());
        Double amount = Double.valueOf(body.get("amount").toString());
        return accountService.transfer(fromId, toId, amount);
    }
}
