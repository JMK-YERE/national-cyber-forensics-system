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
import java.util.List;
import java.util.Map;

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
        return chat(userMessage, context, lang, java.util.Collections.emptyList());
    }

    public String chat(String userMessage, String context, String lang, List<Map<String, String>> history) {
        log.info("AI request received (lang: {})", lang);

        if (!isConfigured()) {
            return getErrorResponse(lang, "AI not configured");
        }

        try {
            String systemPrompt = buildSystemPrompt(lang, context);

            StringBuilder body = new StringBuilder("{");
            body.append("\"model\":\"").append(escapeJson(model)).append("\",");
            body.append("\"messages\":[{\"role\":\"system\",\"content\":\"")
                    .append(escapeJson(systemPrompt)).append("\"}");
            if (history != null) {
                int start = Math.max(0, history.size() - 12);
                for (int i = start; i < history.size(); i++) {
                    Map<String, String> item = history.get(i);
                    if (item == null) continue;
                    String type = item.get("type");
                    String role = "user".equals(type) ? "user"
                            : (("ai".equals(type) || "assistant".equals(type)) ? "assistant" : null);
                    String content = item.get("text");
                    if (role == null || content == null || content.isBlank()) continue;
                    if (content.length() > 4000) content = content.substring(0, 4000);
                    body.append(",{\"role\":\"").append(role)
                            .append("\",\"content\":\"").append(escapeJson(content)).append("\"}");
                }
            }
            body.append(",{\"role\":\"user\",\"content\":\"").append(escapeJson(userMessage))
                    .append("\"}],\"temperature\":0.4,\"max_tokens\":1600}");
            String jsonBody = body.toString();

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
                    return cleanAiText(text);
                }
            }

            log.warn("AI provider returned HTTP {}", response.statusCode());
            return getErrorResponse(lang, "provider_error");

        } catch (Exception e) {
            log.error("AI request failed", e);
            return getErrorResponse(lang, "provider_error");
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
            + "Format answers clearly, line by line. Start with a direct answer, then use short numbered steps or bullet points with one main idea per line. "
            + "For procedures, provide ordered steps; for comparisons, use clearly labelled items; for technical explanations, separate finding, reason, action, and caution. "
            + "Avoid dense paragraphs, run-on sentences, and unsupported claims. Use plain text line breaks that display correctly in the chat. "
            + "Be professional, clear, concise, and helpful. "
            + "For cybersecurity and digital-forensics questions, prioritize safe, lawful defensive guidance and explain risks clearly. "
            + "You can help with cybersecurity, digital forensics, incident response, evidence handling, account security, education, technology, and general productivity. "
            + "Adapt assistance to the authenticated user's role in the supplied user context. For ADMIN, explain governance, account access, audit interpretation, configuration and operational summaries; never claim to change settings or permissions unless a verified tool actually did so. "
            + "For CYBER_PRO, help triage reports, identify missing intake details, suggest severity considerations, coordinate response and draft assignments for human approval. "
            + "For FORENSICS, help with lawful examination planning, evidence documentation, chain-of-custody concepts, timelines and report drafting; never fabricate examination results or imply evidence integrity has been verified when it has not. "
            + "For ANALYST, assist with incident analysis, IOC interpretation, correlations, timelines and clearly labelled hypotheses. For INDIVIDUAL or USER, explain how to report and track incidents and give accessible defensive security guidance. "
            + "Do not claim access to live incidents, cases, evidence, audit logs, external threat intelligence or other system data unless that data is explicitly supplied in the context. Never reveal another user's data. "
            + "For consequential actions, provide recommendations and require the application's authorized workflow and human approval; do not claim to perform actions you cannot execute. "
            + "Do not automatically tell every user to contact the police. First understand the incident and give relevant, proportionate next steps. Mention police or emergency services only when the described situation reasonably calls for law-enforcement or immediate safety assistance. "
            + "Never invent phone numbers, email addresses, websites, agency names, reporting hotlines, laws, or official procedures. Give contact details only when they are explicitly provided in trusted context or verified source data. If you cannot verify them, say so plainly and direct the user to the official website or verified institutional channel without guessing a number. "
            + "For Tanzania-specific advice, distinguish confirmed local information from general guidance. Do not claim to have contacted police, TZ-CERT, a bank, a telecom provider, or any authority. "
            + "For incident reporting, guide the user to preserve relevant evidence safely, record dates and affected accounts or devices, avoid deleting files or confronting suspected attackers, and use the appropriate reporting channel based on the case. Do not request passwords, OTPs, or unnecessary sensitive personal data. "
            + "Use simple readable formatting only. Do not use Markdown bold, asterisks, hash headings, or decorative symbols. Use short paragraphs or numbered lists when useful. "
            + "User context: " + (context != null ? context : "None");
    }

    private String getErrorResponse(String lang, String error) {
        switch (lang != null ? lang.toLowerCase() : "en") {
            case "sw": return "❌ AI haiwezi kujibu kwa sasa. Tafadhali jaribu tena baadaye.";
            case "fr": return "❌ L'IA ne peut pas répondre pour le moment. Veuillez réessayer plus tard.";
            case "ar": return "❌ لا يمكن للذكاء الاصطناعي الرد حالياً. يرجى المحاولة لاحقاً.";
            default: return "❌ AI cannot respond right now. Please try again later.";
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

    private String cleanAiText(String text) {
        if (text == null) return "";
        return text.replace("**", "")
                .replace("* ", "• ")
                .replace("*", "");
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
