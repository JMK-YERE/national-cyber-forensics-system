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

@Service
public class AIChatService {

    private static final Logger log = LoggerFactory.getLogger(AIChatService.class);

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.model:llama-3.1-8b-instant}")
    private String model;

    // Groq API URL — sahihi
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("null");
    }

    public String chat(String userMessage, String context) {
        log.info("========== GROQ AI REQUEST ==========");
        log.info("Model: {}", model);
        log.info("API URL: {}", GROQ_URL);
        log.info("Key prefix: {}", apiKey != null && apiKey.length() > 12 ? apiKey.substring(0, 12) + "..." : "N/A");
        log.info("User message: {}", userMessage);

        if (!isConfigured()) {
            log.error("API KEY HAIPO!");
            return "❌ *AI Haijawekwa*\n\nTafadhali wasiliana na admin kuhusu API key.";
        }

        try {
            String systemPrompt = "Wewe ni AI Assistant wa Cyber Forensics TZ. "
                + "Jibu kwa Kiswahili kwa ufupi (sentensi 2-5). "
                + "Unaweza kusaidia: security, kazi, elimu, maisha, tech. "
                + "Context: " + (context != null ? context : "Hakuna");

            String jsonBody = "{"
                + "\"model\":\"" + model + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + escapeJson(systemPrompt) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + escapeJson(userMessage) + "\"}"
                + "],"
                + "\"temperature\":0.7,"
                + "\"max_tokens\":1024,"
                + "\"stream\":false"
                + "}";

            log.info("Request body: {}", jsonBody.substring(0, Math.min(300, jsonBody.length())));

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(45))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            log.info("Sending request to Groq...");

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Groq HTTP Status: {}", response.statusCode());
            log.info("Groq Response Body: {}", response.body());

            if (response.statusCode() == 200) {
                String text = extractContent(response.body());
                if (text != null && !text.trim().isEmpty()) {
                    log.info("✅ SUCCESS — Response: {} chars", text.length());
                    return text;
                }
                log.error("Response 200 lakini content haipo!");
                return "❌ *Tatizo la Response*\n\nGroq ilijibu lakini content haipo. Jaribu tena.";
            } else if (response.statusCode() == 401) {
                log.error("401 Unauthorized — Key si sahihi");
                return "❌ *API Key si sahihi*\n\nTafadhali update key kwenye Render.";
            } else if (response.statusCode() == 404) {
                log.error("404 — Model au URL si sahihi");
                return "❌ *Model au URL si sahihi*\n\nModel: `" + model + "`. Jaribu `llama-3.1-8b-instant`.";
            } else if (response.statusCode() == 429) {
                log.error("429 — Rate limit");
                return "⏱️ *Rate Limit*\n\nUmezidi kikomo. Subiri dakika 1 kisha jaribu tena.";
            } else {
                log.error("Groq Error {}: {}", response.statusCode(), response.body());
                return "❌ *Hitilafu ya Groq*\n\nStatus: " + response.statusCode() 
                    + "\n\n" + response.body().substring(0, Math.min(200, response.body().length()));
            }

        } catch (Exception e) {
            log.error("Exception: {}", e.getMessage(), e);
            return "❌ *Hitilafu ya Mtandao*\n\n" + e.getMessage()
                + "\n\nJaribu tena baada ya sekunde 30.";
        }
    }

    private String extractContent(String json) {
        try {
            int contentIdx = json.indexOf("\"content\":");
            if (contentIdx == -1) {
                log.error("No 'content' key in JSON");
                return null;
            }

            int start = json.indexOf('"', contentIdx + 10);
            if (start == -1) return null;
            start++;

            StringBuilder sb = new StringBuilder();
            int i = start;
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    switch (next) {
                        case 'n': sb.append('\n'); i += 2; continue;
                        case 't': sb.append('\t'); i += 2; continue;
                        case 'r': sb.append('\r'); i += 2; continue;
                        case '"': sb.append('"'); i += 2; continue;
                        case '\\': sb.append('\\'); i += 2; continue;
                        case '/': sb.append('/'); i += 2; continue;
                        case 'u':
                            if (i + 5 < json.length()) {
                                try {
                                    String hex = json.substring(i + 2, i + 6);
                                    int code = Integer.parseInt(hex, 16);
                                    sb.append((char) code);
                                    i += 6;
                                    continue;
                                } catch (Exception ignored) {}
                            }
                            break;
                    }
                } else if (c == '"') {
                    break;
                }
                sb.append(c);
                i++;
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Parse failed: {}", e.getMessage());
            return null;
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }
}
