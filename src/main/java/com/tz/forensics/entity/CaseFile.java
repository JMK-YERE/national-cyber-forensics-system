package com.tz.forensics.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "case_files")
public class CaseFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_number", unique = true, nullable = false, length = 50)
    private String caseNumber;

    @Column(name = "incident_id")
    private Long incidentId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 30)
    private String status = "OPEN"; // OPEN, INVESTIGATING, CLOSED, ARCHIVED

    @Column(length = 30)
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "assigned_to_name")
    private String assignedToName;

    @Column(name = "lead_investigator")
    private Long leadInvestigator;

    @Column(name = "lead_investigator_name")
    private String leadInvestigatorName;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closure_reason", columnDefinition = "TEXT")
    private String closureReason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_by_name")
    private String createdByName;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public CaseFile() {}

    public String getStatusClass() {
        return switch (status) {
            case "OPEN" -> "status-critical";
            case "INVESTIGATING" -> "status-investigation";
            case "CLOSED" -> "status-resolved";
            default -> "";
        };
    }

    public String getPriorityClass() {
        return "severity-" + (priority != null ? priority.toLowerCase() : "medium");
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }
    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Long getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Long assignedTo) { this.assignedTo = assignedTo; }
    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }
    public Long getLeadInvestigator() { return leadInvestigator; }
    public void setLeadInvestigator(Long leadInvestigator) { this.leadInvestigator = leadInvestigator; }
    public String getLeadInvestigatorName() { return leadInvestigatorName; }
    public void setLeadInvestigatorName(String leadInvestigatorName) { this.leadInvestigatorName = leadInvestigatorName; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public String getClosureReason() { return closureReason; }
    public void setClosureReason(String closureReason) { this.closureReason = closureReason; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
