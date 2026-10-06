package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.ReportAttackRepository;
import com.tz.forensics.repository.UserRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class DownloadsController {

    private final UserRepository userRepository;
    private final ReportAttackRepository reportRepository;

    public DownloadsController(UserRepository userRepository, ReportAttackRepository reportRepository) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
    }

    @GetMapping("/downloads")
    public String downloads() {
        return "downloads";
    }

    @GetMapping("/downloads/reports.csv")
    public ResponseEntity<ByteArrayResource> myReportsCsv(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<com.tz.forensics.entity.ReportAttack> reports;
        if (user.isAdmin() || user.isProfessional() || user.isForensics()) {
            reports = reportRepository.findAllByOrderByCreatedAtDesc();
        } else if (user.isAnalyst()) {
            reports = reportRepository.findByAssignedToOrderByCreatedAtDesc(user.getId());
        } else {
            reports = reportRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        }

        StringBuilder csv = new StringBuilder("Report ID,Title,Attack Type,Status,Priority,Region,Created At\n");
        for (var r : reports) {
            csv.append(csv(r.getReportId())).append(',')
               .append(csv(r.getTitle())).append(',')
               .append(csv(r.getAttackTypeLabel())).append(',')
               .append(csv(r.getStatus())).append(',')
               .append(csv(r.getPriority())).append(',')
               .append(csv(r.getRegion())).append(',')
               .append(csv(String.valueOf(r.getCreatedAt()))).append('\n');
        }

        ByteArrayResource resource = new ByteArrayResource(csv.toString().getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cyber-reports.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    private String csv(String value) {
        if (value == null) return "\"\"";
        String safe = value;
        if (!safe.isEmpty()) {
            char first = safe.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@') {
                safe = "'" + safe;
            }
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}