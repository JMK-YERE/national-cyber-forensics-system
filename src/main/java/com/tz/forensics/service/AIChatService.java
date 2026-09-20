package com.tz.forensics.service;

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

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String model;

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }

    public String chat(String userMessage, String context) {
        if (!isConfigured()) {
            return fallbackResponse(userMessage);
        }

        try {
            String systemPrompt = "Wewe ni AI Assistant wa National Cyber Forensics System - Tanzania. "
                + "Unasaidia watumiaji kuhusu usalama wa mtandao, digital forensics, na matumizi ya mfumo. "
                + "Jibu kwa Kiswahili kwa ufupi (sentensi 2-5). Kuwa msaada na wa kirafiki. "
                + "Kama swali ni kuhusu usalama, toa ushauri wa kitaalamu. "
                + "Context: " + (context != null ? context : "General security question");

            String jsonBody = buildJsonRequest(systemPrompt + "\n\nUser: " + userMessage);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            String url = BASE_URL + model + ":generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractText(response.body());
            } else {
                System.err.println("Gemini API error: " + response.statusCode() + " - " + response.body());
                return fallbackResponse(userMessage);
            }
        } catch (Exception e) {
            System.err.println("AI Chat failed: " + e.getMessage());
            return fallbackResponse(userMessage);
        }
    }

    private String buildJsonRequest(String prompt) {
        String escaped = prompt.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
        return "{\"contents\":[{\"parts\":[{\"text\":\"" + escaped + "\"}]}]}";
    }

    private String extractText(String jsonResponse) {
        try {
            Pattern pattern = Pattern.compile("\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
            Matcher matcher = pattern.matcher(jsonResponse);
            if (matcher.find()) {
                String text = matcher.group(1);
                return text.replace("\\n", "\n")
                           .replace("\\\"", "\"")
                           .replace("\\\\", "\\");
            }
        } catch (Exception e) {
            System.err.println("Parse failed: " + e.getMessage());
        }
        return "Samahani, sikupata jibu sahihi.";
    }

    private String fallbackResponse(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("password")) {
            return "Kwa password nzuri: tumia herufi 12+, kubwa na ndogo, namba, na alama (!@#$%). Usitumie password ile ile kwenye sites zote.";
        }
        if (lower.contains("phishing")) {
            return "Phishing ni email au SMS za kudanganya. Angalia URL vizuri — hasa zile zinazoanza http:// badala ya https://. Usibonyeze links za kutiliwa shaka.";
        }
        if (lower.contains("2fa") || lower.contains("two factor")) {
            return "2FA (Two-Factor Authentication) inaongeza usalama. Weka kwenye Facebook, Instagram, WhatsApp, Gmail. Tumia Google Authenticator app.";
        }
        if (lower.contains("ransomware")) {
            return "Ransomware inafunga files zako. Kinga: backup data kila wiki, sasisha OS, usifungue attachments za kutiliwa shaka, weka antivirus.";
        }
        if (lower.contains("breach") || lower.contains("imevuja")) {
            return "Kama email yako imevuja: badilisha password mara moja, weka 2FA, angalia accounts zako nyingine, usibonyeze links za kutiliwa shaka.";
        }
        if (lower.contains("whatsapp")) {
            return "WhatsApp security: weka Two-Step Verification (Settings → Account), angalia Linked Devices, usifungue links za kutiliwa shaka.";
        }
        if (lower.contains("facebook") || lower.contains("instagram")) {
            return "Meta platforms security: weka 2FA, angalia Login Alerts, ondoa apps zisizotumika, angalia 'Where You're Logged In'.";
        }
        if (lower.contains("tiktok")) {
            return "TikTok security: weka 2FA (Settings → Security), angalia Manage Devices, usibonyeze links kutoka DM.";
        }
        if (lower.contains("halo") || lower.contains("habari") || lower.contains("hello")) {
            return "Habari! Mimi ni AI Assistant wa Cyber Forensics TZ. Naweza kukusaidia kuhusu usalama wa mtandao, password, phishing, 2FA, na zaidi. Uliza swali lako!";
        }
        if (lower.contains("asante") || lower.contains("thanks")) {
            return "Karibu sana! Kama una swali lingine kuhusu usalama, nipo hapa kukusaidia. 🛡️";
        }
        return "Mimi ni AI Assistant wa Cyber Forensics TZ. Naweza kukusaidia kuhusu: passwords, phishing, 2FA, ransomware, breaches, social media security. Uliza swali lako kwa Kiswahili au Kiingereza!";
    }
}
