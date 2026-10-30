package com.adria.bank.transaction.mcp;

import com.adria.bank.transaction.dto.TransactionRequest;
import com.adria.bank.transaction.dto.TransferRequest;
import com.adria.bank.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MCP tool endpoints — same TransactionService as REST/GraphQL.
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class TransactionMcpController {

    private final TransactionService transactionService;

    @GetMapping("/tools")
    public List<Map<String, Object>> listTools() {
        return List.of(
                Map.of(
                        "name", "list_transactions",
                        "description", "List all transactions",
                        "inputSchema", Map.of("type", "object", "properties", Map.of())
                ),
                Map.of(
                        "name", "get_transactions_by_account",
                        "description", "List transactions for a given account UUID",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of("accountId", Map.of("type", "string")),
                                "required", List.of("accountId")
                        )
                ),
                Map.of(
                        "name", "create_transaction",
                        "description", "Record a new debit transaction (max amount from config)",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "accountId", Map.of("type", "string"),
                                        "amount", Map.of("type", "number"),
                                        "currency", Map.of("type", "string"),
                                        "merchantCategory", Map.of("type", "string"),
                                        "country", Map.of("type", "string")
                                ),
                                "required", List.of("accountId", "amount", "currency")
                        )
                ),
                Map.of(
                        "name", "transfer",
                        "description", "Transfer funds between two accounts",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "fromAccountId", Map.of("type", "string"),
                                        "toAccountId", Map.of("type", "string"),
                                        "amount", Map.of("type", "number"),
                                        "currency", Map.of("type", "string"),
                                        "country", Map.of("type", "string")
                                ),
                                "required", List.of("fromAccountId", "toAccountId", "amount", "currency")
                        )
                )
        );
    }

    @PostMapping("/tools/{toolName}")
    public Object invokeTool(@PathVariable String toolName, @RequestBody(required = false) Map<String, Object> args) {
        if (args == null) {
            args = Map.of();
        }
        return switch (toolName) {
            case "list_transactions" -> transactionService.getAll();
            case "get_transactions_by_account" -> transactionService.getByAccount(
                    UUID.fromString(args.get("accountId").toString())
            );
            case "create_transaction" -> transactionService.create(new TransactionRequest(
                    UUID.fromString(args.get("accountId").toString()),
                    new BigDecimal(args.get("amount").toString()),
                    args.get("currency").toString(),
                    args.getOrDefault("merchantCategory", "OTHER").toString(),
                    args.containsKey("country") ? args.get("country").toString() : null
            ));
            case "transfer" -> transactionService.transfer(new TransferRequest(
                    UUID.fromString(args.get("fromAccountId").toString()),
                    UUID.fromString(args.get("toAccountId").toString()),
                    new BigDecimal(args.get("amount").toString()),
                    args.get("currency").toString(),
                    args.containsKey("country") ? args.get("country").toString() : null
            ));
            default -> Map.of("error", "Unknown tool: " + toolName);
        };
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "transaction-mcp");
    }
}
