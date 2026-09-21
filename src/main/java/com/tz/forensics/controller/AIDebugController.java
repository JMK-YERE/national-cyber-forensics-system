package com.tz.forensics.controller;

import com.tz.forensics.service.AIChatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        result.put("apiKeyPrefix", apiKey != null && apiKey.length() > 12 ? apiKey.substring(0, 12) + "..." : "N/A");
        result.put("model", model);
        result.put("isConfigured", aiChatService.isConfigured());

        // Test with a real question
        try {
            String response = aiChatService.chat("Nipe jibu la maneno matatu tu: nini password nzuri?", "Test");
            result.put("testResponse", response);
            result.put("responseLength", response != null ? response.length() : 0);
            // Check if response is from API or fallback
            boolean isFallback = response != null && response.contains("*AI Assistant*");
            result.put("isFallback", isFallback);
            result.put("apiWorking", !isFallback && response != null && response.length() > 50);
        } catch (Exception e) {
            result.put("testError", e.getMessage());
            result.put("apiWorking", false);
        }

        return result;
    }

    @GetMapping("/ai/test")
    @ResponseBody
    public String test(@RequestParam(defaultValue = "Habari") String message) {
        return aiChatService.chat(message, "Test");
    }
}
