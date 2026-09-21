package com.tz.forensics.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "whistleblower_reports")
public class WhistleblowerReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_code", unique = true, nullable = false, length = 20)
    private String trackingCode;

    @Column(nullable = false, length = 50)
    private String category;
    // CORRUPTION, FRAUD, CYBER_ATTACK, HARASSMENT, DRUGS, HUMAN_TRAFFICKING, TAX_EVASION, OTHER

    @Column(length = 50)
    private String urgency = "NORMAL"; // NORMAL, URGENT, CRITICAL

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "involved_parties", columnDefinition = "TEXT")
    private String involvedParties;

    @Column(length = 100)
    private String location;

    @Column(length = 50)
    private String region;

    @Column(name = "date_occurred")
    private LocalDateTime dateOccurred;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    // ===== REPORTER INFO (OPTIONAL & ENCRYPTED) =====
    @Column(name = "contact_encrypted", columnDefinition = "TEXT")
    private String contactEncrypted; // AES-256 encrypted

    @Column(name = "wants_updates")
    private Boolean wantsUpdates = false;

    // ===== STATUS =====
    @Column(length = 30)
    private String status = "NEW"; // NEW, REVIEWING, INVESTIGATING, ACTION_TAKEN, CLOSED, REJECTED

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

    public String getStatusClass() {
        return switch (status) {
            case "NEW" -> "status-critical";
            case "REVIEWING", "INVESTIGATING" -> "status-investigation";
            case "ACTION_TAKEN", "CLOSED" -> "status-resolved";
            default -> "";
        };
    }

    public String getCategoryLabel() {
        return switch (category) {
            case "CORRUPTION" -> "💰 Rushwa / Ufisadi";
            case "FRAUD" -> "🎭 Utapeli";
            case "CYBER_ATTACK" -> "💻 Mashambulizi ya Mtandao";
            case "HARASSMENT" -> "😢 Unyanyasaji";
            case "DRUGS" -> "💊 Dawa za Kulevya";
            case "HUMAN_TRAFFICKING" -> "🚨 Biashara ya Binadamu";
            case "TAX_EVASION" -> "📊 Kukwepa Kodi";
            case "MONEY_LAUNDERING" -> "💵 Utakatishaji Fedha";
            case "TERRORISM" -> "💥 Ugaidi";
            default -> "⚠️ Nyingine";
        };
    }

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
    public String getContactEncrypted() { return contactEncrypted; }
    public void setContactEncrypted(String contactEncrypted) { this.contactEncrypted = contactEncrypted; }
    public Boolean getWantsUpdates() { return wantsUpdates; }
    public void setWantsUpdates(Boolean wantsUpdates) { this.wantsUpdates = wantsUpdates; }
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
