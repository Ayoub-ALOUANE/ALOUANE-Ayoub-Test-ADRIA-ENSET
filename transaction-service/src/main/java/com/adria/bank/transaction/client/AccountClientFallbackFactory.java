package com.adria.bank.transaction.client;

import com.adria.bank.transaction.dto.AccountDto;
import com.adria.bank.transaction.service.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class AccountClientFallbackFactory implements FallbackFactory<AccountClient> {

    @Override
    public AccountClient create(Throwable cause) {
        return new AccountClient() {
            @Override
            public AccountDto getAccount(UUID id) {
                log.error("Circuit open / account-service unavailable for getAccount {}: {}", id, cause.getMessage());
                throw new BusinessException("Account service unavailable (Resilience4J fallback). Retry later.");
            }

            @Override
            public AccountDto updateBalance(UUID id, Map<String, Object> body) {
                log.error("Circuit open / account-service unavailable for updateBalance {}: {}", id, cause.getMessage());
                throw new BusinessException("Account service unavailable (Resilience4J fallback). Retry later.");
            }

            @Override
            public AccountDto transfer(Map<String, Object> body) {
                log.error("Circuit open / account-service unavailable for transfer: {}", cause.getMessage());
                throw new BusinessException("Account service unavailable (Resilience4J fallback). Retry later.");
            }
        };
    }
}
