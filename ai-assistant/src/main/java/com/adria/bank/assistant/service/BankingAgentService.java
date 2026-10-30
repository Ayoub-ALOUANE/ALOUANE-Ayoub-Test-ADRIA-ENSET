package com.adria.bank.assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agentic banking assistant.
 * - Discovers MCP tools from account-service and transaction-service
 * - Uses OpenAI-compatible LLM when OPENAI_API_KEY is set
 * - Falls back to deterministic intent routing otherwise (demo / offline mode)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankingAgentService {

    private final McpToolClient mcpToolClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${openai.api-key:}")
    private String openAiApiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    public Map<String, Object> chat(String message) {
        List<Map<String, Object>> tools = new ArrayList<>();
        try {
            Optional.ofNullable(mcpToolClient.listAccountTools()).ifPresent(tools::addAll);
            Optional.ofNullable(mcpToolClient.listTransactionTools()).ifPresent(tools::addAll);
        } catch (Exception ex) {
            log.warn("Could not list MCP tools: {}", ex.getMessage());
        }

        if (openAiApiKey != null && !openAiApiKey.isBlank()) {
            return chatWithLlm(message, tools);
        }
        return chatWithRules(message);
    }

    public Map<String, Object> listTools() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accountTools", mcpToolClient.listAccountTools());
        result.put("transactionTools", mcpToolClient.listTransactionTools());
        return result;
    }

    private Map<String, Object> chatWithLlm(String message, List<Map<String, Object>> tools) {
        try {
            String systemPrompt = """
                    You are a banking operations assistant. You can call MCP tools to list accounts,
                    create accounts, transfer funds, and create transactions.
                    Available tools JSON:
                    """ + objectMapper.writeValueAsString(tools) + """

                    Reply ONLY with JSON of the form:
                    {"tool":"<name or null>","server":"account|transaction|null","args":{},"reply":"<natural language>"}
                    """;

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", message)
                    ),
                    "temperature", 0.1
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            ResponseEntity<String> response = restTemplate.exchange(
                    openAiBaseUrl + "/chat/completions",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").path(0).path("message").path("content").asText();
            JsonNode plan = objectMapper.readTree(extractJson(content));

            Object toolResult = null;
            String tool = plan.path("tool").asText(null);
            String server = plan.path("server").asText(null);
            Map<String, Object> args = objectMapper.convertValue(
                    plan.path("args"),
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
            );

            if (tool != null && !tool.equals("null") && !tool.isBlank()) {
                toolResult = "account".equalsIgnoreCase(server)
                        ? mcpToolClient.invokeAccountTool(tool, args == null ? Map.of() : args)
                        : mcpToolClient.invokeTransactionTool(tool, args == null ? Map.of() : args);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("mode", "llm");
            result.put("reply", plan.path("reply").asText(content));
            result.put("tool", tool);
            result.put("toolResult", toolResult);
            return result;
        } catch (Exception ex) {
            log.error("LLM chat failed, falling back to rules: {}", ex.getMessage());
            Map<String, Object> fallback = chatWithRules(message);
            fallback.put("llmError", ex.getMessage());
            return fallback;
        }
    }

    private Map<String, Object> chatWithRules(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", "rules");

        try {
            if (lower.contains("list account") || lower.contains("show account")
                    || lower.contains("comptes") || lower.contains("list accounts")) {
                Object data = mcpToolClient.invokeAccountTool("list_accounts", Map.of());
                result.put("tool", "list_accounts");
                result.put("toolResult", data);
                result.put("reply", "Here are the bank accounts retrieved via MCP.");
                return result;
            }
            if (lower.contains("list transaction") || lower.contains("transactions")
                    || lower.contains("historique")) {
                Object data = mcpToolClient.invokeTransactionTool("list_transactions", Map.of());
                result.put("tool", "list_transactions");
                result.put("toolResult", data);
                result.put("reply", "Here are the transactions retrieved via MCP.");
                return result;
            }
            if (lower.contains("transfer") || lower.contains("virement")) {
                Matcher m = Pattern.compile(
                        "([0-9a-fA-F-]{36}).*?([0-9a-fA-F-]{36}).*?(\\d+(?:\\.\\d+)?)"
                ).matcher(message);
                if (m.find()) {
                    Map<String, Object> args = Map.of(
                            "fromAccountId", m.group(1),
                            "toAccountId", m.group(2),
                            "amount", Double.parseDouble(m.group(3)),
                            "currency", "EUR"
                    );
                    Object data = mcpToolClient.invokeTransactionTool("transfer", args);
                    result.put("tool", "transfer");
                    result.put("args", args);
                    result.put("toolResult", data);
                    result.put("reply", "Transfer executed via MCP transaction tools.");
                    return result;
                }
                result.put("reply", "To transfer, provide: transfer <fromUUID> <toUUID> <amount>");
                return result;
            }
            if (lower.contains("create transaction") || lower.contains("payer")
                    || lower.contains("payment")) {
                Matcher m = Pattern.compile(
                        "([0-9a-fA-F-]{36}).*?(\\d+(?:\\.\\d+)?)"
                ).matcher(message);
                if (m.find()) {
                    Map<String, Object> args = new LinkedHashMap<>();
                    args.put("accountId", m.group(1));
                    args.put("amount", Double.parseDouble(m.group(2)));
                    args.put("currency", "EUR");
                    args.put("merchantCategory", "ECOMMERCE");
                    args.put("country", "FR");
                    Object data = mcpToolClient.invokeTransactionTool("create_transaction", args);
                    result.put("tool", "create_transaction");
                    result.put("args", args);
                    result.put("toolResult", data);
                    result.put("reply", "Transaction created via MCP.");
                    return result;
                }
            }
            if (lower.contains("help") || lower.contains("aide")) {
                result.put("reply", """
                        Banking agent (MCP tools). Examples:
                        - list accounts
                        - list transactions
                        - transfer <fromUUID> <toUUID> <amount>
                        - create transaction <accountUUID> <amount>
                        Set OPENAI_API_KEY for full LLM agent mode.
                        """);
                result.put("tools", listTools());
                return result;
            }
            result.put("reply", "I did not understand. Type 'help' for available commands. (Rule-based mode — set OPENAI_API_KEY for LLM)");
            result.put("tools", listTools());
            return result;
        } catch (Exception ex) {
            result.put("reply", "Error while calling MCP tools: " + ex.getMessage());
            result.put("error", ex.getMessage());
            return result;
        }
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return "{\"tool\":null,\"server\":null,\"args\":{},\"reply\":" + objectMapper.valueToTree(content) + "}";
    }
}
