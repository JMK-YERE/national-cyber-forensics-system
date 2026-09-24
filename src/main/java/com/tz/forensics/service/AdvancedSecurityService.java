package com.tz.forensics.service;

import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

@Service
public class AdvancedSecurityService {

    // ========== 1. SSL CERTIFICATE CHECK ==========
    public Map<String, Object> checkSSL(String url) {
        Map<String, Object> result = new HashMap<>();
        result.put("url", url);
        result.put("valid", false);

        try {
            if (!url.startsWith("https://")) url = "https://" + url.replaceFirst("^https?://", "");
            String domain = URI.create(url).getHost();

            URL siteURL = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) siteURL.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("HEAD");
            conn.connect();

            javax.net.ssl.SSLSocketFactory factory = (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault();
            try (javax.net.ssl.SSLSocket socket = (javax.net.ssl.SSLSocket) factory.createSocket(domain, 443)) {
                socket.startHandshake();
                java.security.cert.Certificate[] certs = socket.getSession().getPeerCertificates();
                java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) certs[0];

                result.put("valid", true);
                result.put("subject", cert.getSubjectX500Principal().getName());
                result.put("issuer", cert.getIssuerX500Principal().getName());
                result.put("notBefore", cert.getNotBefore().toString());
                result.put("notAfter", cert.getNotAfter().toString());
                result.put("serialNumber", cert.getSerialNumber().toString());
                result.put("message", "SSL Certificate ni sahihi");
            }
        } catch (Exception e) {
            result.put("message", "SSL Certificate ina tatizo: " + e.getMessage());
        }
        return result;
    }

    // ========== 2. DNS LOOKUP ==========
    public Map<String, Object> dnsLookup(String domain) {
        Map<String, Object> result = new HashMap<>();
        try {
            domain = domain.replace("https://", "").replace("http://", "").split("/")[0];
            InetAddress[] addresses = InetAddress.getAllByName(domain);

            List<String> ips = new ArrayList<>();
            for (InetAddress addr : addresses) {
                ips.add(addr.getHostAddress());
            }

            result.put("success", true);
            result.put("domain", domain);
            result.put("ips", ips);
            result.put("hostName", addresses[0].getHostName());
            result.put("canonicalHost", addresses[0].getCanonicalHostName());
            result.put("reachable", addresses[0].isReachable(3000));
            result.put("message", "Domain inatafsiriwa kwa IPs " + ips.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Domain haipatikani: " + e.getMessage());
        }
        return result;
    }

    // ========== 3. IP REPUTATION ==========
    public Map<String, Object> checkIPReputation(String ip) {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("ip", ip);

            // Check private IP
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                int first = Integer.parseInt(parts[0]);
                int second = Integer.parseInt(parts[1]);
                if (first == 10 || first == 127 || (first == 172 && second >= 16 && second <= 31)
                        || (first == 192 && second == 168)) {
                    result.put("private", true);
                    result.put("message", "Hii ni private IP (bila hatari)");
                    return result;
                }
            }

            // Reverse DNS
            try {
                InetAddress addr = InetAddress.getByName(ip);
                result.put("reverseDns", addr.getCanonicalHostName());
                result.put("reachable", addr.isReachable(3000));
            } catch (Exception e) {
                result.put("reverseDns", "N/A");
            }

            // Check TOR ranges
            boolean suspicious = false;
            String[] torRanges = {"185.220.", "199.87.", "171.25.", "5.188.", "45.155."};
            for (String range : torRanges) {
                if (ip.startsWith(range)) { suspicious = true; break; }
            }

            result.put("suspicious", suspicious);
            result.put("message", suspicious ? "⚠️ IP inaonekana kama TOR exit node" : "✅ IP inaonekana salama");
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ========== 4. HTTP HEADERS ANALYZER ==========
    public Map<String, Object> analyzeHeaders(String url) {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> headers = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        List<String> good = new ArrayList<>();

        try {
            if (!url.startsWith("http")) url = "https://" + url;

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "CyberForensicsSystem/1.0")
                    .GET().build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            response.headers().map().forEach((k, v) -> headers.put(k, String.join(", ", v)));

            Map<String, String> securityHeaders = Map.of(
                "strict-transport-security", "HSTS",
                "content-security-policy", "CSP",
                "x-frame-options", "X-Frame-Options",
                "x-content-type-options", "X-Content-Type-Options",
                "referrer-policy", "Referrer-Policy",
                "permissions-policy", "Permissions-Policy"
            );

            for (Map.Entry<String, String> entry : securityHeaders.entrySet()) {
                if (headers.containsKey(entry.getKey())) good.add("✅ " + entry.getValue() + " imewekwa");
                else warnings.add("⚠️ " + entry.getValue() + " haipo");
            }

            if (headers.containsKey("server")) warnings.add("⚠️ Server info imefichuliwa: " + headers.get("server"));

            result.put("success", true);
            result.put("statusCode", response.statusCode());
            result.put("url", url);
            result.put("headers", headers);
            result.put("good", good);
            result.put("warnings", warnings);
            result.put("score", (good.size() * 100) / securityHeaders.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ========== 5. HASH ANALYZER ==========
    public Map<String, Object> analyzeHash(String hash) {
        Map<String, Object> result = new HashMap<>();
        result.put("hash", hash);
        result.put("length", hash.length());

        String type = "Unknown";
        if (hash.matches("[a-fA-F0-9]{32}")) type = "MD5";
        else if (hash.matches("[a-fA-F0-9]{40}")) type = "SHA-1";
        else if (hash.matches("[a-fA-F0-9]{64}")) type = "SHA-256";
        else if (hash.matches("[a-fA-F0-9]{128}")) type = "SHA-512";

        result.put("type", type);
        result.put("valid", !"Unknown".equals(type));

        if ("Unknown".equals(type)) {
            result.put("message", "❌ Hash si sahihi — inapaswa kuwa MD5, SHA-1, SHA-256, au SHA-512");
        } else {
            result.put("message", "✅ Hash ni sahihi (" + type + ")");
            result.put("virustotal", "https://www.virustotal.com/gui/file/" + hash);
        }
        return result;
    }

    // ========== 6. PASSWORD BREACH (HIBP) ==========
    public Map<String, Object> checkPasswordBreach(String password) {
        Map<String, Object> result = new HashMap<>();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = digest.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            String hash = sb.toString().toUpperCase();

            String prefix = hash.substring(0, 5);
            String suffix = hash.substring(5);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.pwnedpasswords.com/range/" + prefix))
                    .header("User-Agent", "CyberForensicsSystem")
                    .timeout(Duration.ofSeconds(10))
                    .GET().build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String[] lines = response.body().split("\n");
                int count = 0;
                for (String line : lines) {
                    String[] parts = line.split(":");
                    if (parts.length == 2 && parts[0].equalsIgnoreCase(suffix)) {
                        count = Integer.parseInt(parts[1].trim());
                        break;
                    }
                }

                result.put("success", true);
                result.put("breached", count > 0);
                result.put("count", count);
                result.put("message", count > 0
                        ? "🚨 Password imevuja " + count + " mara! Badilisha mara moja!"
                        : "✅ Password haijavuja kwenye breaches zinazojulikana");
            } else {
                result.put("success", false);
                result.put("message", "⚠️ API haijarudisha data");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ========== 7. PASSWORD STRENGTH ==========
    public Map<String, Object> checkPasswordStrength(String password) {
        Map<String, Object> result = new HashMap<>();
        int score = 0;
        List<String> feedback = new ArrayList<>();

        if (password.length() >= 8) score += 20; else feedback.add("❌ Ongeza urefu (8+)");
        if (password.length() >= 12) score += 10;
        if (password.length() >= 16) score += 10;
        if (password.matches(".*[A-Z].*")) score += 10; else feedback.add("❌ Ongeza herufi kubwa");
        if (password.matches(".*[a-z].*")) score += 10; else feedback.add("❌ Ongeza herufi ndogo");
        if (password.matches(".*\\d.*")) score += 10; else feedback.add("❌ Ongeza namba");
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) score += 10;
        else feedback.add("❌ Ongeza alama maalum");

        String lower = password.toLowerCase();
        if (lower.contains("password") || lower.contains("123456") || lower.contains("qwerty")) {
            score = Math.max(0, score - 30);
            feedback.add("⚠️ Password maarufu!");
        }

        score = Math.max(0, Math.min(100, score));
        result.put("score", score);
        result.put("rating", score >= 80 ? "STRONG" : score >= 60 ? "GOOD" : score >= 40 ? "WEAK" : "VERY WEAK");
        result.put("emoji", score >= 80 ? "🟢" : score >= 60 ? "🟡" : score >= 40 ? "🟠" : "🔴");
        result.put("feedback", feedback);
        return result;
    }

    // ========== 8. URL REPUTATION ==========
    public Map<String, Object> checkUrlReputation(String url) {
        Map<String, Object> result = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        List<String> safe = new ArrayList<>();
        int riskScore = 0;

        try {
            String original = url;
            if (!url.startsWith("http")) url = "https://" + url;

            URI uri = URI.create(url);
            String host = uri.getHost();

            result.put("url", original);
            result.put("host", host);

            if (original.startsWith("http://")) {
                riskScore += 30;
                warnings.add("🔴 HTTP (not HTTPS) — encryption haipo");
            } else safe.add("✅ HTTPS inatumika");

            if (host != null && host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                riskScore += 40;
                warnings.add("🔴 Inatumia IP address badala ya domain");
            }

            String[] riskyTlds = {".tk", ".ml", ".ga", ".cf", ".gq", ".top", ".work", ".click", ".zip"};
            if (host != null) {
                for (String tld : riskyTlds) {
                    if (host.toLowerCase().endsWith(tld)) {
                        riskScore += 30;
                        warnings.add("🔴 TLD ya kutiliwa shaka: " + tld);
                        break;
                    }
                }
            }

            String lower = url.toLowerCase();
            if (lower.contains("@") && !lower.startsWith("mailto:")) {
                riskScore += 35;
                warnings.add("🔴 Ina alama @ — inaweza kufanya redirect");
            }

            if (url.length() > 150) { riskScore += 15; warnings.add("🟠 URL ni ndefu sana"); }

            try {
                InetAddress.getByName(host);
                safe.add("✅ Domain inatafsiriwa");
            } catch (Exception e) {
                riskScore += 20;
                warnings.add("🟠 Domain haiwezi kutafsiriwa (DNS)");
            }

            riskScore = Math.min(100, riskScore);
            result.put("riskScore", riskScore);
            result.put("verdict", riskScore >= 60 ? "PHISHING/MALWARE" : riskScore >= 30 ? "SUSPICIOUS" : "SAFE");
            result.put("emoji", riskScore >= 60 ? "🚨" : riskScore >= 30 ? "⚠️" : "✅");
            result.put("warnings", warnings);
            result.put("safe", safe);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("riskScore", 100);
            result.put("verdict", "INVALID URL");
        }
        return result;
    }

    // ========== 9. DOMAIN AGE ==========
    public Map<String, Object> checkDomainAge(String domain) {
        Map<String, Object> result = new HashMap<>();
        try {
            domain = domain.replace("https://", "").replace("http://", "").split("/")[0];
            InetAddress address = InetAddress.getByName(domain);
            result.put("resolved", true);
            result.put("ip", address.getHostAddress());
            result.put("domain", domain);

            String[] riskyTlds = {".tk", ".ml", ".ga", ".cf", ".gq", ".top", ".work", ".click"};
            boolean risky = false;
            for (String tld : riskyTlds) {
                if (domain.toLowerCase().endsWith(tld)) { risky = true; break; }
            }

            result.put("riskyTld", risky);
            result.put("message", risky ? "⚠️ Domain ina TLD inayotiliwa shaka" : "✅ Domain TLD ni ya kawaida");
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ========== 10. SSL EXPIRY CHECK ==========
    public Map<String, Object> checkSSLSecure(String url) {
        return checkSSL(url);
    }
}
