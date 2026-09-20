package com.tz.forensics.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AdvancedSecurityService {

    private static final String VIRUSTOTAL_API = "https://www.virustotal.com/api/v3";
    private static final String URLSCAN_API = "https://urlscan.io/api/v1/scan/";
    private static final String HIBP_API = "https://api.pwnedpasswords.com/range/";
    private static final String ABUSEIPDB_API = "https://api.abuseipdb.com/api/v2/check";

    // ===== 1. SSL CERTIFICATE CHECK =====
    public Map<String, Object> checkSSL(String url) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!url.startsWith("https://")) {
                result.put("valid", false);
                result.put("message", "🔴 URL haitumii HTTPS — sio salama");
                return result;
            }

            String domain = URI.create(url).getHost();
            URL siteURL = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) siteURL.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();

            javax.net.ssl.SSLSocketFactory factory = (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault();
            try (javax.net.ssl.SSLSocket socket = (javax.net.ssl.SSLSocket) factory.createSocket(domain, 443)) {
                socket.startHandshake();
                javax.security.cert.X509Certificate[] certs = socket.getSession().getPeerCertificateChain();
                javax.security.cert.X509Certificate cert = certs[0];

                result.put("valid", true);
                result.put("subject", cert.getSubjectDN().getName());
                result.put("issuer", cert.getIssuerDN().getName());
                result.put("notBefore", cert.getNotBefore());
                result.put("notAfter", cert.getNotAfter());
                result.put("message", "✅ SSL Certificate ni sahihi");
            } catch (Exception e) {
                result.put("valid", false);
                result.put("message", "🔴 SSL Certificate ina tatizo: " + e.getMessage());
            }
        } catch (Exception e) {
            result.put("valid", false);
            result.put("message", "❌ Error: " + e.getMessage());
        }
        return result;
    }

    // ===== 2. DNS LOOKUP =====
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
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ===== 3. IP REPUTATION CHECK =====
    public Map<String, Object> checkIPReputation(String ip) {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("ip", ip);

            // Check kama ni private IP
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                int first = Integer.parseInt(parts[0]);
                int second = Integer.parseInt(parts[1]);
                if (first == 10 || first == 127 ||
                    (first == 172 && second >= 16 && second <= 31) ||
                    (first == 192 && second == 168)) {
                    result.put("private", true);
                    result.put("message", "ℹ️ Hii ni private IP");
                    return result;
                }
            }

            // Check reverse DNS
            try {
                InetAddress addr = InetAddress.getByName(ip);
                result.put("reverseDns", addr.getCanonicalHostName());
                result.put("reachable", addr.isReachable(3000));
            } catch (Exception e) {
                result.put("reverseDns", "N/A");
            }

            // Known malicious IP ranges (basic check)
            boolean suspicious = false;
            String[] suspiciousRanges = {"1.1.1.", "8.8.8.", "185.220.", "5.188.", "45.155."};
            String[] torRanges = {"185.220.", "199.87.", "171.25."};
            for (String range : torRanges) {
                if (ip.startsWith(range)) {
                    suspicious = true;
                    result.put("tor", true);
                    break;
                }
            }

            result.put("suspicious", suspicious);
            result.put("message", suspicious ? "⚠️ IP inaonekana kama TOR exit node" : "✅ IP inaonekana salama");

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ===== 4. HTTP HEADERS ANALYZER =====
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
                    .header("User-Agent", "CyberForensicsTZ/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            response.headers().map().forEach((k, v) -> headers.put(k, String.join(", ", v)));

            // Check security headers
            Map<String, String> securityHeaders = Map.of(
                "strict-transport-security", "HSTS",
                "content-security-policy", "CSP",
                "x-frame-options", "X-Frame-Options",
                "x-content-type-options", "X-Content-Type-Options",
                "referrer-policy", "Referrer-Policy",
                "permissions-policy", "Permissions-Policy"
            );

            for (Map.Entry<String, String> entry : securityHeaders.entrySet()) {
                if (headers.containsKey(entry.getKey())) {
                    good.add("✅ " + entry.getValue() + " imewekwa");
                } else {
                    warnings.add("⚠️ " + entry.getValue() + " haipo");
                }
            }

            // Server header
            if (headers.containsKey("server")) {
                warnings.add("⚠️ Server info imefichuliwa: " + headers.get("server"));
            }

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

    // ===== 5. DOMAIN AGE CHECK (Basic WHOIS) =====
    public Map<String, Object> checkDomainAge(String domain) {
        Map<String, Object> result = new HashMap<>();
        try {
            domain = domain.replace("https://", "").replace("http://", "").split("/")[0];

            // Check DNS first
            InetAddress address = InetAddress.getByName(domain);
            result.put("resolved", true);
            result.put("ip", address.getHostAddress());

            // Simple check — kama domain inaonekana kama malicious TLD
            String[] riskyTlds = {".tk", ".ml", ".ga", ".cf", ".gq", ".top", ".work", ".click", ".zip", ".mov"};
            boolean risky = false;
            for (String tld : riskyTlds) {
                if (domain.toLowerCase().endsWith(tld)) {
                    risky = true;
                    break;
                }
            }

            // Check length of domain
            String[] parts = domain.split("\\.");
            if (parts.length > 4) {
                result.put("manySubdomains", true);
            }

            result.put("riskyTld", risky);
            result.put("domain", domain);
            result.put("message", risky ? "⚠️ Domain ina TLD inayotiliwa shaka" : "✅ Domain TLD ni ya kawaida");

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ===== 6. HASH ANALYZER =====
    public Map<String, Object> analyzeHash(String hash) {
        Map<String, Object> result = new HashMap<>();
        result.put("hash", hash);

        String type = "Unknown";
        if (hash.matches("[a-fA-F0-9]{32}")) type = "MD5";
        else if (hash.matches("[a-fA-F0-9]{40}")) type = "SHA-1";
        else if (hash.matches("[a-fA-F0-9]{64}")) type = "SHA-256";
        else if (hash.matches("[a-fA-F0-9]{128}")) type = "SHA-512";

        result.put("type", type);
        result.put("valid", !"Unknown".equals(type));
        result.put("length", hash.length());

        if ("Unknown".equals(type)) {
            result.put("message", "❌ Hash si sahihi — inapaswa kuwa MD5, SHA-1, SHA-256, au SHA-512");
        } else {
            result.put("message", "✅ Hash ni sahihi (" + type + ")");
            result.put("virustotal", "https://www.virustotal.com/gui/file/" + hash);
        }
        return result;
    }

    // ===== 7. PASSWORD BREACH CHECK (HaveIBeenPwned k-anonymity) =====
    public Map<String, Object> checkPasswordBreach(String password) {
        Map<String, Object> result = new HashMap<>();
        try {
            // SHA-1 hash ya password
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = digest.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            String hash = sb.toString().toUpperCase();

            String prefix = hash.substring(0, 5);
            String suffix = hash.substring(5);

            // Query HIBP API k-anonymity
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HIBP_API + prefix))
                    .header("User-Agent", "CyberForensicsTZ")
                    .GET()
                    .build();

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
                        ? "🚨 Password imevuja " + count + " mara kwenye data breaches! Badilisha mara moja!"
                        : "✅ Password haijavuja kwenye data breaches zinazojulikana");
            } else {
                result.put("success", false);
                result.put("message", "⚠️ API haijarudisha data (status: " + response.statusCode() + ")");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ===== 8. URL REPUTATION (Enhanced) =====
    public Map<String, Object> checkUrlReputation(String url) {
        Map<String, Object> result = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        List<String> safe = new ArrayList<>();
        int riskScore = 0;

        try {
            String original = url;
            if (!url.startsWith("http")) url = "https://" + url;

            // Parse URL
            URI uri = URI.create(url);
            String host = uri.getHost();
            String path = uri.getPath();
            String query = uri.getQuery();

            result.put("url", original);
            result.put("host", host);
            result.put("path", path);

            // ===== CHECK 1: Protocol =====
            if (original.startsWith("http://")) {
                riskScore += 30;
                warnings.add("🔴 HTTP (not HTTPS) — encryption haipo");
            } else {
                safe.add("✅ HTTPS inatumika");
            }

            // ===== CHECK 2: IP instead of domain =====
            if (host != null && host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                riskScore += 40;
                warnings.add("🔴 Inatumia IP address badala ya domain");
            }

            // ===== CHECK 3: Suspicious TLDs =====
            String[] riskyTlds = {".tk", ".ml", ".ga", ".cf", ".gq", ".top", ".work", ".click", ".zip", ".mov", ".rest", ".fit", ".beauty"};
            if (host != null) {
                for (String tld : riskyTlds) {
                    if (host.toLowerCase().endsWith(tld)) {
                        riskScore += 30;
                        warnings.add("🔴 TLD ya kutiliwa shaka: " + tld);
                        break;
                    }
                }
            }

            // ===== CHECK 4: Too many subdomains =====
            if (host != null && host.split("\\.").length > 4) {
                riskScore += 20;
                warnings.add("🟠 Subdomains nyingi sana");
            }

            // ===== CHECK 5: URL length =====
            if (url.length() > 150) {
                riskScore += 15;
                warnings.add("🟠 URL ni ndefu sana (" + url.length() + " chars)");
            }

            // ===== CHECK 6: Phishing keywords =====
            String lower = url.toLowerCase();
            String[] keywords = {"login", "verify", "account", "update", "secure", "bank",
                                 "paypal", "signin", "confirm", "password", "wallet", "crypto"};
            int keywordCount = 0;
            for (String kw : keywords) {
                if (lower.contains(kw)) keywordCount++;
            }
            if (keywordCount >= 2) {
                riskScore += 25;
                warnings.add("🟠 Maneno " + keywordCount + " ya phishing yamepatikana");
            }

            // ===== CHECK 7: @ in URL =====
            if (lower.contains("@") && !lower.startsWith("mailto:")) {
                riskScore += 35;
                warnings.add("🔴 Ina alama @ — inaweza kufanya redirect");
            }

            // ===== CHECK 8: Long path =====
            if (path != null && path.length() > 100) {
                riskScore += 15;
                warnings.add("🟠 Path ni ndefu sana");
            }

            // ===== CHECK 9: Many query params =====
            if (query != null && query.split("&").length > 5) {
                riskScore += 15;
                warnings.add("🟠 Query parameters nyingi");
            }

            // ===== CHECK 10: DNS Resolution =====
            try {
                InetAddress addr = InetAddress.getByName(host);
                safe.add("✅ Domain inatafsiriwa (IP: " + addr.getHostAddress() + ")");
                result.put("resolvedIP", addr.getHostAddress());
            } catch (Exception e) {
                riskScore += 20;
                warnings.add("🟠 Domain haiwezi kutafsiriwa (DNS)");
            }

            riskScore = Math.min(100, riskScore);

            String verdict;
            String emoji;
            if (riskScore >= 60) { verdict = "PHISHING/MALWARE"; emoji = "🚨"; }
            else if (riskScore >= 30) { verdict = "SUSPICIOUS"; emoji = "⚠️"; }
            else { verdict = "SAFE"; emoji = "✅"; }

            result.put("riskScore", riskScore);
            result.put("verdict", verdict);
            result.put("emoji", emoji);
            result.put("warnings", warnings);
            result.put("safe", safe);

            // Add external check links
            result.put("virustotalLink", "https://www.virustotal.com/gui/url/" + Base64.getUrlEncoder().withoutPadding().encodeToString(url.getBytes()));
            result.put("urlscanLink", "https://urlscan.io/");

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("riskScore", 100);
            result.put("verdict", "INVALID URL");
            result.put("emoji", "❌");
        }
        return result;
    }
}

// ===== EXTENDED: AI-POWERED CHECKS (appended) =====

