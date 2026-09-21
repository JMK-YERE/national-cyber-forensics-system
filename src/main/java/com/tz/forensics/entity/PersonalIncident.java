package com.tz.forensics.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "personal_incidents")
public class PersonalIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", unique = true, nullable = false, length = 50)
    private String reportId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "reporter_name", length = 100)
    private String reporterName;

    @Column(name = "reporter_phone", length = 30)
    private String reporterPhone;

    @Column(name = "reporter_email", length = 100)
    private String reporterEmail;

    @Column(name = "attack_type", nullable = false, length = 50)
    private String attackType;

    @Column(name = "attack_category", length = 50)
    private String attackCategory;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "date_occurred")
    private LocalDateTime dateOccurred;

    @Column(length = 50)
    private String location;

    @Column(length = 50)
    private String region;

    @Column(name = "financial_loss_tzs")
    private BigDecimal financialLossTzs;

    @Column(name = "has_evidence")
    private Boolean hasEvidence = false;

    @Column(name = "evidence_description", columnDefinition = "TEXT")
    private String evidenceDescription;

    @Column(length = 30)
    private String status = "NEW";

    @Column(length = 20)
    private String priority = "MEDIUM";

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "assigned_to_name", length = 100)
    private String assignedToName;

    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;

    @Column(name = "ai_recommendation", columnDefinition = "TEXT")
    private String aiRecommendation;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public PersonalIncident() {}

    public String getStatusClass() {
        return switch (status) {
            case "NEW" -> "status-critical";
            case "REVIEWING", "INVESTIGATING" -> "status-investigation";
            case "RESOLVED", "CLOSED" -> "status-resolved";
            default -> "";
        };
    }

    public String getAttackTypeLabel() {
        return switch (attackType != null ? attackType : "OTHER") {
            case "PHONE_STOLEN" -> "📱 Simu Iliibiwa";
            case "SOCIAL_MEDIA_HACKED" -> "👤 Account ya Social Media Iliibiwa";
            case "BANK_CARD_LOST" -> "💳 Kadi ya Benki Ilipotea";
            case "MONEY_STOLEN" -> "💰 Pesa Iliibiwa";
            case "EMAIL_HACKED" -> "📧 Email Iliibiwa";
            case "PHISHING" -> "🎣 Phishing";
            case "DOCUMENTS_STOLEN" -> "💼 Documents Ziliibiwa";
            case "HOME_BREAK_IN" -> "🏠 Nyumbani Kuliingiliwa";
            case "IDENTITY_THEFT" -> "🆔 Utambulisho Uliibiwa";
            default -> "⚠️ Nyingine";
        };
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    public String getReporterPhone() { return reporterPhone; }
    public void setReporterPhone(String reporterPhone) { this.reporterPhone = reporterPhone; }
    public String getReporterEmail() { return reporterEmail; }
    public void setReporterEmail(String reporterEmail) { this.reporterEmail = reporterEmail; }
    public String getAttackType() { return attackType; }
    public void setAttackType(String attackType) { this.attackType = attackType; }
    public String getAttackCategory() { return attackCategory; }
    public void setAttackCategory(String attackCategory) { this.attackCategory = attackCategory; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDateOccurred() { return dateOccurred; }
    public void setDateOccurred(LocalDateTime dateOccurred) { this.dateOccurred = dateOccurred; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public BigDecimal getFinancialLossTzs() { return financialLossTzs; }
    public void setFinancialLossTzs(BigDecimal financialLossTzs) { this.financialLossTzs = financialLossTzs; }
    public Boolean getHasEvidence() { return hasEvidence; }
    public void setHasEvidence(Boolean hasEvidence) { this.hasEvidence = hasEvidence; }
    public String getEvidenceDescription() { return evidenceDescription; }
    public void setEvidenceDescription(String evidenceDescription) { this.evidenceDescription = evidenceDescription; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Long getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Long assignedTo) { this.assignedTo = assignedTo; }
    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }
    public String getAdminResponse() { return adminResponse; }
    public void setAdminResponse(String adminResponse) { this.adminResponse = adminResponse; }
    public String getAiRecommendation() { return aiRecommendation; }
    public void setAiRecommendation(String aiRecommendation) { this.aiRecommendation = aiRecommendation; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
