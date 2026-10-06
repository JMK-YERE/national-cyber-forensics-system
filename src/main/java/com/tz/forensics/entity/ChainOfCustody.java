package com.tz.forensics.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chain_of_custody")
public class ChainOfCustody {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "evidence_id", nullable = false) private Long evidenceId;
    @Column(name = "report_attack_id") private Long reportAttackId;
    @Column(nullable = false, length = 50) private String action;
    @Column(name = "performed_by") private Long performedBy;
    @Column(name = "performed_by_name", length = 100) private String performedByName;
    @Column(name = "performed_by_role", length = 50) private String performedByRole;
    @Column(nullable = false) private LocalDateTime timestamp = LocalDateTime.now();
    @Column(name = "ip_address", length = 45) private String ipAddress;
    @Column(columnDefinition = "TEXT") private String purpose;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(name = "hash_at_action", length = 64) private String hashAtAction;
    @Column(name = "digital_signature", length = 512) private String digitalSignature;

    public ChainOfCustody() {}
    public ChainOfCustody(Long evidenceId, String action, Long performedBy, String performedByName, String ipAddress, String purpose) {
        this.evidenceId=evidenceId; this.action=action; this.performedBy=performedBy; this.performedByName=performedByName; this.ipAddress=ipAddress; this.purpose=purpose;
    }
    @PreUpdate private void preventUpdate(){ throw new IllegalStateException("Chain-of-custody records are immutable."); }
    @PreRemove private void preventDelete(){ throw new IllegalStateException("Chain-of-custody records cannot be deleted."); }

    public Long getId(){return id;} public void setId(Long v){this.id=v;}
    public Long getEvidenceId(){return evidenceId;} public void setEvidenceId(Long v){this.evidenceId=v;}
    public Long getReportAttackId(){return reportAttackId;} public void setReportAttackId(Long v){this.reportAttackId=v;}
    public String getAction(){return action;} public void setAction(String v){this.action=v;}
    public Long getPerformedBy(){return performedBy;} public void setPerformedBy(Long v){this.performedBy=v;}
    public String getPerformedByName(){return performedByName;} public void setPerformedByName(String v){this.performedByName=v;}
    public String getPerformedByRole(){return performedByRole;} public void setPerformedByRole(String v){this.performedByRole=v;}
    public LocalDateTime getTimestamp(){return timestamp;} public void setTimestamp(LocalDateTime v){this.timestamp=v;}
    public String getIpAddress(){return ipAddress;} public void setIpAddress(String v){this.ipAddress=v;}
    public String getPurpose(){return purpose;} public void setPurpose(String v){this.purpose=v;}
    public String getNotes(){return notes;} public void setNotes(String v){this.notes=v;}
    public String getHashAtAction(){return hashAtAction;} public void setHashAtAction(String v){this.hashAtAction=v;}
    public String getDigitalSignature(){return digitalSignature;} public void setDigitalSignature(String v){this.digitalSignature=v;}
}