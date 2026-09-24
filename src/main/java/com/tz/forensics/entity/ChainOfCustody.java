package com.tz.forensics.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chain_of_custody")
public class ChainOfCustody {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evidence_id", nullable = false)
    private Long evidenceId;

    @Column(name = "report_attack_id")
    private Long reportAttackId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "performed_by")
    private Long performedBy;

    @Column(name = "performed_by_name", length = 100)
    private String performedByName;

    @Column(name = "performed_by_role", length = 50)
    private String performedByRole;

    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String purpose;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "hash_at_action", length = 64)
    private String hashAtAction;

    @Column(name = "digital_signature", length = 512)
    private String digitalSignature;

    public ChainOfCustody() {}

    public ChainOfCustody(Long evidenceId, String action, Long performedBy,
                          String performedByName, String ipAddress, String purpose) {
        this.evidenceId = evidenceId;
        this.action = action;
        this.performedBy = performedBy;
        this.performedByName = performedByName;
        this.ipAddress = ipAddress;
        this.purpose = purpose;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEvidenceId() { return evidenceId; }
    public void setEvidenceId(Long evidenceId) { this.evidenceId = evidenceId; }
    public Long getReportAttackId() { return reportAttackId; }
    public void setReportAttackId(Long reportAttackId) { this.reportAttackId = reportAttackId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Long getPerformedBy() { return performedBy; }
    public void setPerformedBy(Long performedBy) { this.performedBy = performedBy; }
    public String getPerformedByName() { return performedByName; }
    public void setPerformedByName(String performedByName) { this.performedByName = performedByName; }
    public String getPerformedByRole() { return performedByRole; }
    public void setPerformedByRole(String performedByRole) { this.performedByRole = performedByRole; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getHashAtAction() { return hashAtAction; }
    public void setHashAtAction(String hashAtAction) { this.hashAtAction = hashAtAction; }
    public String getDigitalSignature() { return digitalSignature; }
    public void setDigitalSignature(String digitalSignature) { this.digitalSignature = digitalSignature; }
}
