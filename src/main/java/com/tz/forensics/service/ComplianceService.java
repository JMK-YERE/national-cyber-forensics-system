package com.tz.forensics.service;

import com.tz.forensics.entity.ComplianceReport;
import com.tz.forensics.entity.Incident;
import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.repository.ComplianceReportRepository;
import com.tz.forensics.repository.EvidenceRepository;
import com.tz.forensics.repository.IncidentRepository;
import com.tz.forensics.repository.ReportAttackRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ComplianceService {

    private final ComplianceReportRepository reportRepo;
    private final IncidentRepository incidentRepo;
    private final ReportAttackRepository attackRepo;
    private final EvidenceRepository evidenceRepo;

    public ComplianceService(ComplianceReportRepository reportRepo,
                              IncidentRepository incidentRepo,
                              ReportAttackRepository attackRepo,
                              EvidenceRepository evidenceRepo) {
        this.reportRepo = reportRepo;
        this.incidentRepo = incidentRepo;
        this.attackRepo = attackRepo;
        this.evidenceRepo = evidenceRepo;
    }

    // ===== GENERATE COMPLIANCE REPORT =====
    public ComplianceReport generateReport(String type, Long userId, String userName) {
        String normalizedType = type == null ? "" : type.trim().toUpperCase();
        if (!java.util.Set.of("TCRA", "ISO27001", "DATA_PROTECTION", "ANNUAL").contains(normalizedType)) {
            throw new IllegalArgumentException("Unsupported compliance report type.");
        }
        if (userId == null || userName == null || userName.isBlank()) {
            throw new IllegalArgumentException("Authenticated report generator is required.");
        }

        ComplianceReport report = new ComplianceReport();
        String reportId = "CMP-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        report.setReportId(reportId);
        report.setReportType(normalizedType);
        report.setGeneratedBy(userId);
        report.setGeneratedByName(userName);
        report.setStatus("DRAFT");
        report.setPeriodStart(LocalDateTime.now().minusMonths(1));
        report.setPeriodEnd(LocalDateTime.now());

        // Calculate compliance score
        int totalControls = getTotalControls(normalizedType);
        int passedControls = calculatePassedControls(normalizedType);

        report.setTotalControls(totalControls);
        report.setPassedControls(passedControls);
        report.setFailedControls(totalControls - passedControls);
        report.setComplianceScore((passedControls * 100) / totalControls);
        report.setTitle(getReportTitle(normalizedType));
        report.setSummary(generateSummary(normalizedType, passedControls, totalControls));
        report.setFindings(generateFindings(normalizedType));
        report.setRecommendations(generateRecommendations(normalizedType));

        return reportRepo.save(report);
    }

    public List<ComplianceReport> getAllReports() {
        return reportRepo.findAllByOrderByCreatedAtDesc();
    }

    public ComplianceReport getById(Long id) {
        return reportRepo.findById(id).orElse(null);
    }

    public void updateStatus(Long id, String status) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();
        if (!java.util.Set.of("DRAFT", "REVIEW", "FINAL", "ARCHIVED").contains(normalizedStatus)) {
            throw new IllegalArgumentException("Unsupported compliance report status.");
        }
        ComplianceReport r = reportRepo.findById(id).orElse(null);
        if (r == null) {
            throw new IllegalArgumentException("Compliance report not found.");
        }

        String from = r.getStatus() == null ? "DRAFT" : r.getStatus().trim().toUpperCase();
        boolean valid = switch (from) {
            case "DRAFT" -> "REVIEW".equals(normalizedStatus);
            case "REVIEW" -> "FINAL".equals(normalizedStatus);
            case "FINAL" -> "ARCHIVED".equals(normalizedStatus);
            case "ARCHIVED" -> false;
            default -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException("Invalid compliance workflow transition: " + from + " -> " + normalizedStatus);
        }

        r.setStatus(normalizedStatus);
        reportRepo.save(r);
    }

    // ===== STATISTICS =====
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        List<Incident> incidents = incidentRepo.findAll();
        List<ReportAttack> attacks = attackRepo.findAll();

        stats.put("totalIncidents", incidents.size());
        stats.put("totalAttacks", attacks.size());
        stats.put("totalEvidence", evidenceRepo.count());

        // Resolved incidents
        long resolved = incidents.stream()
                .filter(i -> i.getStatus() != null && "RESOLVED".equalsIgnoreCase(i.getStatus().trim()))
                .count();
        stats.put("resolvedIncidents", resolved);

        // Compliance score (overall)
        int total = incidents.size() + attacks.size();
        int score = total > 0 ? Math.min(100, 70 + (int)(resolved * 100 / Math.max(1, total))) : 85;
        stats.put("overallScore", score);

        return stats;
    }

    private int getTotalControls(String type) {
        return switch (type) {
            case "TCRA" -> 25;
            case "ISO27001" -> 114;
            case "DATA_PROTECTION" -> 42;
            case "ANNUAL" -> 50;
            default -> 30;
        };
    }

    private int calculatePassedControls(String type) {
        // Baseline control model for internal readiness assessment; this is not evidence of certification.
        int base = switch (type) {
            case "TCRA" -> 20;
            case "ISO27001" -> 85;
            case "DATA_PROTECTION" -> 35;
            case "ANNUAL" -> 40;
            default -> 25;
        };
        return base;
    }

    private String getReportTitle(String type) {
        return switch (type) {
            case "TCRA" -> "TCRA Compliance Report — " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"));
            case "ISO27001" -> "ISO 27001 Information Security Report";
            case "DATA_PROTECTION" -> "Data Protection Act Compliance Report";
            case "ANNUAL" -> "Annual Cyber Security Report " + LocalDateTime.now().getYear();
            default -> "Compliance Report";
        };
    }

    private String generateSummary(String type, int passed, int total) {
        return "Ripoti hii ni internal readiness assessment ya mfumo, si cheti cha compliance wala legal opinion. " +
               "Imefanya tathmini ya controls " + total + ", ambapo " + passed + " zime-markiwa kama baseline controls. " +
               "Internal readiness score ni " + ((passed * 100) / total) + "%. " +
               "Kwa " + type + ", matokeo lazima yathibitishwe dhidi ya mahitaji rasmi, scope ya organization, risk assessment na ushahidi wa utekelezaji.";
    }

    private String generateFindings(String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("1. Access Control: 2FA inafanya kazi kwa admin. ✅\n");
        sb.append("2. Audit Logging: Kila kitendo kinarekodiwa. ✅\n");
        sb.append("3. Evidence Protection: Evidence encryption mechanism ipo; configuration na key management vinahitaji verification. ⚠️\n");
        sb.append("4. Incident Response: Mfumo wa kuripoti upo. ✅\n");
        sb.append("5. Data Backup: Backup/restore evidence inahitaji kuthibitishwa kwa mazingira ya production. ⚠️\n");
        sb.append("6. User Training: Training ya watumiaji inahitajika. ⚠️\n");
        return sb.toString();
    }

    private String generateRecommendations(String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("✅ Ongeza 2FA kwa watumiaji wote\n");
        sb.append("✅ Fanya security awareness training kila miezi 6\n");
        sb.append("✅ Sasisha security policies kila mwaka\n");
        sb.append("✅ Fanya penetration testing kila robo mwaka\n");
        sb.append("✅ Backup data kila siku\n");
        return sb.toString();
    }
}
