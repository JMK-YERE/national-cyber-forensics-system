package com.tz.forensics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VirusTotalService {

    private static final Logger log = LoggerFactory.getLogger(VirusTotalService.class);

    @Value("${virustotal.api.key:}")
    private String apiKey;

    private static final String VT_API = "https://www.virustotal.com/api/v3";

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("null");
    }

    // ========== URL CHECK ==========
    public Map<String, Object> checkUrl(String url) {
        Map<String, Object> result = new HashMap<>();
        result.put("url", url);
        result.put("type", "URL");

        if (!isConfigured()) {
            result.put("success", false);
            result.put("error", "VirusTotal API key haipo");
            return result;
        }

        try {
            String urlId = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(url.getBytes(StandardCharsets.UTF_8));

            HttpResponse<String> response = callVT(VT_API + "/urls/" + urlId);

            if (response.statusCode() == 200) {
                result.put("success", true);
                parseAndFill(response.body(), result);
                result.put("message", "Matokeo yamepatikana");
            } else if (response.statusCode() == 404) {
                // URL haipo — submit kwa scan
                log.info("URL haipo VT, submitting for scan");
                HttpResponse<String> submitResponse = submitUrl(url);
                result.put("success", submitResponse.statusCode() == 200);
                result.put("message", "URL imetumwa kwa scanning. Inachukua sekunde 30-60. Jaribu tena baadaye.");
            } else if (response.statusCode() == 401) {
                result.put("success", false);
                result.put("error", "API key si sahihi");
            } else if (response.statusCode() == 429) {
                result.put("success", false);
                result.put("error", "Rate limit — subiri dakika 1 (BURE: 4 req/min)");
            } else {
                result.put("success", false);
                result.put("error", "Error: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("VT URL check failed: {}", e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ========== FILE HASH CHECK ==========
    public Map<String, Object> checkFileHash(String hash) {
        Map<String, Object> result = new HashMap<>();
        result.put("hash", hash);
        result.put("type", "FILE");

        if (!isConfigured()) {
            result.put("success", false);
            result.put("error", "VirusTotal API key haipo");
            return result;
        }

        try {
            HttpResponse<String> response = callVT(VT_API + "/files/" + hash);

            if (response.statusCode() == 200) {
                result.put("success", true);
                parseAndFill(response.body(), result);
                result.put("message", "File hash ipo kwenye VirusTotal");
            } else if (response.statusCode() == 404) {
                result.put("success", false);
                result.put("error", "Hash haipo kwenye database ya VirusTotal");
            } else {
                result.put("success", false);
                result.put("error", "Error: " + response.statusCode());
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ========== IP CHECK ==========
    public Map<String, Object> checkIP(String ip) {
        Map<String, Object> result = new HashMap<>();
        result.put("ip", ip);
        result.put("type", "IP");

        if (!isConfigured()) {
            result.put("success", false);
            result.put("error", "VirusTotal API key haipo");
            return result;
        }

        try {
            HttpResponse<String> response = callVT(VT_API + "/ip_addresses/" + ip);

            if (response.statusCode() == 200) {
                result.put("success", true);
                parseAndFill(response.body(), result);
                result.put("message", "Matokeo ya IP yamepatikana");
            } else if (response.statusCode() == 404) {
                result.put("success", false);
                result.put("error", "IP haipo kwenye database ya VirusTotal");
            } else {
                result.put("success", false);
                result.put("error", "Error: " + response.statusCode());
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ========== DOMAIN CHECK ==========
    public Map<String, Object> checkDomain(String domain) {
        Map<String, Object> result = new HashMap<>();
        result.put("domain", domain);
        result.put("type", "DOMAIN");

        if (!isConfigured()) {
            result.put("success", false);
            result.put("error", "VirusTotal API key haipo");
            return result;
        }

        try {
            HttpResponse<String> response = callVT(VT_API + "/domains/" + domain);

            if (response.statusCode() == 200) {
                result.put("success", true);
                parseAndFill(response.body(), result);
                result.put("message", "Matokeo ya domain yamepatikana");
            } else if (response.statusCode() == 404) {
                result.put("success", false);
                result.put("error", "Domain haipo kwenye database ya VirusTotal");
            } else {
                result.put("success", false);
                result.put("error", "Error: " + response.statusCode());
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ========== HELPER — CALL VT ==========
    private HttpResponse<String> callVT(String endpoint) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("x-apikey", apiKey)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .GET().build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // ========== SUBMIT URL FOR SCAN ==========
    private HttpResponse<String> submitUrl(String url) throws Exception {
        String formData = "url=" + URLEncoder.encode(url, StandardCharsets.UTF_8);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(VT_API + "/urls"))
                .header("x-apikey", apiKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // ========== PARSE RESPONSE ==========
    private void parseAndFill(String json, Map<String, Object> result) {
        try {
            // Malicious count
            int malicious = extractInt(json, "\"malicious\":");
            int suspicious = extractInt(json, "\"suspicious\":");
            int harmless = extractInt(json, "\"harmless\":");
            int undetected = extractInt(json, "\"undetected\":");

            result.put("malicious", malicious);
            result.put("suspicious", suspicious);
            result.put("harmless", harmless);
            result.put("undetected", undetected);

            int total = malicious + suspicious + harmless + undetected;
            result.put("totalEngines", total);

            // Verdict
            String verdict;
            String emoji;
            if (malicious > 5) { verdict = "MALICIOUS"; emoji = "🚨"; }
            else if (malicious > 0) { verdict = "SUSPICIOUS"; emoji = "⚠️"; }
            else if (suspicious > 0) { verdict = "SUSPICIOUS"; emoji = "⚠️"; }
            else { verdict = "SAFE"; emoji = "✅"; }

            result.put("verdict", verdict);
            result.put("emoji", emoji);

            // Additional info
            String reputation = extractValue(json, "\"reputation\":");
            if (reputation != null) result.put("reputation", reputation);

            String country = extractValue(json, "\"country\":\"");
            if (country != null) result.put("country", country);

            String asOwner = extractValue(json, "\"as_owner\":\"");
            if (asOwner != null) result.put("asOwner", asOwner);

            String registrar = extractValue(json, "\"registrar\":\"");
            if (registrar != null) result.put("registrar", registrar);

        } catch (Exception e) {
            log.error("Parse failed: {}", e.getMessage());
        }
    }

    private int extractInt(String json, String key) {
        try {
            int idx = json.indexOf(key);
            if (idx == -1) return 0;
            int start = idx + key.length();
            int end = start;
            while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String extractValue(String json, String key) {
        try {
            int idx = json.indexOf(key);
            if (idx == -1) return null;
            int start = idx + key.length();
            int end = json.indexOf('"', start);
            if (end == -1) return null;
            return json.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }
}
