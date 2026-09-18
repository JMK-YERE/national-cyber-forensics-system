package com.tz.forensics.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class WhatsAppService {

    // CallMeBot API - BURE MILELE
    private static final String API_URL = "https://api.callmebot.com/whatsapp.php";

    public boolean sendWhatsApp(String phoneNumber, String apiKey, String message) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            System.out.println("⚠️ WhatsApp: No phone number");
            return false;
        }
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("⚠️ WhatsApp: No API key. Get free at: https://www.callmebot.com/blog/free-api-whatsapp-messages/");
            System.out.println("   Would send to: " + phoneNumber);
            System.out.println("   Message: " + message);
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
            System.out.println("✅ WhatsApp sent to " + phoneNumber + " (status: " + response.statusCode() + ")");
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("❌ WhatsApp failed: " + e.getMessage());
            return false;
        }
    }

    public void sendIncidentAlert(String phone, String apiKey, String incidentId, String title, String severity) {
        String emoji = switch (severity != null ? severity.toUpperCase() : "MEDIUM") {
            case "CRITICAL" -> "🚨";
            case "HIGH" -> "⚠️";
            case "MEDIUM" -> "📌";
            default -> "ℹ️";
        };
        String message = emoji + " *CYBER FORENSICS TZ*\n\n"
                + "📋 Tukio Jipya\n"
                + "🆔 ID: " + incidentId + "\n"
                + "📝 Title: " + title + "\n"
                + "⚡ Severity: " + severity + "\n\n"
                + "Ingia mfumo kwa maelezo zaidi 🇹🇿";
        sendWhatsApp(phone, apiKey, message);
    }

    public void sendWelcome(String phone, String apiKey, String username) {
        String message = "🇹🇿 *Karibu Cyber Forensics TZ*\n\n"
                + "Habari " + username + "!\n\n"
                + "Akaunti yako imefunguliwa kwa mafanikio.\n"
                + "Unaweza kuanza kuripoti matukio ya usalama.\n\n"
                + "Asante! 🇹🇿";
        sendWhatsApp(phone, apiKey, message);
    }

    public void sendEvidenceAlert(String phone, String apiKey, String evidenceName) {
        String message = "🔬 *EVIDENCE MPYA*\n\n"
                + "File: " + evidenceName + "\n"
                + "Imehifadhiwa kwa usalama (AES-256)\n\n"
                + "Angalia mfumo kwa maelezo 🇹🇿";
        sendWhatsApp(phone, apiKey, message);
    }
}
