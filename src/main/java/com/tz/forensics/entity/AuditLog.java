package com.tz.forensics.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id") private Long userId;
    private String username;
    @Column(nullable = false, length = 100) private String action;
    @Column(name = "entity_type") private String entityType;
    @Column(name = "entity_id") private String entityId;
    @Column(columnDefinition = "TEXT") private String details;
    @Column(name = "ip_address", length = 45) private String ipAddress;
    @Column(name = "user_agent", length = 500) private String userAgent;
    private LocalDateTime timestamp = LocalDateTime.now();

    public AuditLog() {}
    public AuditLog(Long userId, String username, String action, String entityType, String entityId, String details, String ipAddress) {
        this.userId=userId; this.username=username; this.action=action; this.entityType=entityType; this.entityId=entityId; this.details=details; this.ipAddress=ipAddress;
    }

    @PreUpdate private void preventUpdate(){ throw new IllegalStateException("Audit log records are immutable."); }
    @PreRemove private void preventDelete(){ throw new IllegalStateException("Audit log records cannot be deleted."); }

    public Long getId(){return id;} public void setId(Long v){this.id=v;}
    public Long getUserId(){return userId;} public void setUserId(Long v){this.userId=v;}
    public String getUsername(){return username;} public void setUsername(String v){this.username=v;}
    public String getAction(){return action;} public void setAction(String v){this.action=v;}
    public String getEntityType(){return entityType;} public void setEntityType(String v){this.entityType=v;}
    public String getEntityId(){return entityId;} public void setEntityId(String v){this.entityId=v;}
    public String getDetails(){return details;} public void setDetails(String v){this.details=v;}
    public String getIpAddress(){return ipAddress;} public void setIpAddress(String v){this.ipAddress=v;}
    public String getUserAgent(){return userAgent;} public void setUserAgent(String v){this.userAgent=v;}
    public LocalDateTime getTimestamp(){return timestamp;} public void setTimestamp(LocalDateTime v){this.timestamp=v;}
}