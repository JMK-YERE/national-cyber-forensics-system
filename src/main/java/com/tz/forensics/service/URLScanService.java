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
import java.util.HashMap;
import java.util.Map;

@Service
public class URLScanService {

    private static final Logger log = LoggerFactory.getLogger(URLScanService.class);

    @Value("${urlscan.api.key:}")
    private String apiKey;

    private static final String URLSCAN_API = "https://urlscan.io/api/v1";

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }

    // ===== SUBMIT URL FOR SCANNING =====
    public Map<String, Object> scanUrl(String url) {
        Map<String, Object> result = new HashMap<>();
        result.put("url", url);

        if (!isConfigured()) {
            result.put("success", false);
            result.put("error", "URLScan API key haipo");
            return result;
        }

        try {
            String jsonBody = "{\"url\":\"" + url.replace("\"", "\\\"") + "\",\"visibility\":\"public\"}";

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URLSCAN_API + "/scan/"))
                    .header("Content-Type", "application/json")
                    .header("API-Key", apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("URLScan submit status: {}", response.statusCode());

            if (response.statusCode() == 200) {
                result.put("success", true);
                result.put("message", "URL imetumwa kwa scanning. Inachukua sekunde 10-30.");
                result.put("resultUrl", extractValue(response.body(), "\"result\":\""));
                result.put("apiUrl", extractValue(response.body(), "\"api\":\""));
                result.put("uuid", extractValue(response.body(), "\"uuid\":\""));
            } else if (response.statusCode() == 429) {
                result.put("success", false);
                result.put("error", "Rate limit — subiri dakika 1");
            } else {
                result.put("success", false);
                result.put("error", "Error: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("URLScan failed: {}", e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ===== GET SCAN RESULT =====
    public Map<String, Object> getResult(String uuid) {
        Map<String, Object> result = new HashMap<>();

        if (!isConfigured()) {
            result.put("success", false);
            result.put("error", "URLScan API key haipo");
            return result;
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URLSCAN_API + "/result/" + uuid + "/"))
                    .header("API-Key", apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .GET().build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();

                result.put("success", true);
                result.put("verdict", extractValue(body, "\"malicious\":"));
                result.put("score", extractValue(body, "\"score\":"));
                result.put("url", extractValue(body, "\"url\":\""));
                result.put("domain", extractValue(body, "\"domain\":\""));
                result.put("ip", extractValue(body, "\"ip\":\""));
                result.put("country", extractValue(body, "\"country\":\""));
                result.put("server", extractValue(body, "\"server\":\""));
                result.put("screenshot", "https://urlscan.io/screenshots/" + uuid + ".png");
            } else if (response.statusCode() == 404) {
                result.put("success", false);
                result.put("error", "Scan bado haijakamilika. Subiri sekunde 10.");
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

    // ===== SEARCH BY DOMAIN =====
    public Map<String, Object> searchDomain(String domain) {
        Map<String, Object> result = new HashMap<>();

        if (!isConfigured()) {
            result.put("success", false);
            result.put("error", "URLScan API key haipo");
            return result;
        }

        try {
            String query = URLEncoder.encode("domain:" + domain, StandardCharsets.UTF_8);
            String url = URLSCAN_API + "/search/?q=" + query + "&size=10";

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("API-Key", apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .GET().build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                result.put("success", true);
                result.put("domain", domain);
                result.put("results", response.body());
                result.put("count", countOccurrences(response.body(), "\"_id\":"));
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

    private String extractValue(String json, String key) {
        try {
            int idx = json.indexOf(key);
            if (idx == -1) return null;
            int start = idx + key.length();
            if (key.endsWith("\":")) start++; // for string values
            if (key.endsWith(":")) {
                // number or boolean
                int end = start;
                while (end < json.length() && ",}\n\r ".indexOf(json.charAt(end)) == -1) end++;
                return json.substring(start, end).trim();
            }
            start = json.indexOf('"', start);
            if (start == -1) return null;
            start++;
            int end = json.indexOf('"', start);
            if (end == -1) return null;
            return json.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    private int countOccurrences(String str, String sub) {
        int count = 0, idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) { count++; idx += sub.length(); }
        return count;
    }
}
