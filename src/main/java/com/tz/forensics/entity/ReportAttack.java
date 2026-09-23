package com.tz.forensics.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_attacks")
public class ReportAttack {

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

    @Column(name = "reporter_id_number", length = 100)
    private String reporterIdNumber;

    @Column(name = "reporter_address", length = 200)
    private String reporterAddress;

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

    @Column(length = 100)
    private String location;

    @Column(length = 50)
    private String region;

    @Column(length = 10)
    private String country = "TZ";

    @Column(name = "country_name", length = 100)
    private String countryName;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "financial_loss_tzs")
    private BigDecimal financialLossTzs;

    @Column(name = "financial_currency", length = 10)
    private String financialCurrency = "TZS";

    // ===== SPECIFIC DETAILS — JSON-like =====
    @Column(name = "specific_details", columnDefinition = "TEXT")
    private String specificDetails;

    // ===== EVIDENCE =====
    @Column(name = "has_evidence")
    private Boolean hasEvidence = false;

    @Column(name = "evidence_description", columnDefinition = "TEXT")
    private String evidenceDescription;

    @Column(name = "evidence_file_path", length = 500)
    private String evidenceFilePath;

    @Column(name = "evidence_file_type", length = 100)
    private String evidenceFileType;

    @Column(name = "evidence_file_size")
    private Long evidenceFileSize;

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

    @Column(name = "police_case_number", length = 50)
    private String policeCaseNumber;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public ReportAttack() {}

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
            case "PHONE_STOLEN" -> "📱 Phone Stolen";
            case "SOCIAL_MEDIA_HACKED" -> "👤 Social Media Hacked";
            case "BANK_CARD_LOST" -> "💳 Bank Card Lost";
            case "MONEY_STOLEN" -> "💰 Money Stolen";
            case "EMAIL_HACKED" -> "📧 Email Hacked";
            case "PHISHING" -> "🎣 Phishing";
            case "DOCUMENTS_STOLEN" -> "💼 Documents Stolen";
            case "HOME_BREAK_IN" -> "🏠 Home Break-In";
            case "IDENTITY_THEFT" -> "🆔 Identity Theft";
            case "SIM_SWAP" -> "📲 SIM Swap";
            case "RANSOMWARE" -> "🦠 Ransomware";
            default -> "⚠️ Other";
        };
    }

    // Getters & Setters
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
    public String getReporterIdNumber() { return reporterIdNumber; }
    public void setReporterIdNumber(String reporterIdNumber) { this.reporterIdNumber = reporterIdNumber; }
    public String getReporterAddress() { return reporterAddress; }
    public void setReporterAddress(String reporterAddress) { this.reporterAddress = reporterAddress; }
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
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCountryName() { return countryName; }
    public void setCountryName(String countryName) { this.countryName = countryName; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public BigDecimal getFinancialLossTzs() { return financialLossTzs; }
    public void setFinancialLossTzs(BigDecimal financialLossTzs) { this.financialLossTzs = financialLossTzs; }
    public String getFinancialCurrency() { return financialCurrency; }
    public void setFinancialCurrency(String financialCurrency) { this.financialCurrency = financialCurrency; }
    public String getSpecificDetails() { return specificDetails; }
    public void setSpecificDetails(String specificDetails) { this.specificDetails = specificDetails; }
    public Boolean getHasEvidence() { return hasEvidence; }
    public void setHasEvidence(Boolean hasEvidence) { this.hasEvidence = hasEvidence; }
    public String getEvidenceDescription() { return evidenceDescription; }
    public void setEvidenceDescription(String evidenceDescription) { this.evidenceDescription = evidenceDescription; }
    public String getEvidenceFilePath() { return evidenceFilePath; }
    public void setEvidenceFilePath(String evidenceFilePath) { this.evidenceFilePath = evidenceFilePath; }
    public String getEvidenceFileType() { return evidenceFileType; }
    public void setEvidenceFileType(String evidenceFileType) { this.evidenceFileType = evidenceFileType; }
    public Long getEvidenceFileSize() { return evidenceFileSize; }
    public void setEvidenceFileSize(Long evidenceFileSize) { this.evidenceFileSize = evidenceFileSize; }
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
    public String getPoliceCaseNumber() { return policeCaseNumber; }
    public void setPoliceCaseNumber(String policeCaseNumber) { this.policeCaseNumber = policeCaseNumber; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
