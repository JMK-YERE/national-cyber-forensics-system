package com.tz.forensics.service;

import com.tz.forensics.dto.IncidentDto;
import com.tz.forensics.entity.Incident;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.IncidentRepository;
import com.tz.forensics.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.security.SecureRandom;

@Service
public class IncidentService {
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public IncidentService(IncidentRepository incidentRepository, UserRepository userRepository) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
    }

    public Incident createIncident(IncidentDto dto, String username) {
        validateIncident(dto, username);
        User reporterUser = userRepository.findByUsername(username).orElse(null);
        Incident incident = new Incident();

        String incidentId = generateIncidentId();
        incident.setIncidentId(incidentId);
        incident.setTitle(dto.getTitle());
        incident.setDescription(dto.getDescription());
        String reporterName = reporterUser != null && reporterUser.getFullName() != null && !reporterUser.getFullName().isBlank()
                ? reporterUser.getFullName() : username;
        incident.setReporter(reporterName);
        incident.setReporterUserId(reporterUser != null ? reporterUser.getId() : null);
        incident.setDateReported(LocalDateTime.now());
        incident.setStatus("Under Investigation");
        incident.setSeverity(dto.getSeverity() != null ? dto.getSeverity() : "MEDIUM");
        incident.setCategory(dto.getCategory());
        incident.setRegion(dto.getRegion());
        incident.setOrganization(dto.getOrganization());
        incident.setWorkflowStatus("NEW");

        incident.setDirectLossTzs(dto.getDirectLossTzs());
        incident.setRecoveryCostTzs(dto.getRecoveryCostTzs());
        incident.setDowntimeCostTzs(dto.getDowntimeCostTzs());
        incident.setLegalFeesTzs(dto.getLegalFeesTzs());
        incident.setReputationDamageTzs(dto.getReputationDamageTzs());

        BigDecimal total = BigDecimal.ZERO;
        if (dto.getDirectLossTzs() != null) total = total.add(dto.getDirectLossTzs());
        if (dto.getRecoveryCostTzs() != null) total = total.add(dto.getRecoveryCostTzs());
        if (dto.getDowntimeCostTzs() != null) total = total.add(dto.getDowntimeCostTzs());
        if (dto.getLegalFeesTzs() != null) total = total.add(dto.getLegalFeesTzs());
        if (dto.getReputationDamageTzs() != null) total = total.add(dto.getReputationDamageTzs());
        incident.setTotalLossTzs(total);

        return incidentRepository.save(incident);
    }

    private void validateIncident(IncidentDto dto, String username) {
        if (dto == null) throw new IllegalArgumentException("Incident data is required.");
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Authenticated reporter is required.");

        String title = dto.getTitle() == null ? "" : dto.getTitle().trim();
        String description = dto.getDescription() == null ? "" : dto.getDescription().trim();
        if (title.length() < 3 || title.length() > 200) {
            throw new IllegalArgumentException("Incident title must be between 3 and 200 characters.");
        }
        if (description.length() < 10 || description.length() > 10000) {
            throw new IllegalArgumentException("Incident description must be between 10 and 10000 characters.");
        }

        String severity = dto.getSeverity() == null || dto.getSeverity().isBlank()
                ? "MEDIUM" : dto.getSeverity().trim().toUpperCase();
        if (!Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(severity)) {
            throw new IllegalArgumentException("Unsupported incident severity.");
        }

        validateNonNegative(dto.getDirectLossTzs(), "Direct loss");
        validateNonNegative(dto.getRecoveryCostTzs(), "Recovery cost");
        validateNonNegative(dto.getDowntimeCostTzs(), "Downtime cost");
        validateNonNegative(dto.getLegalFeesTzs(), "Legal fees");
        validateNonNegative(dto.getReputationDamageTzs(), "Reputation damage");
    }

    private void validateNonNegative(BigDecimal value, String field) {
        if (value != null && value.signum() < 0) {
            throw new IllegalArgumentException(field + " cannot be negative.");
        }
    }

    private String generateIncidentId() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int i = 0; i < 20; i++) {
            int rand = 100 + secureRandom.nextInt(900);
            String candidate = "SEC-" + date + "-" + rand;
            if (!incidentRepository.existsByIncidentId(candidate)) return candidate;
        }
        return "SEC-" + date + "-" + secureRandom.nextInt(900000);
    }

    public List<Incident> getAllIncidents() { return incidentRepository.findAllByOrderByDateReportedDesc(); }
    public Incident getById(Long id) { return incidentRepository.findById(id).orElse(null); }
    public List<Incident> searchByIncidentId(String incidentId) {
        return incidentRepository.findByIncidentIdContainingIgnoreCase(incidentId);
    }
    public List<Incident> getMyIncidents(Long userId) {
        return incidentRepository.findByReporterUserIdOrAssignedToOrderByDateReportedDesc(userId, userId);
    }
    public List<Incident> getMyActiveIncidents(Long userId) {
        return incidentRepository.findByReporterUserIdOrAssignedToAndWorkflowStatusNotOrderByDateReportedDesc(userId, userId, "CLOSED");
    }

    public void assignIncident(Long incidentId, Long assignedTo, String assignedToName,
                               Long assignedBy, String priority, LocalDateTime dueDate) {
        if (incidentId == null || assignedTo == null) {
            throw new IllegalArgumentException("Incident and assignee are required.");
        }
        Incident incident = incidentRepository.findById(incidentId).orElse(null);
        if (incident == null) throw new IllegalArgumentException("Incident not found.");
        if ("CLOSED".equalsIgnoreCase(incident.getWorkflowStatus())) {
            throw new IllegalStateException("Closed incidents cannot be assigned.");
        }

        User assignee = userRepository.findById(assignedTo).orElse(null);
        if (assignee == null
                || !Boolean.TRUE.equals(assignee.getEnabled())
                || !assignee.isApproved()
                || !(assignee.isAdmin() || assignee.isProfessional() || assignee.isForensics() || assignee.isAnalyst())) {
            throw new IllegalArgumentException("Assignee is inactive, unapproved, or not authorized.");
        }
        if (dueDate != null && dueDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Due date cannot be in the past.");
        }
        if (priority != null && !priority.isBlank()) {
            String normalizedPriority = priority.trim().toUpperCase();
            if (!Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(normalizedPriority)) {
                throw new IllegalArgumentException("Unsupported incident priority.");
            }
            priority = normalizedPriority;
        }

        incident.setAssignedTo(assignedTo);
        incident.setAssignedToName(assignedToName);
        incident.setAssignedBy(assignedBy);
        incident.setAssignedAt(LocalDateTime.now());
        if (priority != null && !priority.isBlank()) incident.setPriority(priority.trim().toUpperCase());
        if (dueDate != null) incident.setDueDate(dueDate);

        String current = normalizeWorkflow(incident.getWorkflowStatus());
        if ("NEW".equals(current) || "TRIAGED".equals(current)) {
            incident.setWorkflowStatus("ASSIGNED");
        }
        incidentRepository.save(incident);
    }

    public void updateWorkflowStatus(Long incidentId, String status) {
        if (incidentId == null) throw new IllegalArgumentException("Incident id is required.");
        String requested = normalizeWorkflow(status);
        if (!ALLOWED_TRANSITIONS.containsKey(requested)) {
            throw new IllegalArgumentException("Unsupported workflow status: " + status);
        }

        Incident incident = incidentRepository.findById(incidentId).orElse(null);
        if (incident == null) throw new IllegalArgumentException("Incident not found.");

        String current = normalizeWorkflow(incident.getWorkflowStatus());
        if (current.equals(requested)) return;
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(requested)) {
            throw new IllegalStateException("Invalid incident workflow transition: " + current + " -> " + requested);
        }
        if ("ASSIGNED".equals(requested) && incident.getAssignedTo() == null) {
            throw new IllegalStateException("Incident must have an assignee before entering ASSIGNED.");
        }

        incident.setWorkflowStatus(requested);
        if ("CLOSED".equals(requested)) {
            incident.setIsClosed(true);
            incident.setClosedAt(LocalDateTime.now());
            incident.setStatus("Resolved");
        } else {
            incident.setIsClosed(false);
            incident.setClosedAt(null);
            incident.setStatus("Under Investigation");
        }
        incidentRepository.save(incident);
    }

    private static final java.util.Map<String, java.util.Set<String>> ALLOWED_TRANSITIONS = java.util.Map.of(
            "NEW", java.util.Set.of("TRIAGED"),
            "TRIAGED", java.util.Set.of("ASSIGNED"),
            "ASSIGNED", java.util.Set.of("INVESTIGATING"),
            "INVESTIGATING", java.util.Set.of("CONTAINMENT", "ERADICATION"),
            "CONTAINMENT", java.util.Set.of("ERADICATION", "RECOVERY"),
            "ERADICATION", java.util.Set.of("RECOVERY"),
            "RECOVERY", java.util.Set.of("CLOSED"),
            "CLOSED", java.util.Set.of()
    );

    private String normalizeWorkflow(String status) {
        return status == null ? "NEW" : status.trim().toUpperCase();
    }
}