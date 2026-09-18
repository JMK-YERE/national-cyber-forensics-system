package com.tz.forensics.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class WhatsAppService {

    @Value("${whatsapp.default.phone:}")
    private String defaultPhone;

    @Value("${whatsapp.default.apikey:}")
    private String defaultApiKey;

    @Value("${whatsapp.admin.phone:}")
    private String adminPhone;

    @Value("${whatsapp.admin.apikey:}")
    private String adminApiKey;

    private static final String API_URL = "https://api.callmebot.com/whatsapp.php";

    // ===== METHOD YA MSINGI =====
    public boolean sendWhatsApp(String phoneNumber, String apiKey, String message) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            System.out.println("⚠️ WhatsApp: No phone");
            return false;
        }
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = defaultApiKey;
        }
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("⚠️ WhatsApp: No API key");
            return false;
        }

        try {
            String encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8);
            String url = API_URL + "?phone=" + phoneNumber
                    + "&text=" + encodedMsg
                    + "&apikey=" + apiKey;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("✅ WhatsApp sent to " + phoneNumber + " (" + response.statusCode() + ")");
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("❌ WhatsApp failed: " + e.getMessage());
            return false;
        }
    }

    // ===== METHOD YA INCIDENT (args 3 — inatumika kwenye IncidentController) =====
    public void sendIncidentAlert(String incidentId, String title, String severity) {
        if (adminPhone == null || adminPhone.isEmpty()) {
            System.out.println("⚠️ WhatsApp: Admin phone not set");
            return;
        }
        String emoji = switch (severity != null ? severity.toUpperCase() : "MEDIUM") {
            case "CRITICAL" -> "🚨";
            case "HIGH" -> "⚠️";
            case "MEDIUM" -> "📌";
            default -> "ℹ️";
        };
        String message = emoji + " *CYBER FORENSICS TZ*\n\n"
                + "📋 *Tukio Jipya*\n"
                + "🆔 ID: " + incidentId + "\n"
                + "📝 " + title + "\n"
                + "⚡ Severity: " + severity + "\n\n"
                + "Ingia mfumo kwa maelezo zaidi 🇹🇿";
        sendWhatsApp(adminPhone, adminApiKey, message);
    }

    // ===== METHOD YA WELCOME =====
    public void sendWelcome(String phone, String apiKey, String username) {
        String message = "🇹🇿 *Karibu Cyber Forensics TZ*\n\n"
                + "Habari " + username + "!\n\n"
                + "Akaunti yako imefunguliwa kwa mafanikio.\n"
                + "Unaweza kuanza kuripoti matukio ya usalama.\n\n"
                + "Asante! 🇹🇿";
        sendWhatsApp(phone, apiKey, message);
    }

    // ===== METHOD YA EVIDENCE =====
    public void sendEvidenceAlert(String evidenceName) {
        if (adminPhone == null || adminPhone.isEmpty()) return;
        String message = "🔬 *EVIDENCE MPYA*\n\n"
                + "File: " + evidenceName + "\n"
                + "Imehifadhiwa kwa AES-256 ✅\n\n"
                + "Angalia mfumo 🇹🇿";
        sendWhatsApp(adminPhone, adminApiKey, message);
    }

    // ===== TEST =====
    public void sendTestMessage() {
        sendWhatsApp(adminPhone, adminApiKey,
                "🇹🇿 *TEST MESSAGE*\n\nMfumo wako unafanya kazi! ✅\nWhatsApp notifications ziko tayari.");
    }
}
