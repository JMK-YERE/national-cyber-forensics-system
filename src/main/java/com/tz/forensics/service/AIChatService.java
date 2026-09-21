package com.tz.forensics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AIChatService {

    private static final Logger log = LoggerFactory.getLogger(AIChatService.class);

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    public boolean isConfigured() {
        boolean configured = apiKey != null && !apiKey.isEmpty() && !apiKey.equals("null");
        log.info("Groq API configured: {} (key length: {})", configured,
                 apiKey != null ? apiKey.length() : 0);
        return configured;
    }

    public String chat(String userMessage, String context) {
        log.info("AI Chat request: '{}'", 
                 userMessage.substring(0, Math.min(50, userMessage.length())));

        if (!isConfigured()) {
            log.warn("Groq API key haipo — using fallback");
            return fallbackResponse(userMessage);
        }

        try {
            String systemPrompt = "Wewe ni AI Assistant wa National Cyber Forensics System - Tanzania. "
                + "Unaweza kusaidia kwa KILA KITU: cybersecurity, digital forensics, mfumo huu, "
                + "maswali ya kawaida, ushauri wa kazi, maisha, elimu, tech, biashara, stori, na zaidi. "
                + "Jibu kwa Kiswahili (au Kiingereza kama mtumiaji anatumia Kiingereza). "
                + "Kuwa rafiki, msaidizi, na wa kitaalamu. "
                + "Kama ni swali la kiufundi, toa maelezo ya hatua kwa hatua yenye namba. "
                + "Kama ni swali la kawaida, jibu kwa ufupi. "
                + "Kama ni stori, jibu kwa ubunifu. "
                + "Tumia emoji kwa mpangilio mzuri. "
                + "Context: " + (context != null ? context : "Hakuna");

            String jsonBody = "{"
                + "\"model\":\"" + model + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + escapeJson(systemPrompt) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + escapeJson(userMessage) + "\"}"
                + "],"
                + "\"temperature\":0.7,"
                + "\"max_tokens\":1500,"
                + "\"top_p\":0.9"
                + "}";

            log.debug("Calling Groq API: {}", apiUrl);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(45))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Groq API response status: {}", response.statusCode());

            if (response.statusCode() == 200) {
                String text = extractText(response.body());
                if (text != null && !text.isEmpty()) {
                    log.info("AI response received ({} chars)", text.length());
                    return text;
                }
            } else {
                log.error("Groq API error: {} - Body: {}", 
                          response.statusCode(),
                          response.body().substring(0, Math.min(500, response.body().length())));
            }

            return fallbackResponse(userMessage);

        } catch (Exception e) {
            log.error("AI Chat exception: {}", e.getMessage(), e);
            return fallbackResponse(userMessage);
        }
    }

    private String extractText(String jsonResponse) {
        try {
            // First try: content field
            Pattern pattern = Pattern.compile("\"content\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
            Matcher matcher = pattern.matcher(jsonResponse);
            if (matcher.find()) {
                String text = matcher.group(1);
                return unescape(text);
            }
        } catch (Exception e) {
            log.error("Parse failed: {}", e.getMessage());
        }
        return null;
    }

    private String unescape(String s) {
        return s.replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\r", "")
                .replace("\\t", "    ");
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ===== FALLBACK =====
    private String fallbackResponse(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("password")) {
            return "🔐 *Password Nzuri:*\n\n✅ Herufi 12+\n✅ Kubwa na ndogo (A-Z, a-z)\n✅ Namba (0-9)\n✅ Alama (!@#$%)\n❌ Usitumie 'password123'\n\n*Mfano:* `Tz#Cyber2024!`";
        }
        if (lower.contains("halo") || lower.contains("habari") || lower.contains("hello") || lower.contains("hi")) {
            return "👋 *Habari!*\n\nMimi ni AI Assistant wa Cyber Forensics TZ.\n\nNinaweza kukusaidia kwa:\n\n🔐 Security\n💼 Kazi\n📚 Elimu\n💡 Maisha\n\n*Uliza swali lolote!*";
        }
        return "🤖 *AI Assistant*\n\nKwa sasa API ina tatizo, lakini naweza kujibu:\n\n🔐 Security\n💼 Kazi\n📚 Elimu\n\n*Uliza swali lolote!*";
    }
}
