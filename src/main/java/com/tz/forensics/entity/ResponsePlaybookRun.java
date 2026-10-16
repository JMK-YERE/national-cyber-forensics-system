package com.tz.forensics.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "response_playbook_runs")
public class ResponsePlaybookRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "case_id", nullable = false)
    private Long caseId;
    @Column(name = "playbook_slug", nullable = false, length = 80)
    private String playbookSlug;
    @Column(name = "playbook_version", nullable = false, length = 20)
    private String playbookVersion;
    @Column(nullable = false, length = 20)
    private String status = "IN_PROGRESS";
    @Column(name = "started_by")
    private Long startedBy;
    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt = OffsetDateTime.now();
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public ResponsePlaybookRun() {}
    public Long getId(){return id;} public Long getCaseId(){return caseId;} public void setCaseId(Long v){caseId=v;}
    public String getPlaybookSlug(){return playbookSlug;} public void setPlaybookSlug(String v){playbookSlug=v;}
    public String getPlaybookVersion(){return playbookVersion;} public void setPlaybookVersion(String v){playbookVersion=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public Long getStartedBy(){return startedBy;} public void setStartedBy(Long v){startedBy=v;}
    public OffsetDateTime getStartedAt(){return startedAt;} public OffsetDateTime getCompletedAt(){return completedAt;}
    public void setCompletedAt(OffsetDateTime v){completedAt=v;}
}
