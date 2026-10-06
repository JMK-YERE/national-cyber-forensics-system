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

    @org.springframework.transaction.annotation.Transactional
    public ReportAttack create(ReportAttack report) {
        if (report == null) throw new IllegalArgumentException("Report is required.");
        String title = report.getTitle() == null ? "" : report.getTitle().trim();
        String description = report.getDescription() == null ? "" : report.getDescription().trim();
        if (title.length() < 3 || title.length() > 200) throw new IllegalArgumentException("Report title must be between 3 and 200 characters.");
        if (description.length() < 10 || description.length() > 10000) throw new IllegalArgumentException("Report description must be between 10 and 10000 characters.");
        if (report.getStatus() == null || report.getStatus().isBlank()) report.setStatus("NEW");
        report.setStatus(report.getStatus().trim().toUpperCase());
        if (!Set.of("NEW").contains(report.getStatus())) throw new IllegalArgumentException("New reports must start in NEW status.");
        if (report.getPriority() != null) {
            String priority = report.getPriority().trim().toUpperCase();
            if (!Set.of("LOW","MEDIUM","HIGH","CRITICAL").contains(priority)) throw new IllegalArgumentException("Unsupported report priority.");
            report.setPriority(priority);
        }
        if (report.getSeverity() != null) report.setSeverity(report.getSeverity().trim().toUpperCase());
        if (report.getReportId() == null) {
            report.setReportId(generateReportId());
        }
        if (report.getCreatedAt() == null) {
            report.setCreatedAt(LocalDateTime.now());
        }
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

    public long countNewAssignedTo(Long userId) {
        return userId == null ? 0L : repo.countByAssignedToAndStatus(userId, "NEW");
    }

    public long countToday() {
        return repo.countByCreatedAtAfter(LocalDateTime.now().withHour(0).withMinute(0));
    }

    public long countTodayAssignedTo(Long userId) {
        return userId == null ? 0L : repo.countByAssignedToAndCreatedAtAfter(userId, LocalDateTime.now().withHour(0).withMinute(0));
    }

    public long countTotal() {
        return repo.count();
    }

    public long countTotalAssignedTo(Long userId) {
        return userId == null ? 0L : repo.countByAssignedTo(userId);
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateStatus(Long id, String status, String adminResponse, Long assignedTo, String assignedName,
                             String policeCaseNumber, Long actorId, String actorRole) {
        ReportAttack r = repo.findById(id).orElse(null);
        if (r != null) {
            if (actorId == null || actorRole == null) {
                throw new IllegalArgumentException("Authenticated actor is required.");
            }
            String role = actorRole.trim().toUpperCase();
            boolean privileged = Set.of("ADMIN", "CYBER_PRO", "FORENSICS").contains(role);
            boolean assigned = r.getAssignedTo() != null && r.getAssignedTo().equals(actorId);
            if (!privileged && !("ANALYST".equals(role) && assigned)) {
                throw new SecurityException("User is not authorized to update this report.");
            }
            if (!privileged && assignedTo != null && !assignedTo.equals(r.getAssignedTo())) {
                throw new SecurityException("Analysts cannot reassign reports.");
            }
            String normalizedStatus = status == null ? null : status.trim().toUpperCase();
            if (normalizedStatus == null || normalizedStatus.isBlank()) {
                throw new IllegalArgumentException("Report status is required.");
            }
            Set<String> allowed = Set.of("NEW", "UNDER_REVIEW", "INVESTIGATING", "RESOLVED", "REJECTED", "CLOSED");
            if (!allowed.contains(normalizedStatus)) {
                throw new IllegalArgumentException("Unsupported report status: " + status);
            }
            String from = r.getStatus() == null ? "NEW" : r.getStatus().trim().toUpperCase();
            if ("CLOSED".equals(from)) {
                throw new IllegalStateException("Closed reports cannot be reopened.");
            }
            boolean validTransition = switch (from) {
                case "NEW" -> "UNDER_REVIEW".equals(normalizedStatus) || "REJECTED".equals(normalizedStatus);
                case "UNDER_REVIEW" -> "INVESTIGATING".equals(normalizedStatus) || "REJECTED".equals(normalizedStatus);
                case "INVESTIGATING" -> "RESOLVED".equals(normalizedStatus) || "REJECTED".equals(normalizedStatus);
                case "RESOLVED", "REJECTED" -> "CLOSED".equals(normalizedStatus);
                default -> false;
            };
            if (!validTransition) {
                throw new IllegalStateException("Invalid report workflow transition: " + from + " -> " + normalizedStatus);
            }
            r.setStatus(normalizedStatus);
            if (adminResponse != null && !adminResponse.isEmpty()) {
                String normalizedResponse = adminResponse.trim();
                if (normalizedResponse.length() > 10000) {
                    throw new IllegalArgumentException("Admin response is too long.");
                }
                r.setAdminResponse(normalizedResponse);
            }
            if (assignedTo != null) {
                String normalizedAssignedName = assignedName == null ? null : assignedName.trim();
                if (normalizedAssignedName != null && normalizedAssignedName.length() > 200) {
                    throw new IllegalArgumentException("Assigned name is too long.");
                }
                r.setAssignedTo(assignedTo);
                r.setAssignedToName(normalizedAssignedName);
            }
            if (policeCaseNumber != null && !policeCaseNumber.isBlank()) {
                if ("CLOSED".equals(from)) {
                    throw new IllegalStateException("Closed reports cannot be modified.");
                }
                String normalizedPoliceCase = policeCaseNumber.trim();
                if (normalizedPoliceCase.length() > 50) {
                    throw new IllegalArgumentException("Police case number is too long.");
                }
                r.setPoliceCaseNumber(normalizedPoliceCase);
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
        throw new IllegalStateException("Unable to generate a unique report ID.");
    }
}
