package com.tz.forensics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.InetAddress;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("null");
    }

    // ========== URL CHECK ==========
    public Map<String, Object> checkUrl(String url) {
        url = validatePublicUrl(url);
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
                result.put("success", submitResponse.statusCode() >= 200 && submitResponse.statusCode() < 300);
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
        hash = validateHash(hash);
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
        ip = validatePublicIp(ip);
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
        domain = validatePublicDomain(domain);
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

    private String validatePublicUrl(String value) {
        if (value == null || value.isBlank() || value.length() > 2048) throw new IllegalArgumentException("URL si sahihi.");
        String normalized = value.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$") ? value.trim() : "https://" + value.trim();
        URI uri = URI.create(normalized);
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) throw new IllegalArgumentException("HTTP/HTTPS pekee.");
        if (uri.getUserInfo() != null || uri.getHost() == null) throw new IllegalArgumentException("URL si salama.");
        int port = uri.getPort();
        if (port != -1 && port != 80 && port != 443) throw new IllegalArgumentException("Port hairuhusiwi.");
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                throw new IllegalArgumentException("Private/local host hairuhusiwi.");
            }
        }
        return uri.toString();
    }

    private String validateHash(String value) {
        if (value == null || !value.trim().matches("(?i)([a-f0-9]{32}|[a-f0-9]{40}|[a-f0-9]{64}|[a-f0-9]{128})")) throw new IllegalArgumentException("Hash si sahihi.");
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String validatePublicIp(String value) throws Exception {
        if (value == null || value.isBlank() || value.length() > 45) throw new IllegalArgumentException("IP si sahihi.");
        InetAddress address = InetAddress.getByName(value.trim());
        if (!address.getHostAddress().equalsIgnoreCase(value.trim()) || address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress() || address.isMulticastAddress()) throw new IllegalArgumentException("IP private/local/reserved hairuhusiwi.");
        return value.trim();
    }

    private String validatePublicDomain(String value) throws Exception {
        if (value == null || value.isBlank() || value.length() > 253) throw new IllegalArgumentException("Domain si sahihi.");
        String domain = value.trim().toLowerCase(java.util.Locale.ROOT);
        if (domain.matches("^[a-z][a-z0-9+.-]*://.*$")) {
            URI uri = URI.create(domain);
            if (uri.getUserInfo() != null || uri.getHost() == null) throw new IllegalArgumentException("Domain si sahihi.");
            domain = uri.getHost().toLowerCase(java.util.Locale.ROOT);
        }
        if (!domain.matches("(?=.{1,253}$)([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}")) throw new IllegalArgumentException("Domain si sahihi.");
        for (InetAddress address : InetAddress.getAllByName(domain)) {
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress() || address.isMulticastAddress()) throw new IllegalArgumentException("Private/local domain hairuhusiwi.");
        }
        return domain;
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
            JsonNode root = objectMapper.readTree(json);
            JsonNode attrs = root.path("data").path("attributes");
            JsonNode stats = attrs.path("last_analysis_stats");
            int malicious = stats.path("malicious").asInt(0);
            int suspicious = stats.path("suspicious").asInt(0);
            int harmless = stats.path("harmless").asInt(0);
            int undetected = stats.path("undetected").asInt(0);
            int timeout = stats.path("timeout").asInt(0);
            result.put("malicious", malicious);
            result.put("suspicious", suspicious);
            result.put("harmless", harmless);
            result.put("undetected", undetected);
            result.put("timeout", timeout);
            result.put("totalEngines", malicious + suspicious + harmless + undetected + timeout);
            String verdict = malicious > 0 ? "MALICIOUS" : suspicious > 0 ? "SUSPICIOUS" : "SAFE";
            result.put("verdict", verdict);
            result.put("emoji", "MALICIOUS".equals(verdict) ? "🚨" : "SUSPICIOUS".equals(verdict) ? "⚠️" : "✅");
            if (attrs.hasNonNull("reputation")) result.put("reputation", attrs.get("reputation").asInt());
            if (attrs.hasNonNull("country")) result.put("country", attrs.get("country").asText());
            if (attrs.hasNonNull("as_owner")) result.put("asOwner", attrs.get("as_owner").asText());
            if (attrs.hasNonNull("registrar")) result.put("registrar", attrs.get("registrar").asText());
            if (attrs.hasNonNull("last_final_url")) result.put("finalUrl", attrs.get("last_final_url").asText());
            if (attrs.hasNonNull("last_http_response_code")) result.put("httpCode", attrs.get("last_http_response_code").asInt());
        } catch (Exception e) {
            log.error("Parse failed: {}", e.getMessage());
            result.put("success", false);
            result.put("error", "VirusTotal response could not be parsed");
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
