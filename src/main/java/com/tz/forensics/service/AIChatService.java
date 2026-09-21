package com.tz.forensics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class AIChatService {

    private static final Logger log = LoggerFactory.getLogger(AIChatService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    public boolean isConfigured() {
        boolean configured = apiKey != null && !apiKey.isEmpty() && !apiKey.equals("null");
        log.info("Groq configured: {} (key length: {})", configured, apiKey != null ? apiKey.length() : 0);
        return configured;
    }

    public String chat(String userMessage, String context) {
        log.info("AI Chat request received");

        if (!isConfigured()) {
            log.warn("API key haipo — fallback");
            return fallbackResponse(userMessage);
        }

        try {
            // Build request using Jackson (safe JSON escaping)
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                    Map.of("role", "system", "content", buildSystemPrompt(context)),
                    Map.of("role", "user", "content", userMessage)
                ),
                "temperature", 0.7,
                "max_tokens", 1500,
                "top_p", 0.9,
                "stream", false
            );

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            log.debug("Request body: {}", jsonBody.substring(0, Math.min(200, jsonBody.length())));

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(45))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Groq response status: {}", response.statusCode());

            if (response.statusCode() == 200) {
                String text = parseGroqResponse(response.body());
                if (text != null && !text.isEmpty()) {
                    log.info("AI response: {} chars", text.length());
                    return text;
                } else {
                    log.warn("Empty response from Groq");
                }
            } else {
                log.error("Groq error {}: {}", response.statusCode(),
                          response.body().substring(0, Math.min(500, response.body().length())));
            }

            return fallbackResponse(userMessage);

        } catch (Exception e) {
            log.error("AI Chat exception: {}", e.getMessage(), e);
            return fallbackResponse(userMessage);
        }
    }

    // ===== PARSE WITH JACKSON (SAFE) =====
    private String parseGroqResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message");
                JsonNode content = message.path("content");
                if (!content.isMissingNode() && content.isTextual()) {
                    return content.asText();
                }
            }
            log.warn("Could not find content in response");
            return null;
        } catch (Exception e) {
            log.error("JSON parse failed: {}", e.getMessage());
            return null;
        }
    }

    private String buildSystemPrompt(String context) {
        return "Wewe ni AI Assistant wa National Cyber Forensics System - Tanzania. "
            + "Unaweza kusaidia kwa KILA KITU: cybersecurity, digital forensics, mfumo huu, "
            + "maswali ya kawaida, ushauri wa kazi, maisha, elimu, tech, biashara, stori. "
            + "Jibu kwa Kiswahili (au Kiingereza kama mtumiaji anatumia Kiingereza). "
            + "Kuwa rafiki na msaidizi. "
            + "Kama ni swali la kiufundi, toa maelezo ya hatua kwa hatua. "
            + "Tumia emoji kwa mpangilio mzuri. "
            + "Context: " + (context != null ? context : "Hakuna");
    }

    private String fallbackResponse(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("password")) {
            return "🔐 *Password Nzuri:*\n\n✅ Herufi 12+\n✅ Kubwa na ndogo (A-Z, a-z)\n✅ Namba (0-9)\n✅ Alama (!@#$%)\n\n*Mfano:* `Tz#Cyber2024!`";
        }
        if (lower.contains("halo") || lower.contains("habari") || lower.contains("hello") || lower.contains("hi")) {
            return "👋 *Habari!*\n\nMimi ni AI Assistant. Ninaweza kukusaidia kwa:\n\n🔐 Security\n💼 Kazi\n📚 Elimu\n💡 Maisha\n\n*Uliza swali lolote!*";
        }
        return "🤖 *AI Assistant*\n\nUliza swali lolote — nitajaribu kukusaidia!";
    }
}
