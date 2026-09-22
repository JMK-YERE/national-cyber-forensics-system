package com.tz.forensics.controller;

import com.tz.forensics.service.AIChatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        result.put("configuredModel", model);
        result.put("isConfigured", aiChatService.isConfigured());

        try {
            String response = aiChatService.chat("Jibu maneno matatu tu: Password nzuri ni ipi?", "Test");
            result.put("response", response);
            result.put("responseLength", response != null ? response.length() : 0);
            boolean hasError = response != null && response.startsWith("❌");
            result.put("hasError", hasError);
            result.put("apiWorking", !hasError && response != null && response.length() > 20);
        } catch (Exception e) {
            result.put("exception", e.getMessage());
            result.put("apiWorking", false);
        }

        return result;
    }

    // ===== TEST ALL MODELS =====
    @GetMapping("/ai/test-models")
    @ResponseBody
    public Map<String, Object> testModels() {
        Map<String, Object> result = new HashMap<>();
        String[] models = {
            "llama-3.1-8b-instant",
            "llama-3.3-70b-versatile",
            "llama-3.1-70b-versatile",
            "llama3-8b-8192",
            "llama3-70b-8192",
            "mixtral-8x7b-32768",
            "gemma2-9b-it"
        };

        List<Map<String, String>> results = new ArrayList<>();
        for (String m : models) {
            Map<String, String> r = new HashMap<>();
            r.put("model", m);
            try {
                String response = testSingleModel(m);
                r.put("status", response.contains("✅") ? "AVAILABLE" : "ERROR");
                r.put("response", response);
            } catch (Exception e) {
                r.put("status", "ERROR");
                r.put("response", e.getMessage());
            }
            results.add(r);
        }
        result.put("models", results);
        return result;
    }

    private String testSingleModel(String modelName) throws Exception {
        String jsonBody = "{\"model\":\"" + modelName + "\",\"messages\":[{\"role\":\"user\",\"content\":\"Hi\"}],\"max_tokens\":5}";

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return "✅ INATUMIKA";
        } else {
            return "❌ " + response.statusCode() + ": " + response.body().substring(0, Math.min(100, response.body().length()));
        }
    }
}
