package com.adria.bank.assistant.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class McpToolClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${mcp.account-url:http://localhost:8081}")
    private String accountUrl;

    @Value("${mcp.transaction-url:http://localhost:8082}")
    private String transactionUrl;

    public List<Map<String, Object>> listAccountTools() {
        return restTemplate.exchange(
                accountUrl + "/mcp/tools",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
    }

    public List<Map<String, Object>> listTransactionTools() {
        return restTemplate.exchange(
                transactionUrl + "/mcp/tools",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody();
    }

    public Object invokeAccountTool(String toolName, Map<String, Object> args) {
        return restTemplate.postForObject(accountUrl + "/mcp/tools/" + toolName, args, Object.class);
    }

    public Object invokeTransactionTool(String toolName, Map<String, Object> args) {
        return restTemplate.postForObject(transactionUrl + "/mcp/tools/" + toolName, args, Object.class);
    }
}
