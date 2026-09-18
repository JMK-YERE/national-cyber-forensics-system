package com.tz.forensics.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evidence")
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(name = "source_device")
    private String sourceDevice;

    @Column(name = "acquisition_method")
    private String acquisitionMethod;

    private Boolean verified = false;

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    public Evidence() {}

    public String getReadableSize() {
        if (fileSize == null) return "0 B";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }
    public String getMd5Hash() { return md5Hash; }
    public void setMd5Hash(String md5Hash) { this.md5Hash = md5Hash; }
    public Boolean getEncrypted() { return encrypted; }
    public void setEncrypted(Boolean encrypted) { this.encrypted = encrypted; }
    public Long getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(Long uploadedBy) { this.uploadedBy = uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSourceDevice() { return sourceDevice; }
    public void setSourceDevice(String sourceDevice) { this.sourceDevice = sourceDevice; }
    public String getAcquisitionMethod() { return acquisitionMethod; }
    public void setAcquisitionMethod(String acquisitionMethod) { this.acquisitionMethod = acquisitionMethod; }
    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
    public Long getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(Long verifiedBy) { this.verifiedBy = verifiedBy; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
}
