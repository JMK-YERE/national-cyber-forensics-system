package com.tz.forensics.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "case_timeline")
public class CaseTimeline {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="case_id", nullable=false) private Long caseId;
    @Column(length=60, nullable=false) private String eventType;
    @Column(length=200, nullable=false) private String title;
    @Column(columnDefinition="TEXT") private String details;
    @Column(name="actor_id") private Long actorId;
    @Column(name="actor_name", length=100) private String actorName;
    @Column(name="actor_role", length=50) private String actorRole;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt=LocalDateTime.now();

    public CaseTimeline() {}
    public CaseTimeline(Long caseId,String eventType,String title,String details,Long actorId,String actorName,String actorRole){
        this.caseId=caseId;this.eventType=eventType;this.title=title;this.details=details;this.actorId=actorId;this.actorName=actorName;this.actorRole=actorRole;
    }
    public Long getId(){return id;} public Long getCaseId(){return caseId;} public void setCaseId(Long v){caseId=v;}
    public String getEventType(){return eventType;} public void setEventType(String v){eventType=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getDetails(){return details;} public void setDetails(String v){details=v;}
    public Long getActorId(){return actorId;} public void setActorId(Long v){actorId=v;}
    public String getActorName(){return actorName;} public void setActorName(String v){actorName=v;}
    public String getActorRole(){return actorRole;} public void setActorRole(String v){actorRole=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
}