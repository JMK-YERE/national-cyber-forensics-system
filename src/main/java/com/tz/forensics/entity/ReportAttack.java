package com.tz.forensics.entity;

import com.tz.forensics.enums.IncidentType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_attacks")
public class ReportAttack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", unique = true, length = 50)
    private String reportId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "attack_type", length = 50)
    private String attackType;

    @Column(nullable = false, length = 100)
    private String reporterName;

    @Column(length = 100)
    private String reporterEmail;

    @Column(length = 20)
    private String reporterPhone;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "date_occurred")
    private LocalDateTime dateOccurred;

    @Column(length = 50)
    private String status = "NEW";

    @Column(length = 20)
    private String priority = "MEDIUM";

    @Column(length = 50)
    private String region;

    @Column(length = 10)
    private String country = "TZ";

    @Column(name = "country_name", length = 100)
    private String countryName;

    @Column(name = "specific_details", columnDefinition = "TEXT")
    private String specificDetails;

    // ===== DYNAMIC DETAILS — JSONB =====
    @Column(name = "dynamic_details", columnDefinition = "jsonb")
    private String dynamicDetails;

    // ===== EVIDENCE =====
    @Column(name = "evidence_file_path", length = 500)
    private String evidenceFilePath;

    @Column(name = "evidence_file_type", length = 100)
    private String evidenceFileType;

    @Column(name = "evidence_file_size")
    private Long evidenceFileSize;

    @Column(name = "has_evidence")
    private Boolean hasEvidence = false;

    // ===== AI / ADMIN =====
    @Column(name = "ai_recommendation", columnDefinition = "TEXT")
    private String aiRecommendation;

    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;

    @Column(name = "police_case_number", length = 50)
    private String policeCaseNumber;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "assigned_to_name", length = 100)
    private String assignedToName;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public ReportAttack() {}

    // ===== HELPERS =====
    public String getAttackTypeLabel() {
        if (attackType == null) return "❓ Unknown";
        try {
            return IncidentType.valueOf(attackType).getLabel();
        } catch (Exception e) {
            return "❓ " + attackType;
        }
    }

    public String getAttackTypeIcon() {
        if (attackType == null) return "❓";
        try {
            return IncidentType.valueOf(attackType).getIcon();
        } catch (Exception e) { return "❓"; }
    }

    public String getAttackTypeColor() {
        if (attackType == null) return "#6b7280";
        try {
            return IncidentType.valueOf(attackType).getColor();
        } catch (Exception e) { return "#6b7280"; }
    }

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    // ===== GETTERS & SETTERS =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAttackType() { return attackType; }
    public void setAttackType(String attackType) { this.attackType = attackType; }
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    public String getReporterEmail() { return reporterEmail; }
    public void setReporterEmail(String reporterEmail) { this.reporterEmail = reporterEmail; }
    public String getReporterPhone() { return reporterPhone; }
    public void setReporterPhone(String reporterPhone) { this.reporterPhone = reporterPhone; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDateTime getDateOccurred() { return dateOccurred; }
    public void setDateOccurred(LocalDateTime dateOccurred) { this.dateOccurred = dateOccurred; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCountryName() { return countryName; }
    public void setCountryName(String countryName) { this.countryName = countryName; }
    public String getSpecificDetails() { return specificDetails; }
    public void setSpecificDetails(String specificDetails) { this.specificDetails = specificDetails; }
    public String getDynamicDetails() { return dynamicDetails; }
    public void setDynamicDetails(String dynamicDetails) { this.dynamicDetails = dynamicDetails; }
    public String getEvidenceFilePath() { return evidenceFilePath; }
    public void setEvidenceFilePath(String evidenceFilePath) { this.evidenceFilePath = evidenceFilePath; }
    public String getEvidenceFileType() { return evidenceFileType; }
    public void setEvidenceFileType(String evidenceFileType) { this.evidenceFileType = evidenceFileType; }
    public Long getEvidenceFileSize() { return evidenceFileSize; }
    public void setEvidenceFileSize(Long evidenceFileSize) { this.evidenceFileSize = evidenceFileSize; }
    public Boolean getHasEvidence() { return hasEvidence; }
    public void setHasEvidence(Boolean hasEvidence) { this.hasEvidence = hasEvidence; }
    public String getAiRecommendation() { return aiRecommendation; }
    public void setAiRecommendation(String aiRecommendation) { this.aiRecommendation = aiRecommendation; }
    public String getAdminResponse() { return adminResponse; }
    public void setAdminResponse(String adminResponse) { this.adminResponse = adminResponse; }
    public String getPoliceCaseNumber() { return policeCaseNumber; }
    public void setPoliceCaseNumber(String policeCaseNumber) { this.policeCaseNumber = policeCaseNumber; }
    public Long getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Long assignedTo) { this.assignedTo = assignedTo; }
    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
