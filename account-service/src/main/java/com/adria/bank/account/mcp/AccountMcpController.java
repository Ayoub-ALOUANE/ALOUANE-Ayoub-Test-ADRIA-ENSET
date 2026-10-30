package com.adria.bank.account.mcp;

import com.adria.bank.account.domain.AccountStatus;
import com.adria.bank.account.dto.AccountRequest;
import com.adria.bank.account.dto.AccountResponse;
import com.adria.bank.account.dto.BalanceUpdateRequest;
import com.adria.bank.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MCP-compatible tool endpoints exposing the same AccountService used by REST/GraphQL.
 * The AI assistant discovers and invokes these tools to automate banking operations.
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class AccountMcpController {

    private final AccountService accountService;

    @GetMapping("/tools")
    public List<Map<String, Object>> listTools() {
        return List.of(
                Map.of(
                        "name", "list_accounts",
                        "description", "List all bank accounts",
                        "inputSchema", Map.of("type", "object", "properties", Map.of())
                ),
                Map.of(
                        "name", "get_account",
                        "description", "Get account details by UUID",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of("id", Map.of("type", "string")),
                                "required", List.of("id")
                        )
                ),
                Map.of(
                        "name", "create_account",
                        "description", "Create a new bank account",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "customerReference", Map.of("type", "string"),
                                        "currency", Map.of("type", "string"),
                                        "balance", Map.of("type", "number"),
                                        "status", Map.of("type", "string")
                                ),
                                "required", List.of("customerReference", "currency", "balance")
                        )
                ),
                Map.of(
                        "name", "update_balance",
                        "description", "Debit or credit an account balance",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "id", Map.of("type", "string"),
                                        "amount", Map.of("type", "number"),
                                        "operation", Map.of("type", "string", "enum", List.of("DEBIT", "CREDIT"))
                                ),
                                "required", List.of("id", "amount", "operation")
                        )
                ),
                Map.of(
                        "name", "transfer_funds",
                        "description", "Transfer money between two accounts",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "fromAccountId", Map.of("type", "string"),
                                        "toAccountId", Map.of("type", "string"),
                                        "amount", Map.of("type", "number")
                                ),
                                "required", List.of("fromAccountId", "toAccountId", "amount")
                        )
                )
        );
    }

    @PostMapping("/tools/{toolName}")
    public Object invokeTool(@PathVariable String toolName, @RequestBody Map<String, Object> args) {
        return switch (toolName) {
            case "list_accounts" -> accountService.getAll();
            case "get_account" -> accountService.getById(UUID.fromString(args.get("id").toString()));
            case "create_account" -> {
                AccountStatus status = args.containsKey("status")
                        ? AccountStatus.valueOf(args.get("status").toString())
                        : AccountStatus.ACTIVE;
                yield accountService.create(new AccountRequest(
                        args.get("customerReference").toString(),
                        status,
                        args.get("currency").toString(),
                        Double.valueOf(args.get("balance").toString())
                ));
            }
            case "update_balance" -> accountService.updateBalance(
                    UUID.fromString(args.get("id").toString()),
                    new BalanceUpdateRequest(
                            Double.valueOf(args.get("amount").toString()),
                            args.get("operation").toString()
                    )
            );
            case "transfer_funds" -> accountService.transfer(
                    UUID.fromString(args.get("fromAccountId").toString()),
                    UUID.fromString(args.get("toAccountId").toString()),
                    Double.valueOf(args.get("amount").toString())
            );
            default -> Map.of("error", "Unknown tool: " + toolName);
        };
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "account-mcp");
    }
}
