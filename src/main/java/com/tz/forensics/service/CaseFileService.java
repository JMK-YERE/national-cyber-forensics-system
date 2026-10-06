package com.tz.forensics.service;

import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.repository.CaseFileRepository;
import com.tz.forensics.repository.CaseTimelineRepository;
import com.tz.forensics.repository.CaseTaskRepository;
import com.tz.forensics.repository.EvidenceRepository;
import com.tz.forensics.entity.CaseTask;
import com.tz.forensics.entity.Evidence;
import com.tz.forensics.entity.CaseTimeline;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CaseFileService {

    private final CaseFileRepository caseFileRepository;
    private final CaseTimelineRepository timelineRepository;
    private final CaseTaskRepository caseTaskRepository;
    private final EvidenceRepository evidenceRepository;

    public CaseFileService(CaseFileRepository caseFileRepository, CaseTimelineRepository timelineRepository,
                          CaseTaskRepository caseTaskRepository, EvidenceRepository evidenceRepository) {
        this.caseFileRepository = caseFileRepository;
        this.timelineRepository = timelineRepository;
        this.caseTaskRepository = caseTaskRepository;
        this.evidenceRepository = evidenceRepository;
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

    /**
     * Atomically assigns a case and, when appropriate, advances TRIAGED -> ASSIGNED.
     * Keeping the mutation and lifecycle transition in one transaction prevents a
     * partially-assigned case when the workflow transition fails.
     */
    @org.springframework.transaction.annotation.Transactional
    public boolean assignCase(Long caseId, Long investigatorId, String investigatorName,
                              Long leadInvestigatorId, String leadInvestigatorName,
                              LocalDateTime dueDate, Long actorId, String actorName, String actorRole) {
        CaseFile cf = caseFileRepository.findById(caseId).orElse(null);
        if (cf == null || investigatorId == null || leadInvestigatorId == null) return false;

        String current = cf.getStatus() == null ? "OPEN" : cf.getStatus().trim().toUpperCase();
        if ("CLOSED".equals(current) || "ARCHIVED".equals(current)) return false;
        if (!Set.of("TRIAGED", "ASSIGNED", "INVESTIGATING", "EXAMINATION", "REVIEW").contains(current)) return false;
        if (dueDate != null && dueDate.isBefore(LocalDateTime.now())) return false;

        cf.setAssignedTo(investigatorId);
        cf.setAssignedToName(investigatorName);
        cf.setLeadInvestigator(leadInvestigatorId);
        cf.setLeadInvestigatorName(leadInvestigatorName);
        cf.setDueDate(dueDate);
        cf.setUpdatedAt(LocalDateTime.now());
        caseFileRepository.save(cf);

        if ("TRIAGED".equals(current) && !updateStatus(caseId, "ASSIGNED", null, actorId, actorName, actorRole)) {
            throw new IllegalStateException("Failed TRIAGED -> ASSIGNED transition.");
        }
        return true;
    }

    public boolean updateStatus(Long caseId, String status, String closureReason,
                                Long actorId, String actorName, String actorRole) {
        CaseFile cf = caseFileRepository.findById(caseId).orElse(null);
        if (cf == null || status == null) return false;
        String from = cf.getStatus() == null ? "OPEN" : cf.getStatus().trim().toUpperCase();
        String to = status.trim().toUpperCase();

        boolean valid = switch (from) {
            case "OPEN" -> "TRIAGED".equals(to);
            case "TRIAGED" -> "ASSIGNED".equals(to);
            case "ASSIGNED" -> "INVESTIGATING".equals(to);
            case "INVESTIGATING" -> "EXAMINATION".equals(to);
            case "EXAMINATION" -> "REVIEW".equals(to);
            case "REVIEW" -> "CLOSED".equals(to);
            case "CLOSED" -> "ARCHIVED".equals(to);
            case "ARCHIVED" -> false;
            default -> false;
        };
        if (!valid) return false;
        if ("ASSIGNED".equals(to) && cf.getAssignedTo() == null) return false;
        if ("CLOSED".equals(to) && (closureReason == null || closureReason.isBlank())) return false;

        if ("CLOSED".equals(to)) {
            List<CaseTask> tasks = caseTaskRepository.findByCaseIdOrderByCreatedAtDesc(cf.getId());
            boolean hasOpenTask = tasks.stream().anyMatch(t -> {
                String taskStatus = t.getStatus() == null ? "OPEN" : t.getStatus().trim().toUpperCase();
                return !"COMPLETED".equals(taskStatus) && !"CANCELLED".equals(taskStatus);
            });
            if (hasOpenTask) return false;

            List<Evidence> evidence = evidenceRepository.findByCaseIdOrderByUploadedAtDesc(cf.getId());
            boolean hasUnresolvedEvidence = evidence.stream().anyMatch(e -> {
                String custody = e.getCustodyStatus();
                return !"EXAMINED".equalsIgnoreCase(custody) && !"REPORT_GENERATED".equalsIgnoreCase(custody);
            });
            if (hasUnresolvedEvidence) return false;
        }

        cf.setStatus(to);
        cf.setUpdatedAt(LocalDateTime.now());
        if ("CLOSED".equals(to)) {
            cf.setClosedAt(LocalDateTime.now());
            cf.setClosureReason(closureReason.trim());
        }
        caseFileRepository.save(cf);
        addTimeline(cf.getId(), "STATUS_CHANGED", "Case status changed",
                from + " → " + to + (closureReason != null && !closureReason.isBlank() ? " | " + closureReason.trim() : ""),
                actorId, actorName, actorRole);
        return true;
    }

    public boolean updateStatus(Long caseId, String status, String closureReason) {
        return updateStatus(caseId, status, closureReason, null, "System", "WORKFLOW");
    }

    public long countOpen() { return caseFileRepository.countByStatus("OPEN"); }
    public long countInvestigating() { return caseFileRepository.countByStatus("INVESTIGATING"); }
    public long countClosed() { return caseFileRepository.countByStatus("CLOSED"); }
}    @org.springframework.transaction.annotation.Transactional
    public CaseFile createCase(Long incidentId, String title, String description,
                               String priority, Long createdBy, String createdByName) {
        if (incidentId == null || createdBy == null) {
            throw new IllegalArgumentException("Incident and creator are required.");
        }
        String normalizedTitle = title == null ? "" : title.trim();
        String normalizedDescription = description == null ? "" : description.trim();
        if (normalizedTitle.length() < 3 || normalizedTitle.length() > 200) {
            throw new IllegalArgumentException("Case title must be between 3 and 200 characters.");
        }
        if (normalizedDescription.length() < 10 || normalizedDescription.length() > 10000) {
            throw new IllegalArgumentException("Case description must be between 10 and 10000 characters.");
        }
        String normalizedPriority = priority == null || priority.isBlank()
                ? "MEDIUM" : priority.trim().toUpperCase();
        if (!Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(normalizedPriority)) {
            throw new IllegalArgumentException("Unsupported case priority.");
        }
        CaseFile caseFile = new CaseFile();
        String caseNumber = "CASE-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        caseFile.setCaseNumber(caseNumber);
        caseFile.setIncidentId(incidentId);
        caseFile.setTitle(normalizedTitle);
        caseFile.setDescription(normalizedDescription);
        caseFile.setPriority(normalizedPriority);
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

    /**
     * Atomically assigns a case and, when appropriate, advances TRIAGED -> ASSIGNED.
     * Keeping the mutation and lifecycle transition in one transaction prevents a
     * partially-assigned case when the workflow transition fails.
     */
    @org.springframework.transaction.annotation.Transactional
    public boolean assignCase(Long caseId, Long investigatorId, String investigatorName,
                              Long leadInvestigatorId, String leadInvestigatorName,
                              LocalDateTime dueDate, Long actorId, String actorName, String actorRole) {
        CaseFile cf = caseFileRepository.findById(caseId).orElse(null);
        if (cf == null || investigatorId == null || leadInvestigatorId == null) return false;

        String current = cf.getStatus() == null ? "OPEN" : cf.getStatus().trim().toUpperCase();
        if ("CLOSED".equals(current) || "ARCHIVED".equals(current)) return false;
        if (!Set.of("TRIAGED", "ASSIGNED", "INVESTIGATING", "EXAMINATION", "REVIEW").contains(current)) return false;
        if (dueDate != null && dueDate.isBefore(LocalDateTime.now())) return false;

        cf.setAssignedTo(investigatorId);
        cf.setAssignedToName(investigatorName);
        cf.setLeadInvestigator(leadInvestigatorId);
        cf.setLeadInvestigatorName(leadInvestigatorName);
        cf.setDueDate(dueDate);
        cf.setUpdatedAt(LocalDateTime.now());
        caseFileRepository.save(cf);

        if ("TRIAGED".equals(current) && !updateStatus(caseId, "ASSIGNED", null, actorId, actorName, actorRole)) {
            throw new IllegalStateException("Failed TRIAGED -> ASSIGNED transition.");
        }
        return true;
    }

    public boolean updateStatus(Long caseId, String status, String closureReason,
                                Long actorId, String actorName, String actorRole) {
        CaseFile cf = caseFileRepository.findById(caseId).orElse(null);
        if (cf == null || status == null) return false;
        String from = cf.getStatus() == null ? "OPEN" : cf.getStatus().trim().toUpperCase();
        String to = status.trim().toUpperCase();

        boolean valid = switch (from) {
            case "OPEN" -> "TRIAGED".equals(to);
            case "TRIAGED" -> "ASSIGNED".equals(to);
            case "ASSIGNED" -> "INVESTIGATING".equals(to);
            case "INVESTIGATING" -> "EXAMINATION".equals(to);
            case "EXAMINATION" -> "REVIEW".equals(to);
            case "REVIEW" -> "CLOSED".equals(to);
            case "CLOSED" -> "ARCHIVED".equals(to);
            case "ARCHIVED" -> false;
            default -> false;
        };
        if (!valid) return false;
        if ("ASSIGNED".equals(to) && cf.getAssignedTo() == null) return false;
        if ("CLOSED".equals(to) && (closureReason == null || closureReason.isBlank())) return false;

        if ("CLOSED".equals(to)) {
            List<CaseTask> tasks = caseTaskRepository.findByCaseIdOrderByCreatedAtDesc(cf.getId());
            boolean hasOpenTask = tasks.stream().anyMatch(t -> {
                String taskStatus = t.getStatus() == null ? "OPEN" : t.getStatus().trim().toUpperCase();
                return !"COMPLETED".equals(taskStatus) && !"CANCELLED".equals(taskStatus);
            });
            if (hasOpenTask) return false;

            List<Evidence> evidence = evidenceRepository.findByCaseIdOrderByUploadedAtDesc(cf.getId());
            boolean hasUnresolvedEvidence = evidence.stream().anyMatch(e -> {
                String custody = e.getCustodyStatus();
                return !"EXAMINED".equalsIgnoreCase(custody) && !"REPORT_GENERATED".equalsIgnoreCase(custody);
            });
            if (hasUnresolvedEvidence) return false;
        }

        cf.setStatus(to);
        cf.setUpdatedAt(LocalDateTime.now());
        if ("CLOSED".equals(to)) {
            cf.setClosedAt(LocalDateTime.now());
            cf.setClosureReason(closureReason.trim());
        }
        caseFileRepository.save(cf);
        addTimeline(cf.getId(), "STATUS_CHANGED", "Case status changed",
                from + " → " + to + (closureReason != null && !closureReason.isBlank() ? " | " + closureReason.trim() : ""),
                actorId, actorName, actorRole);
        return true;
    }

    public boolean updateStatus(Long caseId, String status, String closureReason) {
        return updateStatus(caseId, status, closureReason, null, "System", "WORKFLOW");
    }

    public long countOpen() { return caseFileRepository.countByStatus("OPEN"); }
    public long countInvestigating() { return caseFileRepository.countByStatus("INVESTIGATING"); }
    public long countClosed() { return caseFileRepository.countByStatus("CLOSED"); }
}
