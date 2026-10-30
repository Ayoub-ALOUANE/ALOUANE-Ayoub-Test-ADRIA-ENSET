package com.adria.bank.assistant.controller;

import com.adria.bank.assistant.service.BankingAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final BankingAgentService bankingAgentService;

    @PostMapping
    public Map<String, Object> chat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", body.getOrDefault("text", ""));
        return bankingAgentService.chat(message);
    }

    @GetMapping("/tools")
    public Map<String, Object> tools() {
        return bankingAgentService.listTools();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "ai-assistant");
    }
}
