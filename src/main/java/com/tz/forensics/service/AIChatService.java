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

    // ===== BUILD SYSTEM PROMPT — By Language =====
    private String buildSystemPrompt(String lang, String context) {
        String basePrompt;

        switch (lang != null ? lang.toLowerCase() : "en") {
            case "sw":
                basePrompt = "Wewe ni AI Assistant wa Cyber Forensics System. "
                    + "LAZIMA ujibu kwa KISWAHILI pekee — hata kama mtumiaji anatumia lugha nyingine. "
                    + "Jibu kwa ufupi (sentensi 2-5) na kwa heshima. "
                    + "Unaweza kusaidia: cybersecurity, digital forensics, kazi, elimu, maisha, tech, biashara. "
                    + "Tumia emoji kwa mpangilio mzuri. "
                    + "Kama mtumiaji ameuliza Kiingereza, BADO jibu kwa KISWAHILI. ";
                break;

            case "fr":
                basePrompt = "Vous êtes l'assistant IA du système Cyber Forensics. "
                    + "VOUS DEVEZ répondre UNIQUEMENT en FRANÇAIS. "
                    + "Répondez brièvement (2-5 phrases) avec respect. "
                    + "Vous pouvez aider avec: cybersécurité, forensique numérique, travail, éducation, tech. "
                    + "Utilisez des emojis. ";
                break;

            case "ar":
                basePrompt = "أنت مساعد الذكاء الاصطناعي لنظام Cyber Forensics. "
                    + "يجب أن ترد باللغة العربية فقط. "
                    + "أجب بإيجاز (2-5 جمل) باحترام. "
                    + "يمكنك المساعدة في: الأمن السيبراني، الطب الشرعي الرقمي، العمل، التعليم، التكنولوجيا. "
                    + "استخدم الرموز التعبيرية. ";
                break;

            case "en":
            default:
                basePrompt = "You are the AI Assistant for Cyber Forensics System. "
                    + "You MUST respond in ENGLISH ONLY — even if the user writes in another language. "
                    + "Be brief (2-5 sentences), helpful, and professional. "
                    + "You can help with: cybersecurity, digital forensics, work, education, life, tech, business. "
                    + "Use emojis appropriately. "
                    + "If the user asks in another language, STILL respond in ENGLISH. ";
                break;
        }

        basePrompt += "Context: " + (context != null ? context : "None");
        return basePrompt;
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
