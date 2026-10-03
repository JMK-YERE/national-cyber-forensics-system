package com.tz.forensics.controller;

import com.tz.forensics.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final UserRepository userRepository;

    public HealthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "Cyber Forensics TZ");
        body.put("timestamp", Instant.now().toString());
        try {
            userRepository.count();
            body.put("status", "UP");
            body.put("database", "UP");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("status", "DEGRADED");
            body.put("database", "DOWN");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
    }
}
