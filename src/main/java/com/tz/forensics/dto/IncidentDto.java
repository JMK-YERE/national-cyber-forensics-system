package com.tz.forensics.dto;

public class IncidentDto {
    private String title;
    private String description;
    private String reporter;
    private String severity;
    private String category;
    private String region;
    private String organization;

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
}
