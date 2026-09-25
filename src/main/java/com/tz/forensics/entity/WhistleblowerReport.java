package com.tz.forensics.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "whistleblower_reports")
public class WhistleblowerReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_code", unique = true, length = 20)
    private String trackingCode;

    @Column(length = 50)
    private String category;

    @Column(length = 50)
    private String urgency = "NORMAL";

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String involvedParties;

    @Column(length = 100)
    private String location;

    @Column(length = 50)
    private String region;

    @Column(name = "date_occurred")
    private LocalDateTime dateOccurred;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "evidence_file_path", length = 500)
    private String evidenceFilePath;

    @Column(name = "evidence_file_type", length = 100)
    private String evidenceFileType;

    @Column(name = "evidence_file_size")
    private Long evidenceFileSize;

    @Column(name = "contact_encrypted", columnDefinition = "TEXT")
    private String contactEncrypted;

    @Column(name = "wants_updates")
    private Boolean wantsUpdates = false;

    @Column(length = 10)
    private String country = "TZ";

    @Column(name = "country_name", length = 100)
    private String countryName;

    @Column(length = 30)
    private String status = "NEW";

    @Column(length = 20)
    private String priority = "MEDIUM";

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public WhistleblowerReport() {}

    public String getCategoryLabel() {
        if (category == null) return "❓ Other";
        return switch (category) {
            case "CORRUPTION" -> "💰 Corruption / Bribery";
            case "FRAUD" -> "🎭 Fraud";
            case "CYBER_ATTACK" -> "💻 Cyber Attack";
            case "HARASSMENT" -> "😢 Harassment";
            case "DRUG_TRAFFICKING" -> "💊 Drug Trafficking";
            case "HUMAN_TRAFFICKING" -> "🚫 Human Trafficking";
            case "TAX_EVASION" -> "📊 Tax Evasion";
            case "ENVIRONMENTAL" -> "🌍 Environmental Crime";
            case "OTHER" -> "❓ Other";
            default -> "❓ " + category;
        };
    }

    public String getPriorityLabel() {
        if (priority == null) return "MEDIUM";
        return switch (priority) {
            case "CRITICAL" -> "🔴 CRITICAL";
            case "HIGH" -> "🟠 HIGH";
            case "MEDIUM" -> "🟡 MEDIUM";
            case "LOW" -> "🟢 LOW";
            default -> "🟡 MEDIUM";
        };
    }

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getInvolvedParties() { return involvedParties; }
    public void setInvolvedParties(String involvedParties) { this.involvedParties = involvedParties; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public LocalDateTime getDateOccurred() { return dateOccurred; }
    public void setDateOccurred(LocalDateTime dateOccurred) { this.dateOccurred = dateOccurred; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public String getEvidenceFilePath() { return evidenceFilePath; }
    public void setEvidenceFilePath(String evidenceFilePath) { this.evidenceFilePath = evidenceFilePath; }
    public String getEvidenceFileType() { return evidenceFileType; }
    public void setEvidenceFileType(String evidenceFileType) { this.evidenceFileType = evidenceFileType; }
    public Long getEvidenceFileSize() { return evidenceFileSize; }
    public void setEvidenceFileSize(Long evidenceFileSize) { this.evidenceFileSize = evidenceFileSize; }
    public String getContactEncrypted() { return contactEncrypted; }
    public void setContactEncrypted(String contactEncrypted) { this.contactEncrypted = contactEncrypted; }
    public Boolean getWantsUpdates() { return wantsUpdates; }
    public void setWantsUpdates(Boolean wantsUpdates) { this.wantsUpdates = wantsUpdates; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCountryName() { return countryName; }
    public void setCountryName(String countryName) { this.countryName = countryName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Long getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Long assignedTo) { this.assignedTo = assignedTo; }
    public String getAdminResponse() { return adminResponse; }
    public void setAdminResponse(String adminResponse) { this.adminResponse = adminResponse; }
    public String getInternalNotes() { return internalNotes; }
    public void setInternalNotes(String internalNotes) { this.internalNotes = internalNotes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
