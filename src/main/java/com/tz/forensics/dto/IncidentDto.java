package com.tz.forensics.dto;

import java.math.BigDecimal;

public class IncidentDto {
    private String title;
    private String description;
    private String reporter;
    private String severity;
    private String category;
    private String region;
    private String organization;
    private BigDecimal directLossTzs;
    private BigDecimal recoveryCostTzs;
    private BigDecimal downtimeCostTzs;
    private BigDecimal legalFeesTzs;
    private BigDecimal reputationDamageTzs;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReporter() { return reporter; }
    public void setReporter(String reporter) { this.reporter = reporter; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
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
}
