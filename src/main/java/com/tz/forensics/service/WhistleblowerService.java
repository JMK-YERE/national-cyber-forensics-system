package com.tz.forensics.service;

import com.tz.forensics.entity.WhistleblowerMessage;
import com.tz.forensics.entity.WhistleblowerReport;
import com.tz.forensics.repository.WhistleblowerMessageRepository;
import com.tz.forensics.repository.WhistleblowerReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Locale;

@Service
public class WhistleblowerService {

    private final WhistleblowerReportRepository reportRepo;
    private final WhistleblowerMessageRepository messageRepo;

    public WhistleblowerService(WhistleblowerReportRepository reportRepo,
                                 WhistleblowerMessageRepository messageRepo) {
        this.reportRepo = reportRepo;
        this.messageRepo = messageRepo;
    }

    @Transactional
    public WhistleblowerReport createReport(WhistleblowerReport report) {
        if (report == null) throw new IllegalArgumentException("Report is required.");
        requireLength(report.getCategory(), "Category", 100);
        requireLength(report.getUrgency(), "Urgency", 30);
        requireLength(report.getTitle(), "Title", 200);
        requireLength(report.getDescription(), "Description", 20000);
        requireLength(report.getInvolvedParties(), "Involved parties", 5000);
        requireLength(report.getLocation(), "Location", 500);
        requireLength(report.getRegion(), "Region", 100);
        requireLength(report.getEvidence(), "Evidence description", 10000);
        requireLength(report.getCountry(), "Country", 10);
        requireLength(report.getCountryName(), "Country name", 100);
        if (report.getTitle() == null || report.getTitle().isBlank()) {
            throw new IllegalArgumentException("Report title is required.");
        }
        if (report.getDescription() == null || report.getDescription().isBlank()) {
            throw new IllegalArgumentException("Report description is required.");
        }
        report.setTrackingCode(generateTrackingCode());
        report.setCreatedAt(LocalDateTime.now());
        report.setStatus("NEW");
        report.setPriority(mapUrgency(report.getUrgency()));
        WhistleblowerReport saved = reportRepo.save(report);

        messageRepo.save(new WhistleblowerMessage(
            saved.getId(), "SYSTEM",
            "✅ Taarifa yako imepokelewa kwa usalama. Tracking code: " + saved.getTrackingCode()
        ));
        return saved;
    }

    public WhistleblowerReport getById(Long id) {
        return reportRepo.findById(id).orElse(null);
    }

    public WhistleblowerReport findByTrackingCode(String code) {
        if (code == null || code.isBlank()) return null;
        return reportRepo.findByTrackingCode(code.trim().toUpperCase(Locale.ROOT)).orElse(null);
    }

    public List<WhistleblowerReport> getAll() {
        return reportRepo.findAllByOrderByCreatedAtDesc();
    }

    public List<WhistleblowerReport> getAssignedTo(Long userId) {
        if (userId == null) return List.of();
        return reportRepo.findByAssignedToOrderByCreatedAtDesc(userId);
    }

    public List<WhistleblowerMessage> getMessages(Long reportId) {
        return messageRepo.findByReportIdOrderByCreatedAtAsc(reportId);
    }

    @Transactional
    public void addMessage(Long reportId, String senderType, String message) {
        if (reportId == null) throw new IllegalArgumentException("Report is required.");
        if (reportRepo.findById(reportId).isEmpty()) throw new IllegalArgumentException("Report not found.");
        String normalizedSender = senderType == null ? "" : senderType.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("SYSTEM", "ADMIN", "REPORTER").contains(normalizedSender)) {
            throw new IllegalArgumentException("Unsupported message sender type.");
        }
        requireLength(senderType, "Sender type", 30);
        requireLength(message, "Message", 4000);
        if (message == null || message.isBlank()) throw new IllegalArgumentException("Message is required.");
        messageRepo.save(new WhistleblowerMessage(reportId, normalizedSender, message.trim()));
    }

    @Transactional
    public void updateStatus(Long id, String status, String adminResponse, String internalNotes) {
        WhistleblowerReport r = reportRepo.findById(id).orElse(null);
        if (r != null) {
            String normalized = status == null ? "" : status.trim().toUpperCase();
            Set<String> allowed = Set.of("NEW", "UNDER_REVIEW", "INVESTIGATING", "RESOLVED", "REJECTED", "CLOSED");
            if (!allowed.contains(normalized)) {
                throw new IllegalArgumentException("Unsupported whistleblower status: " + status);
            }
            String from = r.getStatus() == null ? "NEW" : r.getStatus().trim().toUpperCase();
            if ("CLOSED".equals(from)) {
                throw new IllegalStateException("Closed whistleblower reports cannot be reopened.");
            }
            boolean validTransition = switch (from) {
                case "NEW" -> "UNDER_REVIEW".equals(normalized) || "REJECTED".equals(normalized);
                case "UNDER_REVIEW" -> "INVESTIGATING".equals(normalized) || "REJECTED".equals(normalized);
                case "INVESTIGATING" -> "RESOLVED".equals(normalized) || "REJECTED".equals(normalized);
                case "RESOLVED", "REJECTED" -> "CLOSED".equals(normalized);
                default -> false;
            };
            if (!validTransition) {
                throw new IllegalStateException("Invalid whistleblower workflow transition: " + from + " -> " + normalized);
            }
            r.setStatus(normalized);
            if (adminResponse != null && !adminResponse.isBlank()) {
                requireLength(adminResponse, "Admin response", 5000);
                r.setAdminResponse(adminResponse.trim());
                messageRepo.save(new WhistleblowerMessage(id, "ADMIN", adminResponse.trim()));
            }
            if (internalNotes != null) {
                requireLength(internalNotes, "Internal notes", 10000);
                r.setInternalNotes(internalNotes.trim());
            }
            r.setUpdatedAt(LocalDateTime.now());
            reportRepo.save(r);
        }
    }

    public long countNew() { return reportRepo.countByStatus("NEW"); }
    public long countNewAssignedTo(Long userId) {
        return userId == null ? 0L : reportRepo.countByAssignedToAndStatus(userId, "NEW");
    }
    public long countToday() {
        return reportRepo.countByCreatedAtAfter(LocalDateTime.now().withHour(0).withMinute(0));
    }
    public long countTodayAssignedTo(Long userId) {
        return userId == null ? 0L : reportRepo.countByAssignedToAndCreatedAtAfter(userId, LocalDateTime.now().withHour(0).withMinute(0));
    }

    private void requireLength(String value, String field, int max) {
        if (value != null && value.length() > max) {
            throw new IllegalArgumentException(field + " exceeds " + max + " characters.");
        }
    }

    private String generateTrackingCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom();
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder sb = new StringBuilder("WB-");
            for (int i = 0; i < 8; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            String candidate = sb.toString();
            if (!reportRepo.existsByTrackingCode(candidate)) return candidate;
        }
        throw new IllegalStateException("Unable to generate a unique whistleblower tracking code.");
    }

    private String mapUrgency(String urgency) {
        if (urgency == null) return "MEDIUM";
        return switch (urgency.toUpperCase()) {
            case "CRITICAL" -> "CRITICAL";
            case "HIGH" -> "HIGH";
            case "LOW" -> "LOW";
            default -> "MEDIUM";
        };
    }
}
