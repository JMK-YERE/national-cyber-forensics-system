package com.tz.forensics.controller;

import com.tz.forensics.service.AIChatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class AIDebugController {

    private final AIChatService aiChatService;

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.model:}")
    private String model;

    public AIDebugController(AIChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @GetMapping("/ai/debug")
    @ResponseBody
    public Map<String, Object> debug() {
        Map<String, Object> result = new HashMap<>();
        result.put("apiKeyExists", apiKey != null && !apiKey.isEmpty());
        result.put("apiKeyLength", apiKey != null ? apiKey.length() : 0);
        result.put("apiKeyPrefix", apiKey != null && apiKey.length() > 10 ? apiKey.substring(0, 10) + "..." : "N/A");
        result.put("model", model);
        result.put("isConfigured", aiChatService.isConfigured());

        // Test API
        try {
            String response = aiChatService.chat("Habari, jibu 'Safi' tu", "Test");
            result.put("testResponse", response);
            result.put("apiWorking", !response.contains("tatizo") && !response.contains("API key haipo"));
        } catch (Exception e) {
            result.put("testError", e.getMessage());
            result.put("apiWorking", false);
        }

        return result;
    }

    @GetMapping("/ai/test")
    @ResponseBody
    public String test(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "Habari") String message) {
        return aiChatService.chat(message, "Test");
    }
}
