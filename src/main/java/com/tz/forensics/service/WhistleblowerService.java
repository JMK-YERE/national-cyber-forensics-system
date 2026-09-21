package com.tz.forensics.service;

import com.tz.forensics.entity.WhistleblowerMessage;
import com.tz.forensics.entity.WhistleblowerReport;
import com.tz.forensics.repository.WhistleblowerMessageRepository;
import com.tz.forensics.repository.WhistleblowerRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class WhistleblowerService {

    private final WhistleblowerRepository reportRepo;
    private final WhistleblowerMessageRepository messageRepo;

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom random = new SecureRandom();

    public WhistleblowerService(WhistleblowerRepository reportRepo,
                                 WhistleblowerMessageRepository messageRepo) {
        this.reportRepo = reportRepo;
        this.messageRepo = messageRepo;
    }

    public WhistleblowerReport createReport(WhistleblowerReport report) {
        String code = generateTrackingCode();
        report.setTrackingCode(code);
        report.setStatus("NEW");
        report.setPriority(determinePriority(report.getCategory(), report.getUrgency()));
        return reportRepo.save(report);
    }

    public WhistleblowerReport findByTrackingCode(String code) {
        return reportRepo.findByTrackingCode(code.toUpperCase()).orElse(null);
    }

    public List<WhistleblowerReport> getAll() {
        return reportRepo.findAllByOrderByCreatedAtDesc();
    }

    public WhistleblowerReport getById(Long id) {
        return reportRepo.findById(id).orElse(null);
    }

    public void updateStatus(Long id, String status, String response, String internalNotes) {
        WhistleblowerReport r = reportRepo.findById(id).orElse(null);
        if (r != null) {
            r.setStatus(status);
            if (response != null && !response.isEmpty()) r.setAdminResponse(response);
            if (internalNotes != null && !internalNotes.isEmpty()) r.setInternalNotes(internalNotes);
            r.setUpdatedAt(LocalDateTime.now());
            reportRepo.save(r);
        }
    }

    public void addMessage(Long reportId, String senderType, String message) {
        messageRepo.save(new WhistleblowerMessage(reportId, senderType, message));
    }

    public List<WhistleblowerMessage> getMessages(Long reportId) {
        return messageRepo.findByReportIdOrderByCreatedAtAsc(reportId);
    }

    public long countNew() { return reportRepo.countByStatus("NEW"); }

    private String generateTrackingCode() {
        StringBuilder sb = new StringBuilder("WB-");
        for (int i = 0; i < 8; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        // Ensure uniqueness
        if (reportRepo.findByTrackingCode(sb.toString()).isPresent()) {
            return generateTrackingCode();
        }
        return sb.toString();
    }

    private String determinePriority(String category, String urgency) {
        if ("CRITICAL".equals(urgency)) return "CRITICAL";
        if ("URGENT".equals(urgency)) return "HIGH";
        if (category != null) {
            return switch (category) {
                case "HUMAN_TRAFFICKING", "TERRORISM", "DRUGS" -> "CRITICAL";
                case "CORRUPTION", "MONEY_LAUNDERING", "CYBER_ATTACK" -> "HIGH";
                default -> "MEDIUM";
            };
        }
        return "MEDIUM";
    }
}
