package com.tz.forensics.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evidence")
public class Evidence {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false)
    private Long incidentId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;

    @Column(name = "md5_hash", length = 32)
    private String md5Hash;

    private Boolean encrypted = true;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_device", length = 200)
    private String sourceDevice;

    @Column(name = "acquisition_method", length = 100)
    private String acquisitionMethod;

    @Column(name = "acquisition_type", length = 50)
    private String acquisitionType;

    @Column(name = "device_make", length = 100)
    private String deviceMake;

    @Column(name = "device_model", length = 150)
    private String deviceModel;

    @Column(name = "device_serial", length = 200)
    private String deviceSerial;

    @Column(name = "source_identifier", length = 250)
    private String sourceIdentifier;

    @Column(name = "acquisition_tool", length = 150)
    private String acquisitionTool;

    @Column(name = "acquisition_tool_version", length = 100)
    private String acquisitionToolVersion;

    @Column(name = "write_blocker_used")
    private Boolean writeBlockerUsed = false;

    @Column(name = "acquisition_started_at")
    private LocalDateTime acquisitionStartedAt;

    @Column(name = "acquisition_completed_at")
    private LocalDateTime acquisitionCompletedAt;

    @Column(name = "acquisition_notes", columnDefinition = "TEXT")
    private String acquisitionNotes;

    private Boolean verified = false;

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "custody_status", length = 40)
    private String custodyStatus = "UPLOADED";

    @Column(name = "custodian_id")
    private Long custodianId;

    @Column(name = "custodian_name", length = 100)
    private String custodianName;

    @Column(name = "custodian_role", length = 50)
    private String custodianRole;

    public Evidence() {}

    public String getReadableSize() {
        if (fileSize == null) return "0 B";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        if (fileSize < 1024L * 1024L * 1024L) return String.format("%.1f MB", fileSize / (1024.0 * 1024));
        return String.format("%.2f GB", fileSize / (1024.0 * 1024 * 1024));
    }

    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getIncidentId(){return incidentId;} public void setIncidentId(Long v){incidentId=v;}
    public String getOriginalFilename(){return originalFilename;} public void setOriginalFilename(String v){originalFilename=v;}
    public String getStoredFilename(){return storedFilename;} public void setStoredFilename(String v){storedFilename=v;}
    public String getFileType(){return fileType;} public void setFileType(String v){fileType=v;}
    public Long getFileSize(){return fileSize;} public void setFileSize(Long v){fileSize=v;}
    public String getSha256Hash(){return sha256Hash;} public void setSha256Hash(String v){sha256Hash=v;}
    public String getMd5Hash(){return md5Hash;} public void setMd5Hash(String v){md5Hash=v;}
    public Boolean getEncrypted(){return encrypted;} public void setEncrypted(Boolean v){encrypted=v;}
    public Long getUploadedBy(){return uploadedBy;} public void setUploadedBy(Long v){uploadedBy=v;}
    public LocalDateTime getUploadedAt(){return uploadedAt;} public void setUploadedAt(LocalDateTime v){uploadedAt=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getSourceDevice(){return sourceDevice;} public void setSourceDevice(String v){sourceDevice=v;}
    public String getAcquisitionMethod(){return acquisitionMethod;} public void setAcquisitionMethod(String v){acquisitionMethod=v;}
    public String getAcquisitionType(){return acquisitionType;} public void setAcquisitionType(String v){acquisitionType=v;}
    public String getDeviceMake(){return deviceMake;} public void setDeviceMake(String v){deviceMake=v;}
    public String getDeviceModel(){return deviceModel;} public void setDeviceModel(String v){deviceModel=v;}
    public String getDeviceSerial(){return deviceSerial;} public void setDeviceSerial(String v){deviceSerial=v;}
    public String getSourceIdentifier(){return sourceIdentifier;} public void setSourceIdentifier(String v){sourceIdentifier=v;}
    public String getAcquisitionTool(){return acquisitionTool;} public void setAcquisitionTool(String v){acquisitionTool=v;}
    public String getAcquisitionToolVersion(){return acquisitionToolVersion;} public void setAcquisitionToolVersion(String v){acquisitionToolVersion=v;}
    public Boolean getWriteBlockerUsed(){return writeBlockerUsed;} public void setWriteBlockerUsed(Boolean v){writeBlockerUsed=v;}
    public LocalDateTime getAcquisitionStartedAt(){return acquisitionStartedAt;} public void setAcquisitionStartedAt(LocalDateTime v){acquisitionStartedAt=v;}
    public LocalDateTime getAcquisitionCompletedAt(){return acquisitionCompletedAt;} public void setAcquisitionCompletedAt(LocalDateTime v){acquisitionCompletedAt=v;}
    public String getAcquisitionNotes(){return acquisitionNotes;} public void setAcquisitionNotes(String v){acquisitionNotes=v;}
    public Boolean getVerified(){return verified;} public void setVerified(Boolean v){verified=v;}
    public Long getVerifiedBy(){return verifiedBy;} public void setVerifiedBy(Long v){verifiedBy=v;}
    public LocalDateTime getVerifiedAt(){return verifiedAt;} public void setVerifiedAt(LocalDateTime v){verifiedAt=v;}
    public String getCustodyStatus(){return custodyStatus == null ? "UPLOADED" : custodyStatus;}
    public void setCustodyStatus(String v){custodyStatus=v;}
    public Long getCustodianId(){return custodianId;} public void setCustodianId(Long v){custodianId=v;}
    public String getCustodianName(){return custodianName;} public void setCustodianName(String v){custodianName=v;}
    public String getCustodianRole(){return custodianRole;} public void setCustodianRole(String v){custodianRole=v;}
}