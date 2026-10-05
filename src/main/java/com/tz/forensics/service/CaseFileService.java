package com.tz.forensics.service;

import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.repository.CaseFileRepository;
import com.tz.forensics.repository.CaseTimelineRepository;
import com.tz.forensics.entity.CaseTimeline;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class CaseFileService {

    private final CaseFileRepository caseFileRepository;
    private final CaseTimelineRepository timelineRepository;

    public CaseFileService(CaseFileRepository caseFileRepository, CaseTimelineRepository timelineRepository) {
        this.caseFileRepository = caseFileRepository;
        this.timelineRepository = timelineRepository;
    }

    public List<CaseTimeline> getTimeline(Long caseId) {
        return timelineRepository.findByCaseIdOrderByCreatedAtDesc(caseId);
    }

    public void addTimeline(Long caseId, String eventType, String title, String details,
                            Long actorId, String actorName, String actorRole) {
        timelineRepository.save(new CaseTimeline(caseId, eventType, title, details, actorId, actorName, actorRole));
    }

    public CaseFile createCase(Long incidentId, String title, String description,
                                String priority, Long createdBy, String createdByName) {
        CaseFile caseFile = new CaseFile();
        String caseNumber = "CASE-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        caseFile.setCaseNumber(caseNumber);
        caseFile.setIncidentId(incidentId);
        caseFile.setTitle(title);
        caseFile.setDescription(description);
        caseFile.setPriority(priority != null ? priority : "MEDIUM");
        caseFile.setStatus("OPEN");
        caseFile.setCreatedBy(createdBy);
        caseFile.setCreatedByName(createdByName);
        CaseFile saved = caseFileRepository.save(caseFile);
        addTimeline(saved.getId(), "CASE_CREATED", "Forensic case opened",
                "Case created from incident " + incidentId, createdBy, createdByName, "CASE_CREATOR");
        return saved;
    }

    public List<CaseFile> getAllCases() {
        return caseFileRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<CaseFile> getCasesByStatus(String status) {
        return caseFileRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<CaseFile> getMyCases(Long userId) {
        return caseFileRepository.findByAssignedToOrderByCreatedAtDesc(userId);
    }

    public List<CaseFile> getCasesByIncident(Long incidentId) {
        if (incidentId == null) return List.of();
        return caseFileRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId);
    }

    public CaseFile getById(Long id) {
        return caseFileRepository.findById(id).orElse(null);
    }

    public boolean updateStatus(Long caseId, String status, String closureReason) {
        CaseFile cf = caseFileRepository.findById(caseId).orElse(null);
        if (cf == null || status == null) return false;

        String from = cf.getStatus() == null ? "OPEN" : cf.getStatus().trim().toUpperCase();
        String to = status.trim().toUpperCase();

        // Keep the case lifecycle deterministic; callers cannot jump backwards
        // or reopen a closed/archived forensic case through a crafted request.
        boolean valid = switch (from) {
            case "OPEN" -> "INVESTIGATING".equals(to) || "CLOSED".equals(to);
            case "INVESTIGATING" -> "CLOSED".equals(to);
            case "CLOSED" -> "ARCHIVED".equals(to);
            case "ARCHIVED" -> false;
            default -> false;
        };
        if (!valid) return false;

        cf.setStatus(to);
        cf.setUpdatedAt(LocalDateTime.now());
        if ("CLOSED".equals(to)) {
            cf.setClosedAt(LocalDateTime.now());
            cf.setClosureReason(closureReason);
        }
        caseFileRepository.save(cf);
        addTimeline(cf.getId(), "STATUS_CHANGED", "Case status changed",
                from + " → " + to + (closureReason != null && !closureReason.isBlank() ? " | " + closureReason : ""),
                null, "System", "WORKFLOW");
        return true;
    }

    public long countOpen() { return caseFileRepository.countByStatus("OPEN"); }
    public long countInvestigating() { return caseFileRepository.countByStatus("INVESTIGATING"); }
    public long countClosed() { return caseFileRepository.countByStatus("CLOSED"); }
}
