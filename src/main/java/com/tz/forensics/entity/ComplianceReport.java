package com.tz.forensics.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "compliance_reports")
public class ComplianceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", unique = true, nullable = false, length = 50)
    private String reportId;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType; // TCRA, ISO27001, DATA_PROTECTION, ANNUAL

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "compliance_score")
    private Integer complianceScore = 0;

    @Column(name = "total_controls")
    private Integer totalControls = 0;

    @Column(name = "passed_controls")
    private Integer passedControls = 0;

    @Column(name = "failed_controls")
    private Integer failedControls = 0;

    @Column(name = "period_start")
    private LocalDateTime periodStart;

    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    @Column(columnDefinition = "TEXT")
    private String findings;

    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @Column(length = 30)
    private String status = "DRAFT"; // DRAFT, FINAL, SUBMITTED

    @Column(name = "generated_by")
    private Long generatedBy;

    @Column(name = "generated_by_name", length = 100)
    private String generatedByName;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public ComplianceReport() {}

    public String getReportTypeLabel() {
        return switch (reportType != null ? reportType : "OTHER") {
            case "TCRA" -> "🇹🇿 TCRA Compliance";
            case "ISO27001" -> "📊 ISO 27001";
            case "DATA_PROTECTION" -> "🔒 Data Protection Act";
            case "ANNUAL" -> "📅 Annual Report";
            default -> "Other";
        };
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Integer getComplianceScore() { return complianceScore; }
    public void setComplianceScore(Integer complianceScore) { this.complianceScore = complianceScore; }
    public Integer getTotalControls() { return totalControls; }
    public void setTotalControls(Integer totalControls) { this.totalControls = totalControls; }
    public Integer getPassedControls() { return passedControls; }
    public void setPassedControls(Integer passedControls) { this.passedControls = passedControls; }
    public Integer getFailedControls() { return failedControls; }
    public void setFailedControls(Integer failedControls) { this.failedControls = failedControls; }
    public LocalDateTime getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }
    public LocalDateTime getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDateTime periodEnd) { this.periodEnd = periodEnd; }
    public String getFindings() { return findings; }
    public void setFindings(String findings) { this.findings = findings; }
    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(Long generatedBy) { this.generatedBy = generatedBy; }
    public String getGeneratedByName() { return generatedByName; }
    public void setGeneratedByName(String generatedByName) { this.generatedByName = generatedByName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
