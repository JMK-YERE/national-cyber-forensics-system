package com.tz.forensics.service;

import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.entity.CaseTask;
import com.tz.forensics.repository.CaseFileRepository;
import com.tz.forensics.repository.CaseTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class CaseTaskService {
    private static final Set<String> PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
    private final CaseTaskRepository repository;
    private final CaseFileRepository caseFileRepository;

    public CaseTaskService(CaseTaskRepository repository, CaseFileRepository caseFileRepository) {
        this.repository = repository;
        this.caseFileRepository = caseFileRepository;
    }

    public List<CaseTask> findByCaseId(Long id) {
        return id == null ? List.of() : repository.findByCaseIdOrderByCreatedAtDesc(id);
    }

    public List<CaseTask> findMyTasks(Long userId) {
        return userId == null ? List.of() : repository.findByAssignedToOrderByCreatedAtDesc(userId);
    }

    public CaseTask get(Long id) {
        return id == null ? null : repository.findById(id).orElse(null);
    }

    @Transactional
    public CaseTask create(Long caseId, String title, String description, Long assignedTo, String assignedName,
                           String priority, LocalDateTime dueDate, Long creator, String creatorName) {
        if (caseId == null || creator == null || assignedTo == null) return null;
        CaseFile cf = caseFileRepository.findById(caseId).orElse(null);
        if (cf == null || isClosed(cf)) return null;

        String normalizedTitle = title == null ? "" : title.trim();
        if (normalizedTitle.length() < 3 || normalizedTitle.length() > 300) return null;
        String normalizedPriority = priority == null || priority.isBlank() ? "MEDIUM" : priority.trim().toUpperCase();
        if (!PRIORITIES.contains(normalizedPriority)) return null;
        if (dueDate != null && dueDate.isBefore(LocalDateTime.now())) return null;

        CaseTask t = new CaseTask();
        t.setCaseId(caseId);
        t.setTitle(normalizedTitle);
        t.setDescription(description == null ? null : description.trim());
        t.setAssignedTo(assignedTo);
        t.setAssignedToName(assignedName);
        t.setPriority(normalizedPriority);
        t.setDueDate(dueDate);
        t.setCreatedBy(creator);
        t.setCreatedByName(creatorName);
        return repository.save(t);
    }

    @Transactional
    public boolean transition(CaseTask t, String to, Long actor, String actorName) {
        if (t == null || t.getId() == null || actor == null || to == null || to.isBlank()) return false;
        CaseFile cf = caseFileRepository.findById(t.getCaseId()).orElse(null);
        if (cf == null || isClosed(cf)) return false;

        String from = t.getStatus() == null ? "OPEN" : t.getStatus().trim().toUpperCase();
        String next = to.trim().toUpperCase();
        boolean valid = switch (from) {
            case "OPEN" -> "IN_PROGRESS".equals(next) || "CANCELLED".equals(next);
            case "IN_PROGRESS" -> "BLOCKED".equals(next) || "COMPLETED".equals(next) || "CANCELLED".equals(next);
            case "BLOCKED" -> "IN_PROGRESS".equals(next) || "CANCELLED".equals(next);
            case "COMPLETED", "CANCELLED" -> false;
            default -> false;
        };
        if (!valid) return false;

        t.setStatus(next);
        t.setUpdatedAt(LocalDateTime.now());
        if ("COMPLETED".equals(next)) {
            t.setCompletedAt(LocalDateTime.now());
            t.setCompletedBy(actor);
            t.setCompletedByName(actorName);
        }
        repository.save(t);
        return true;
    }

    private boolean isClosed(CaseFile cf) {
        String s = cf.getStatus() == null ? "OPEN" : cf.getStatus().trim().toUpperCase();
        return "CLOSED".equals(s) || "ARCHIVED".equals(s);
    }
}