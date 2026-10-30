package com.adria.bank.transaction.client;

import com.adria.bank.transaction.dto.AccountDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@FeignClient(
        name = "account-service",
        url = "${account.service.url:}",
        fallbackFactory = AccountClientFallbackFactory.class
)
public interface AccountClient {

    @GetMapping("/api/accounts/{id}")
    AccountDto getAccount(@PathVariable("id") UUID id);

    @PostMapping("/api/accounts/{id}/balance")
    AccountDto updateBalance(@PathVariable("id") UUID id, @RequestBody Map<String, Object> body);

    @PostMapping("/api/accounts/transfer")
    AccountDto transfer(@RequestBody Map<String, Object> body);
}
