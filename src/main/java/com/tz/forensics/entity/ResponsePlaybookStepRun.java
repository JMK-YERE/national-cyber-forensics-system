package com.tz.forensics.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "response_playbook_step_runs",
    uniqueConstraints = @UniqueConstraint(name = "uq_response_playbook_step_order",
        columnNames = {"run_id", "step_order"}))
public class ResponsePlaybookStepRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    @Column(nullable = false)
    private Long version = 0L;
    @Column(name = "run_id", nullable = false)
    private Long runId;
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;
    @Column(nullable = false, length = 40)
    private String phase;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String guidance;
    @Column(nullable = false)
    private boolean required;
    @Column(nullable = false, length = 20)
    private String status = "PENDING";
    @Column(name = "completion_notes", columnDefinition = "TEXT")
    private String completionNotes;
    @Column(name = "completed_by")
    private Long completedBy;
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
    @jakarta.persistence.Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion = 0L;

    public ResponsePlaybookStepRun() {}
    public Long getId(){return id;} public Long getRunId(){return runId;} public void setRunId(Long v){runId=v;}
    public Integer getStepOrder(){return stepOrder;} public void setStepOrder(Integer v){stepOrder=v;}
    public String getPhase(){return phase;} public void setPhase(String v){phase=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getGuidance(){return guidance;} public void setGuidance(String v){guidance=v;}
    public boolean isRequired(){return required;} public void setRequired(boolean v){required=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getCompletionNotes(){return completionNotes;} public void setCompletionNotes(String v){completionNotes=v;}
    public Long getCompletedBy(){return completedBy;} public void setCompletedBy(Long v){completedBy=v;}
    public OffsetDateTime getCompletedAt(){return completedAt;} public void setCompletedAt(OffsetDateTime v){completedAt=v;}
}
