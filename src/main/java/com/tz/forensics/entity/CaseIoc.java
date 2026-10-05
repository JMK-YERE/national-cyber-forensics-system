package com.tz.forensics.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="case_ioc")
public class CaseIoc {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="case_id",nullable=false) private Long caseId;
    @Column(name="ioc_type",length=30,nullable=false) private String iocType;
    @Column(length=500,nullable=false) private String value;
    @Column(length=20) private String confidence;
    @Column(length=120) private String source;
    @Column(name="first_seen") private LocalDateTime firstSeen;
    @Column(name="last_seen") private LocalDateTime lastSeen;
    @Column(columnDefinition="TEXT") private String notes;
    @Column(name="created_by") private Long createdBy;
    @Column(name="created_by_name",length=100) private String createdByName;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt=LocalDateTime.now();

    public CaseIoc(){}
    public Long getId(){return id;} public Long getCaseId(){return caseId;} public void setCaseId(Long v){caseId=v;}
    public String getIocType(){return iocType;} public void setIocType(String v){iocType=v;}
    public String getValue(){return value;} public void setValue(String v){value=v;}
    public String getConfidence(){return confidence;} public void setConfidence(String v){confidence=v;}
    public String getSource(){return source;} public void setSource(String v){source=v;}
    public LocalDateTime getFirstSeen(){return firstSeen;} public void setFirstSeen(LocalDateTime v){firstSeen=v;}
    public LocalDateTime getLastSeen(){return lastSeen;} public void setLastSeen(LocalDateTime v){lastSeen=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public Long getCreatedBy(){return createdBy;} public void setCreatedBy(Long v){createdBy=v;}
    public String getCreatedByName(){return createdByName;} public void setCreatedByName(String v){createdByName=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
}