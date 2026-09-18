package com.tz.forensics.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", unique = true, nullable = false, length = 50)
    private String incidentId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String reporter;

    @Column(name = "reporter_user_id")
    private Long reporterUserId;

    @Column(name = "date_reported", nullable = false)
    private LocalDateTime dateReported;

    @Column(name = "date_occurred")
    private LocalDateTime dateOccurred;

    private String status = "Under Investigation";
    private String severity = "MEDIUM";

    @Column(name = "cvss_score")
    private BigDecimal cvssScore;

    private String category;
    private String region;
    private String organization;

    @Column(name = "mitre_tactic")
    private String mitreTactic;

    @Column(name = "mitre_technique")
    private String mitreTechnique;

    @Column(name = "direct_loss_tzs")
    private BigDecimal directLossTzs;

    @Column(name = "recovery_cost_tzs")
    private BigDecimal recoveryCostTzs;

    @Column(name = "downtime_cost_tzs")
    private BigDecimal downtimeCostTzs;

    @Column(name = "legal_fees_tzs")
    private BigDecimal legalFeesTzs;

    @Column(name = "reputation_damage_tzs")
    private BigDecimal reputationDamageTzs;

    @Column(name = "total_loss_tzs")
    private BigDecimal totalLossTzs;

    @Column(name = "police_case_number")
    private String policeCaseNumber;

    @Column(name = "tcra_reference")
    private String tcraReference;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "is_closed")
    private Boolean isClosed = false;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Incident() {}

    public String getStatusClass() {
        return switch (status) {
            case "Critical" -> "status-critical";
            case "Under Investigation" -> "status-investigation";
            case "Resolved" -> "status-resolved";
            default -> "";
        };
    }

    public String getSeverityClass() {
        return "severity-" + (severity != null ? severity.toLowerCase() : "medium");
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReporter() { return reporter; }
    public void setReporter(String reporter) { this.reporter = reporter; }
    public Long getReporterUserId() { return reporterUserId; }
    public void setReporterUserId(Long reporterUserId) { this.reporterUserId = reporterUserId; }
    public LocalDateTime getDateReported() { return dateReported; }
    public void setDateReported(LocalDateTime dateReported) { this.dateReported = dateReported; }
    public LocalDateTime getDateOccurred() { return dateOccurred; }
    public void setDateOccurred(LocalDateTime dateOccurred) { this.dateOccurred = dateOccurred; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public BigDecimal getCvssScore() { return cvssScore; }
    public void setCvssScore(BigDecimal cvssScore) { this.cvssScore = cvssScore; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public String getMitreTactic() { return mitreTactic; }
    public void setMitreTactic(String mitreTactic) { this.mitreTactic = mitreTactic; }
    public String getMitreTechnique() { return mitreTechnique; }
    public void setMitreTechnique(String mitreTechnique) { this.mitreTechnique = mitreTechnique; }
    public BigDecimal getDirectLossTzs() { return directLossTzs; }
    public void setDirectLossTzs(BigDecimal directLossTzs) { this.directLossTzs = directLossTzs; }
    public BigDecimal getRecoveryCostTzs() { return recoveryCostTzs; }
    public void setRecoveryCostTzs(BigDecimal recoveryCostTzs) { this.recoveryCostTzs = recoveryCostTzs; }
    public BigDecimal getDowntimeCostTzs() { return downtimeCostTzs; }
    public void setDowntimeCostTzs(BigDecimal downtimeCostTzs) { this.downtimeCostTzs = downtimeCostTzs; }
    public BigDecimal getLegalFeesTzs() { return legalFeesTzs; }
    public void setLegalFeesTzs(BigDecimal legalFeesTzs) { this.legalFeesTzs = legalFeesTzs; }
    public BigDecimal getReputationDamageTzs() { return reputationDamageTzs; }
    public void setReputationDamageTzs(BigDecimal reputationDamageTzs) { this.reputationDamageTzs = reputationDamageTzs; }
    public BigDecimal getTotalLossTzs() { return totalLossTzs; }
    public void setTotalLossTzs(BigDecimal totalLossTzs) { this.totalLossTzs = totalLossTzs; }
    public String getPoliceCaseNumber() { return policeCaseNumber; }
    public void setPoliceCaseNumber(String policeCaseNumber) { this.policeCaseNumber = policeCaseNumber; }
    public String getTcraReference() { return tcraReference; }
    public void setTcraReference(String tcraReference) { this.tcraReference = tcraReference; }
    public Long getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Long assignedTo) { this.assignedTo = assignedTo; }
    public Boolean getIsClosed() { return isClosed; }
    public void setIsClosed(Boolean isClosed) { this.isClosed = isClosed; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
