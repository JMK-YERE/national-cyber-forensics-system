package com.tz.forensics.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tenant_audit_events", indexes = {
    @Index(name = "ix_tenant_audit_tenant_time", columnList = "tenant_id, occurred_at"),
    @Index(name = "ix_tenant_audit_resource", columnList = "resource_type, resource_id")
})
public class TenantAuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "resource_id", length = 160)
    private String resourceId;

    @Column(nullable = false, length = 30)
    private String outcome = "SUCCESS";

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt = OffsetDateTime.now();

    @Column(name = "previous_event_hash", length = 64)
    private String previousEventHash;

    @Column(name = "event_hash", length = 64)
    private String eventHash;

    public TenantAuditEvent() {}

    @PreUpdate
    private void preventUpdate() { throw new IllegalStateException("Tenant audit events are immutable."); }

    @PreRemove
    private void preventDelete() { throw new IllegalStateException("Tenant audit events cannot be deleted."); }

    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }
    public String getPreviousEventHash() { return previousEventHash; }
    public void setPreviousEventHash(String previousEventHash) { this.previousEventHash = previousEventHash; }
    public String getEventHash() { return eventHash; }
    public void setEventHash(String eventHash) { this.eventHash = eventHash; }
}
