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

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    // Groq API URL — sahihi
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("null");
    }

    public String chat(String userMessage, String context) {
        log.info("===== GROQ REQUEST =====");
        log.info("Model: {}", model);
        log.info("URL: {}", GROQ_URL);
        log.info("Key length: {}", apiKey != null ? apiKey.length() : 0);
        log.info("Message: {}", userMessage);

        if (!isConfigured()) {
            return "❌ AI Haijawekwa. Wasiliana na admin.";
        }

        try {
            String systemPrompt = "Wewe ni AI Assistant wa Cyber Forensics. "
                + "Jibu kwa Kiswahili kwa ufupi (sentensi 2-5). "
                + "Unaweza kusaidia: security, kazi, elimu, maisha, tech. "
                + "Tumia emoji. Context: " + (context != null ? context : "Hakuna");

            // Manual JSON build — safe escaping
            String jsonBody = "{"
                + "\"model\":\"" + model + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + escapeJson(systemPrompt) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + escapeJson(userMessage) + "\"}"
                + "],"
                + "\"temperature\":0.7,"
                + "\"max_tokens\":1024"
                + "}";

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

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("HTTP Status: {}", response.statusCode());
            log.info("Response: {}", response.body().substring(0, Math.min(500, response.body().length())));

            if (response.statusCode() == 200) {
                String text = extractContent(response.body());
                if (text != null && !text.trim().isEmpty()) {
                    log.info("✅ SUCCESS: {} chars", text.length());
                    return text;
                }
                log.error("Empty content from response");
                return "❌ Response ilikuwa tupu. Jaribu tena.";
            } else if (response.statusCode() == 401) {
                return "❌ API Key si sahihi. Update kwenye Render.";
            } else if (response.statusCode() == 404) {
                return "❌ Model haipo: `" + model + "`. Badilisha kwenye Render.";
            } else if (response.statusCode() == 429) {
                return "⏱️ Rate limit. Subiri dakika 1.";
            } else {
                return "❌ Hitilafu ya Groq: " + response.statusCode();
            }

        } catch (Exception e) {
            log.error("Exception: {}", e.getMessage(), e);
            return "❌ Hitilafu ya mtandao: " + e.getMessage();
        }
    }

    private String extractContent(String json) {
        try {
            int contentIdx = json.indexOf("\"content\":");
            if (contentIdx == -1) return null;

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
