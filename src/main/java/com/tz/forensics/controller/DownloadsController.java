package com.tz.forensics.controller;

import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.ReportAttackRepository;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AuditService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class DownloadsController {

    private final UserRepository userRepository;
    private final ReportAttackRepository reportRepository;
    private final AuditService auditService;

    public DownloadsController(UserRepository userRepository,
                               ReportAttackRepository reportRepository,
                               AuditService auditService) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.auditService = auditService;
    }

    @GetMapping("/downloads")
    public String downloads() {
        return "downloads";
    }

    @GetMapping("/downloads/reports.csv")
    public ResponseEntity<ByteArrayResource> myReportsCsv(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        /*
         * Fail closed until ReportAttack has an approved tenant-ownership mapping.
         * A privileged global role is not proof of tenant membership. Export only
         * reports created by or explicitly assigned to this authenticated user.
         */
        Map<Long, ReportAttack> visible = new LinkedHashMap<>();
        reportRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .forEach(report -> visible.put(report.getId(), report));
        reportRepository.findByAssignedToOrderByCreatedAtDesc(user.getId())
                .forEach(report -> visible.put(report.getId(), report));

        List<ReportAttack> reports = visible.values().stream()
                .sorted(Comparator.comparing(ReportAttack::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        StringBuilder csv = new StringBuilder("Report ID,Title,Attack Type,Status,Priority,Region,Created At\n");
        for (ReportAttack report : reports) {
            csv.append(csv(report.getReportId())).append(',')
               .append(csv(report.getTitle())).append(',')
               .append(csv(report.getAttackTypeLabel())).append(',')
               .append(csv(report.getStatus())).append(',')
               .append(csv(report.getPriority())).append(',')
               .append(csv(report.getRegion())).append(',')
               .append(csv(String.valueOf(report.getCreatedAt()))).append('\n');
        }

        auditService.log("EXPORT_REPORTS_CSV", "ReportAttack", "user:" + user.getId(),
                "CSV export contained " + reports.size() + " owned/assigned reports");

        ByteArrayResource resource = new ByteArrayResource(csv.toString().getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cyber-reports.csv\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(resource.contentLength())
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