package com.tz.forensics.service;

import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.repository.ReportAttackRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.security.SecureRandom;

@Service
public class ReportAttackService {

    private final ReportAttackRepository repo;
    private final SecureRandom secureRandom = new SecureRandom();

    public ReportAttackService(ReportAttackRepository repo) {
        this.repo = repo;
    }

    public ReportAttack create(ReportAttack report) {
        if (report.getReportId() == null) {
            report.setReportId(generateReportId());
        }
        if (report.getCreatedAt() == null) {
            report.setCreatedAt(LocalDateTime.now());
        }
        return repo.save(report);
    }

    public ReportAttack save(ReportAttack report) {
        return repo.save(report);
    }

    public ReportAttack getById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public List<ReportAttack> getAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public List<ReportAttack> getMine(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<ReportAttack> getAssignedTo(Long userId) {
        if (userId == null) return List.of();
        return repo.findByAssignedToOrderByCreatedAtDesc(userId);
    }

    public long countNew() {
        return repo.countByStatus("NEW");
    }

    public long countToday() {
        return repo.countByCreatedAtAfter(LocalDateTime.now().withHour(0).withMinute(0));
    }

    public long countTotal() {
        return repo.count();
    }

    public void updateStatus(Long id, String status, String adminResponse, Long assignedTo, String assignedName) {
        ReportAttack r = repo.findById(id).orElse(null);
        if (r != null) {
            String normalizedStatus = status == null ? null : status.trim().toUpperCase();
            if (normalizedStatus == null || normalizedStatus.isBlank()) {
                throw new IllegalArgumentException("Report status is required.");
            }
            Set<String> allowed = Set.of("NEW", "UNDER_REVIEW", "INVESTIGATING", "RESOLVED", "REJECTED", "CLOSED");
            if (!allowed.contains(normalizedStatus)) {
                throw new IllegalArgumentException("Unsupported report status: " + status);
            }
            if ("CLOSED".equalsIgnoreCase(r.getStatus()) && !"CLOSED".equals(normalizedStatus)) {
                throw new IllegalStateException("Closed reports cannot be reopened.");
            }
            r.setStatus(normalizedStatus);
            if (adminResponse != null && !adminResponse.isEmpty()) {
                r.setAdminResponse(adminResponse);
            }
            if (assignedTo != null) {
                r.setAssignedTo(assignedTo);
                r.setAssignedToName(assignedName);
            }
            r.setUpdatedAt(LocalDateTime.now());
            repo.save(r);
        }
    }

    private String generateReportId() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int i = 0; i < 20; i++) {
            int rand = 1000 + secureRandom.nextInt(9000);
            String candidate = "SEC-" + date + "-" + rand;
            if (!repo.existsByReportId(candidate)) return candidate;
        }
        return "SEC-" + date + "-" + secureRandom.nextInt(900000);
    }
}
