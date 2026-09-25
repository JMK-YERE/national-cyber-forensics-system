package com.tz.forensics.service;

import com.tz.forensics.entity.WhistleblowerMessage;
import com.tz.forensics.entity.WhistleblowerReport;
import com.tz.forensics.repository.WhistleblowerMessageRepository;
import com.tz.forensics.repository.WhistleblowerReportRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WhistleblowerService {

    private final WhistleblowerReportRepository reportRepo;
    private final WhistleblowerMessageRepository messageRepo;

    public WhistleblowerService(WhistleblowerReportRepository reportRepo,
                                 WhistleblowerMessageRepository messageRepo) {
        this.reportRepo = reportRepo;
        this.messageRepo = messageRepo;
    }

    public WhistleblowerReport createReport(WhistleblowerReport report) {
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
        return reportRepo.findByTrackingCode(code.toUpperCase().trim()).orElse(null);
    }

    public List<WhistleblowerReport> getAll() {
        return reportRepo.findAllByOrderByCreatedAtDesc();
    }

    public List<WhistleblowerMessage> getMessages(Long reportId) {
        return messageRepo.findByReportIdOrderByCreatedAtAsc(reportId);
    }

    public void addMessage(Long reportId, String senderType, String message) {
        messageRepo.save(new WhistleblowerMessage(reportId, senderType, message));
    }

    public void updateStatus(Long id, String status, String adminResponse, String internalNotes) {
        WhistleblowerReport r = reportRepo.findById(id).orElse(null);
        if (r != null) {
            r.setStatus(status);
            if (adminResponse != null && !adminResponse.isEmpty()) {
                r.setAdminResponse(adminResponse);
                messageRepo.save(new WhistleblowerMessage(id, "ADMIN", adminResponse));
            }
            if (internalNotes != null) r.setInternalNotes(internalNotes);
            r.setUpdatedAt(LocalDateTime.now());
            reportRepo.save(r);
        }
    }

    public long countNew() { return reportRepo.countByStatus("NEW"); }
    public long countToday() {
        return reportRepo.countByCreatedAtAfter(LocalDateTime.now().withHour(0).withMinute(0));
    }

    private String generateTrackingCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder("WB-");
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
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
