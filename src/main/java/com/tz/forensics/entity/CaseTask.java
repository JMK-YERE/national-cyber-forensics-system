package com.tz.forensics.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "case_tasks")
public class CaseTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="case_id", nullable=false) private Long caseId;
    @Column(nullable=false, length=200) private String title;
    @Column(columnDefinition="TEXT") private String description;
    @Column(name="assigned_to") private Long assignedTo;
    @Column(name="assigned_to_name", length=120) private String assignedToName;
    @Column(name="created_by") private Long createdBy;
    @Column(name="created_by_name", length=120) private String createdByName;
    @Column(length=20) private String priority="MEDIUM";
    @Column(length=30) private String status="OPEN";
    @Column(name="due_date") private LocalDateTime dueDate;
    @Column(name="completed_at") private LocalDateTime completedAt;
    @Column(name="completed_by") private Long completedBy;
    @Column(name="completed_by_name", length=120) private String completedByName;
    @Column(name="created_at") private LocalDateTime createdAt=LocalDateTime.now();
    @Column(name="updated_at") private LocalDateTime updatedAt=LocalDateTime.now();
    public CaseTask(){}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getCaseId(){return caseId;} public void setCaseId(Long v){caseId=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public Long getAssignedTo(){return assignedTo;} public void setAssignedTo(Long v){assignedTo=v;}
    public String getAssignedToName(){return assignedToName;} public void setAssignedToName(String v){assignedToName=v;}
    public Long getCreatedBy(){return createdBy;} public void setCreatedBy(Long v){createdBy=v;}
    public String getCreatedByName(){return createdByName;} public void setCreatedByName(String v){createdByName=v;}
    public String getPriority(){return priority;} public void setPriority(String v){priority=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public LocalDateTime getDueDate(){return dueDate;} public void setDueDate(LocalDateTime v){dueDate=v;}
    public LocalDateTime getCompletedAt(){return completedAt;} public void setCompletedAt(LocalDateTime v){completedAt=v;}
    public Long getCompletedBy(){return completedBy;} public void setCompletedBy(Long v){completedBy=v;}
    public String getCompletedByName(){return completedByName;} public void setCompletedByName(String v){completedByName=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
    public boolean isOverdue(){return dueDate!=null && !"COMPLETED".equalsIgnoreCase(status) && !"CANCELLED".equalsIgnoreCase(status) && dueDate.isBefore(LocalDateTime.now());}
}