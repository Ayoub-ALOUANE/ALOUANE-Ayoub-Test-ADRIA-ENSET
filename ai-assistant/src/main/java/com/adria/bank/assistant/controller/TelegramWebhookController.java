package com.adria.bank.assistant.controller;

import com.adria.bank.assistant.service.BankingAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Optional Telegram bot webhook / long-poll friendly endpoint.
 * Configure TELEGRAM_BOT_TOKEN to enable outbound replies.
 */
@RestController
@RequestMapping("/api/chat/telegram")
@RequiredArgsConstructor
@Slf4j
public class TelegramWebhookController {

    private final BankingAgentService bankingAgentService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telegram.bot-token:}")
    private String botToken;

    @PostMapping
    public Map<String, Object> webhook(@RequestBody Map<String, Object> update) {
        try {
            Map<?, ?> message = (Map<?, ?>) update.get("message");
            if (message == null) {
                return Map.of("ok", true);
            }
            String text = String.valueOf(message.get("text"));
            Map<?, ?> chat = (Map<?, ?>) message.get("chat");
            Long chatId = Long.valueOf(chat.get("id").toString());

            Map<String, Object> agentReply = bankingAgentService.chat(text);
            String reply = String.valueOf(agentReply.getOrDefault("reply", "OK"));

            if (botToken != null && !botToken.isBlank()) {
                restTemplate.postForEntity(
                        "https://api.telegram.org/bot" + botToken + "/sendMessage",
                        Map.of("chat_id", chatId, "text", reply),
                        String.class
                );
            }
            return Map.of("ok", true, "reply", reply, "agent", agentReply);
        } catch (Exception ex) {
            log.error("Telegram webhook error: {}", ex.getMessage());
            return Map.of("ok", false, "error", ex.getMessage());
        }
    }
}
