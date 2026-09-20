package com.tz.forensics.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class URLScanService {

    private static final String URLSCAN_API = "https://urlscan.io/api/v1";

    public String searchDomain(String domain) {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URLSCAN_API + "/search/?q=domain:" + domain))
                    .header("User-Agent", "CyberForensicsTZ")
                    .timeout(Duration.ofSeconds(20))
                    .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}
