package com.tz.forensics.service;

import com.tz.forensics.dto.EvidenceDto;
import com.tz.forensics.entity.ChainOfCustody;
import com.tz.forensics.entity.Evidence;
import com.tz.forensics.repository.ChainOfCustodyRepository;
import com.tz.forensics.repository.EvidenceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

@Service
public class EvidenceService {
    private final EvidenceRepository evidenceRepository;
    private final ChainOfCustodyRepository custodyRepository;
    private final HashService hashService;
    private final EncryptionService encryptionService;

    @Value("$" + "{app.upload.dir}")
    private String uploadDir;

    public EvidenceService(EvidenceRepository evidenceRepository,
                           ChainOfCustodyRepository custodyRepository,
                           HashService hashService,
                           EncryptionService encryptionService) {
        this.evidenceRepository = evidenceRepository;
        this.custodyRepository = custodyRepository;
        this.hashService = hashService;
        this.encryptionService = encryptionService;
    }

    public Evidence uploadEvidence(Long incidentId, MultipartFile file,
                                   EvidenceDto dto, String username, Long uploadedBy) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Evidence file is empty.");

        byte[] fileBytes = file.getBytes();
        String sha256 = hashService.sha256(fileBytes);
        String md5 = hashService.md5(fileBytes);
        byte[] encrypted = encryptionService.encrypt(fileBytes);

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        String storedFilename = UUID.randomUUID() + ".enc";
        Files.write(uploadPath.resolve(storedFilename), encrypted);

        Evidence evidence = new Evidence();
        evidence.setIncidentId(incidentId);
        evidence.setOriginalFilename(file.getOriginalFilename() == null ? "evidence.bin" : file.getOriginalFilename());
        evidence.setStoredFilename(storedFilename);
        evidence.setFileType(file.getContentType());
        evidence.setFileSize(file.getSize());
        evidence.setSha256Hash(sha256);
        evidence.setMd5Hash(md5);
        evidence.setEncrypted(true);
        evidence.setDescription(dto.getDescription());
        evidence.setSourceDevice(dto.getSourceDevice());
        evidence.setAcquisitionMethod(dto.getAcquisitionMethod());
        evidence.setAcquisitionType(dto.getAcquisitionType());
        evidence.setDeviceMake(dto.getDeviceMake());
        evidence.setDeviceModel(dto.getDeviceModel());
        evidence.setDeviceSerial(dto.getDeviceSerial());
        evidence.setSourceIdentifier(dto.getSourceIdentifier());
        evidence.setAcquisitionTool(dto.getAcquisitionTool());
        evidence.setAcquisitionToolVersion(dto.getAcquisitionToolVersion());
        evidence.setWriteBlockerUsed(Boolean.TRUE.equals(dto.getWriteBlockerUsed()));
        evidence.setAcquisitionNotes(dto.getAcquisitionNotes());
        evidence.setAcquisitionStartedAt(LocalDateTime.now());
        evidence.setAcquisitionCompletedAt(LocalDateTime.now());
        evidence.setVerified(false);
        evidence.setUploadedBy(uploadedBy);
        evidence.setUploadedAt(LocalDateTime.now());

        Evidence saved = evidenceRepository.save(evidence);

        ChainOfCustody uploadedEvent = new ChainOfCustody(
                saved.getId(), "UPLOADED", uploadedBy, username, getClientIp(),
                "Initial evidence upload: " + dto.getDescription()
        );
        uploadedEvent.setHashAtAction(sha256);
        custodyRepository.save(uploadedEvent);
        return saved;
    }

    public List<Evidence> getEvidenceByIncident(Long incidentId) {
        return evidenceRepository.findByIncidentIdOrderByUploadedAtDesc(incidentId);
    }

    public Evidence getById(Long evidenceId) {
        return evidenceRepository.findById(evidenceId).orElse(null);
    }

    public byte[] downloadEvidence(Long evidenceId) throws IOException {
        Evidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new RuntimeException("Evidence not found"));
        Path filePath = Paths.get(uploadDir, evidence.getStoredFilename());
        if (!Files.exists(filePath)) throw new IOException("Stored evidence file not found.");
        return encryptionService.decrypt(Files.readAllBytes(filePath));
    }
 
    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            HttpServletRequest request = attrs.getRequest();
            String xff = request.getHeader("X-Forwarded-For");
            return xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public List<ChainOfCustody> getChainOfCustody(Long evidenceId) {
        return custodyRepository.findByEvidenceIdOrderByTimestampDesc(evidenceId);
    }

    public void verifyEvidence(Long evidenceId, String username, Long verifiedBy) {
        Evidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new RuntimeException("Evidence not found"));
        evidence.setVerified(true);
        evidence.setVerifiedBy(verifiedBy);
        evidence.setVerifiedAt(LocalDateTime.now());
        evidenceRepository.save(evidence);

        ChainOfCustody verifiedEvent = new ChainOfCustody(
                evidenceId, "VERIFIED", verifiedBy, username, getClientIp(),
                "Evidence verified by " + username + " | SHA-256: " + evidence.getSha256Hash()
        );
        verifiedEvent.setHashAtAction(evidence.getSha256Hash());
        custodyRepository.save(verifiedEvent);
    }
}