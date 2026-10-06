package com.tz.forensics.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="case_communications")
public class CaseCommunication {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="case_id",nullable=false) private Long caseId;
 @Column(name="author_id",nullable=false) private Long authorId;
 @Column(name="author_name",length=120) private String authorName;
 @Column(name="author_role",length=50) private String authorRole;
 @Column(nullable=false,columnDefinition="TEXT") private String message;
 @Column(length=20) private String tlp="CLEAR";
 @Column(name="created_at") private LocalDateTime createdAt=LocalDateTime.now();
 public CaseCommunication(){}
 public CaseCommunication(Long caseId,Long authorId,String authorName,String authorRole,String message,String tlp){this.caseId=caseId;this.authorId=authorId;this.authorName=authorName;this.authorRole=authorRole;this.message=message;this.tlp=tlp;this.createdAt=LocalDateTime.now();}
 public Long getId(){return id;} public Long getCaseId(){return caseId;} public Long getAuthorId(){return authorId;} public String getAuthorName(){return authorName;} public String getAuthorRole(){return authorRole;} public String getMessage(){return message;} public String getTlp(){return tlp;} public LocalDateTime getCreatedAt(){return createdAt;}
 public void setId(Long v){id=v;} public void setCaseId(Long v){caseId=v;} public void setAuthorId(Long v){authorId=v;} public void setAuthorName(String v){authorName=v;} public void setAuthorRole(String v){authorRole=v;} public void setMessage(String v){message=v;} public void setTlp(String v){tlp=v;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
}