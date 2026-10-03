package com.tz.forensics.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Tanzania-friendly SMS adapter using Africa's Talking.
 * Configure AT_USERNAME, AT_API_KEY and optionally AT_SENDER_ID on Render.
 */
@Service
public class SmsService {
    @Value("${AT_USERNAME:}")
    private String username;

    @Value("${AT_API_KEY:}")
    private String apiKey;

    @Value("${AT_SENDER_ID:}")
    private String senderId;

    public boolean isConfigured() {
        return username != null && !username.isBlank() && apiKey != null && !apiKey.isBlank();
    }

    public boolean sendSms(String phone, String message) {
        if (phone == null || phone.isBlank() || !isConfigured()) {
            System.out.println("⚠️ SMS not configured or phone missing.");
            return false;
        }
        try {
            String body = "username=" + enc(username)
                    + "&to=" + enc(phone)
                    + "&message=" + enc(message);
            if (senderId != null && !senderId.isBlank()) {
                body += "&from=" + enc(senderId);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.africastalking.com/version1/messaging"))
                    .header("apiKey", apiKey)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            boolean ok = response.statusCode() >= 200 && response.statusCode() < 300;
            System.out.println((ok ? "✅" : "❌") + " SMS response: " + response.statusCode());
            return ok;
        } catch (Exception e) {
            System.err.println("❌ SMS failed: " + e.getMessage());
            return false;
        }
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
