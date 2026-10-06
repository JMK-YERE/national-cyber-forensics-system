package com.tz.forensics.service;

import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.entity.CaseTask;
import com.tz.forensics.entity.Evidence;
import com.tz.forensics.entity.CaseTimeline;
import com.tz.forensics.repository.CaseFileRepository;
import com.tz.forensics.repository.CaseTimelineRepository;
import com.tz.forensics.repository.CaseTaskRepository;
import com.tz.forensics.repository.EvidenceRepository;
import com.tz.forensics.repository.IncidentRepository;
import com.tz.forensics.entity.Incident;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final IncidentRepository incidentRepository;

    public CaseFileService(CaseFileRepository caseFileRepository, CaseTimelineRepository timelineRepository,
                           CaseTaskRepository caseTaskRepository, EvidenceRepository evidenceRepository,
                           IncidentRepository incidentRepository) {
        this.caseFileRepository = caseFileRepository;
        this.timelineRepository = timelineRepository;
        this.caseTaskRepository = caseTaskRepository;
        this.evidenceRepository = evidenceRepository;
    }

    public List<CaseTimeline> getTimeline(Long caseId) {
        return caseId == null ? List.of() : timelineRepository.findByCaseIdOrderByCreatedAtDesc(caseId);
    }

    public void addTimeline(Long caseId, String eventType, String title, String details,
                            Long actorId, String actorName, String actorRole) {
        if (caseId == null) return;
        timelineRepository.save(new CaseTimeline(caseId, eventType, title, details, actorId, actorName, actorRole));
    }

    @Transactional
    public CaseFile createCase(Long incidentId, String title, String description,
                               String priority, Long createdBy, String createdByName, String creatorRole) {
        if (incidentId == null || createdBy == null) {
            throw new IllegalArgumentException("Incident and creator are required.");
        }
        Incident incident = incidentRepository.findById(incidentId).orElse(null);
        if (incident == null) throw new IllegalArgumentException("Incident not found.");
        String normalizedRole = creatorRole == null ? "" : creatorRole.trim().toUpperCase();
        if (!Set.of("ADMIN", "CYBER_PRO", "FORENSICS", "ANALYST").contains(normalizedRole)) {
            throw new IllegalArgumentException("User is not authorized to create a forensic case.");
        }
        if ("CLOSED".equalsIgnoreCase(incident.getWorkflowStatus())) {
            throw new IllegalArgumentException("Cases cannot be created from a closed incident.");
        }
        if ("ANALYST".equals(normalizedRole)) {
            boolean involved = (incident.getReporterUserId() != null && createdBy.equals(incident.getReporterUserId()))
                    || (incident.getAssignedTo() != null && createdBy.equals(incident.getAssignedTo()));
            if (!involved) throw new IllegalArgumentException("Analyst is not authorized for this incident.");
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
        caseFile.setCaseNumber("CASE-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
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

    public List<CaseFile> getAllCases() { return caseFileRepository.findAllByOrderByCreatedAtDesc(); }
    public List<CaseFile> getCasesByStatus(String status) {
        return status == null ? List.of() : caseFileRepository.findByStatusOrderByCreatedAtDesc(status.trim().toUpperCase());
    }
    public List<CaseFile> getMyCases(Long userId) {
        return userId == null ? List.of() : caseFileRepository.findByAssignedToOrderByCreatedAtDesc(userId);
    }
    public List<CaseFile> getCasesByIncident(Long incidentId) {
        return incidentId == null ? List.of() : caseFileRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId);
    }
    public CaseFile getById(Long id) { return id == null ? null : caseFileRepository.findById(id).orElse(null); }

    @Transactional
    public boolean assignCase(Long caseId, Long investigatorId, String investigatorName,
                              Long leadInvestigatorId, String leadInvestigatorName,
                              LocalDateTime dueDate, Long actorId, String actorName, String actorRole) {
        CaseFile cf = caseFileRepository.findById(caseId).orElse(null);
        if (cf == null || investigatorId == null || leadInvestigatorId == null) return false;
        String current = cf.getStatus() == null ? "OPEN" : cf.getStatus().trim().toUpperCase();
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

    @Transactional
    public boolean updateStatus(Long caseId, String status, String closureReason,
                                Long actorId, String actorName, String actorRole) {
        CaseFile cf = caseFileRepository.findById(caseId).orElse(null);
        if (cf == null || status == null || status.isBlank()) return false;

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

        if ("CLOSED".equals(to)) {
            if (closureReason == null || closureReason.isBlank() || closureReason.trim().length() > 5000) return false;
            List<CaseTask> tasks = caseTaskRepository.findByCaseIdOrderByCreatedAtDesc(cf.getId());
            boolean hasOpenTask = tasks.stream().anyMatch(t -> {
                String s = t.getStatus() == null ? "OPEN" : t.getStatus().trim().toUpperCase();
                return !"COMPLETED".equals(s) && !"CANCELLED".equals(s);
            });
            if (hasOpenTask) return false;

            List<Evidence> evidence = evidenceRepository.findByCaseIdOrderByUploadedAtDesc(cf.getId());
            boolean unresolved = evidence.stream().anyMatch(e -> {
                String s = e.getCustodyStatus();
                return !"EXAMINED".equalsIgnoreCase(s) && !"REPORT_GENERATED".equalsIgnoreCase(s);
            });
            if (unresolved) return false;
        }

        cf.setStatus(to);
        cf.setUpdatedAt(LocalDateTime.now());
        if ("CLOSED".equals(to)) {
            cf.setClosedAt(LocalDateTime.now());
            cf.setClosureReason(closureReason.trim());
        } else if ("ARCHIVED".equals(to) && cf.getClosedAt() == null) {
            return false;
        }
        caseFileRepository.save(cf);
        addTimeline(cf.getId(), "STATUS_CHANGED", "Case status changed",
                from + " → " + to
                        + (closureReason != null && !closureReason.isBlank() ? " | " + closureReason.trim() : ""),
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