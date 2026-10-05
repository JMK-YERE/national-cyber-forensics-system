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
import java.util.Locale;

@Service
public class AIChatService {

    private static final Logger log = LoggerFactory.getLogger(AIChatService.class);

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.model:openai/gpt-oss-120b}")
    private String model;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("null");
    }

    public String chat(String userMessage, String context) {
        return chat(userMessage, context, "en");
    }

    public String chat(String userMessage, String context, String lang) {
        log.info("=== AI REQUEST (lang: {}) ===", lang);

        if (!isConfigured()) {
            return getErrorResponse(lang, "AI not configured");
        }

        try {
            String systemPrompt = buildSystemPrompt(lang, context);

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

            log.info("Groq status: {}", response.statusCode());

            if (response.statusCode() == 200) {
                String text = extractContent(response.body());
                if (text != null && !text.trim().isEmpty()) {
                    return text;
                }
            }

            return getErrorResponse(lang, "Status: " + response.statusCode());

        } catch (Exception e) {
            log.error("AI failed: {}", e.getMessage());
            return getErrorResponse(lang, e.getMessage());
        }
    }

    // ===== BUILD SYSTEM PROMPT — Respond in the user's own language =====
    private String buildSystemPrompt(String lang, String context) {
        return "You are the AI Assistant for Cyber Forensics TZ, a professional cybersecurity and digital-forensics platform. "
            + "Detect the language of the user's latest message and ALWAYS answer in that same language. "
            + "If the user writes in Kiswahili, answer in Kiswahili. If the user writes in English, answer in English. "
            + "If the user writes in French, Arabic, Portuguese, Spanish, or another language, answer in that language when you can. "
            + "Do not translate the user's question into another language unless they explicitly ask for translation. "
            + "If the message mixes languages, use the dominant language of the question. "
            + "Be professional, clear, concise, and helpful. "
            + "For cybersecurity and digital-forensics questions, prioritize safe, lawful defensive guidance and explain risks clearly. "
            + "You can help with cybersecurity, digital forensics, incident response, evidence handling, account security, education, technology, and general productivity. "
            + "Use structured bullets when useful and avoid unnecessary emojis. "
            + "User context: " + (context != null ? context : "None");
    }

    private String getErrorResponse(String lang, String error) {
        switch (lang != null ? lang.toLowerCase() : "en") {
            case "sw": return "❌ AI haiwezi kujibu kwa sasa. Hitilafu: " + error;
            case "fr": return "❌ L'IA ne peut pas répondre. Erreur: " + error;
            case "ar": return "❌ لا يمكن للذكاء الاصطناعي الرد. خطأ: " + error;
            default: return "❌ AI cannot respond. Error: " + error;
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
