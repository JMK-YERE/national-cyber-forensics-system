package com.tz.forensics.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Service
public class VirusTotalService {

    @Value("${virustotal.api.key:}")
    private String apiKey;

    private static final String VT_API = "https://www.virustotal.com/api/v3";

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }

    // ===== CHECK URL =====
    public String checkUrl(String url) {
        if (!isConfigured()) return "{\"error\":\"VirusTotal API key haipo. Pata bure: https://www.virustotal.com/gui/join-us\"}";

        try {
            String urlId = Base64.getUrlEncoder().withoutPadding().encodeToString(url.getBytes(StandardCharsets.UTF_8));
            String endpoint = VT_API + "/urls/" + urlId;

            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("x-apikey", apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .GET().build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else if (response.statusCode() == 404) {
                // URL haipo — submit kwa scan
                return submitUrl(url);
            } else {
                return "{\"error\":\"VirusTotal error: " + response.statusCode() + "\"}";
            }
        } catch (Exception e) {
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    // ===== SUBMIT URL =====
    private String submitUrl(String url) {
        try {
            String formData = "url=" + URLEncoder.encode(url, StandardCharsets.UTF_8);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VT_API + "/urls"))
                    .header("x-apikey", apiKey)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    // ===== CHECK FILE HASH =====
    public String checkFileHash(String hash) {
        if (!isConfigured()) return "{\"error\":\"VirusTotal API key haipo\"}";
        try {
            String endpoint = VT_API + "/files/" + hash;
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("x-apikey", apiKey)
                    .timeout(Duration.ofSeconds(20))
                    .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) return response.body();
            return "{\"error\":\"Hash haipo kwenye VirusTotal database\"}";
        } catch (Exception e) {
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    // ===== CHECK IP =====
    public String checkIP(String ip) {
        if (!isConfigured()) return "{\"error\":\"VirusTotal API key haipo\"}";
        try {
            String endpoint = VT_API + "/ip_addresses/" + ip;
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("x-apikey", apiKey)
                    .timeout(Duration.ofSeconds(20))
                    .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    // ===== CHECK DOMAIN =====
    public String checkDomain(String domain) {
        if (!isConfigured()) return "{\"error\":\"VirusTotal API key haipo\"}";
        try {
            String endpoint = VT_API + "/domains/" + domain;
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("x-apikey", apiKey)
                    .timeout(Duration.ofSeconds(20))
                    .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    // ===== PARSE RESULTS =====
    public int parseMalicious(String json) {
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"malicious\"\\s*:\\s*(\\d+)");
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {}
        return -1;
    }

    public int parseSuspicious(String json) {
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"suspicious\"\\s*:\\s*(\\d+)");
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {}
        return -1;
    }

    public int parseHarmless(String json) {
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"harmless\"\\s*:\\s*(\\d+)");
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {}
        return -1;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
