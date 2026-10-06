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
        return uploadEvidence(incidentId, null, file, dto, username, uploadedBy);
    }

    public Evidence uploadEvidence(Long incidentId, Long caseId, MultipartFile file,
                                   EvidenceDto dto, String username, Long uploadedBy) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Evidence file is empty.");
        if (file.getSize() > 50L * 1024L * 1024L) throw new IllegalArgumentException("Evidence file exceeds the 50 MB limit.");

        byte[] fileBytes = file.getBytes();
        String sha256 = hashService.sha256(fileBytes);
        String md5 = hashService.md5(fileBytes);
        byte[] encrypted = encryptionService.encrypt(fileBytes);

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        String storedFilename = UUID.randomUUID() + ".enc";
        Path storedPath = uploadPath.resolve(storedFilename);
        try {
            Files.write(storedPath, encrypted);
        } catch (IOException e) {
            throw new IOException("Unable to persist encrypted evidence file.", e);
        }

        Evidence evidence = new Evidence();
        evidence.setIncidentId(incidentId);
        evidence.setCaseId(caseId);
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
        evidence.setCustodyStatus("UPLOADED");
        evidence.setCustodianId(uploadedBy);
        evidence.setCustodianName(username);
        evidence.setCustodianRole("EVIDENCE_CUSTODIAN");
        evidence.setUploadedBy(uploadedBy);
        evidence.setUploadedAt(LocalDateTime.now());

        Evidence saved;
        try {
            saved = evidenceRepository.save(evidence);
        } catch (RuntimeException e) {
            try { Files.deleteIfExists(storedPath); } catch (IOException ignored) { }
            throw e;
        }

        ChainOfCustody uploadedEvent = new ChainOfCustody(
                saved.getId(), "UPLOADED", uploadedBy, username, getClientIp(),
                "Initial evidence upload: " + dto.getDescription()
        );
        uploadedEvent.setPerformedByRole("EVIDENCE_CUSTODIAN");
        uploadedEvent.setHashAtAction(sha256);
        try {
            custodyRepository.save(uploadedEvent);
        } catch (RuntimeException e) {
            try { evidenceRepository.delete(saved); } catch (RuntimeException ignored) { }
            try { Files.deleteIfExists(storedPath); } catch (IOException ignored) { }
            throw e;
        }
        return saved;
    }

    public List<Evidence> getEvidenceByIncident(Long incidentId) {
        return evidenceRepository.findByIncidentIdOrderByUploadedAtDesc(incidentId);
    }

    public List<Evidence> getEvidenceByCase(Long caseId) {
        if (caseId == null) return List.of();
        return evidenceRepository.findByCaseIdOrderByUploadedAtDesc(caseId);
    }

    public Evidence getById(Long evidenceId) {
        return evidenceRepository.findById(evidenceId).orElse(null);
    }

    public byte[] downloadEvidence(Long evidenceId) throws IOException {
        Evidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new RuntimeException("Evidence not found"));
        Path filePath = Paths.get(uploadDir, evidence.getStoredFilename());
        if (!Files.exists(filePath)) throw new IOException("Stored evidence file not found.");
        byte[] decrypted = encryptionService.decrypt(Files.readAllBytes(filePath));
        String actualSha256 = hashService.sha256(decrypted);
        if (!actualSha256.equalsIgnoreCase(evidence.getSha256Hash())) {
            throw new IOException("Evidence integrity check failed: SHA-256 mismatch.");
        }
        return decrypted;
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


    public boolean transferCustody(Long evidenceId, Long actorId, String actorName, String actorRole,
                                   Long recipientId, String recipientName, String recipientRole,
                                   String purpose, String notes) {
        Evidence evidence = evidenceRepository.findById(evidenceId).orElse(null);
        if (evidence == null || actorId == null || recipientId == null) return false;
        String status = evidence.getCustodyStatus();
        if (!"VERIFIED".equals(status) && !"ACCEPTED".equals(status) && !"EXAMINED".equals(status)
                && !"REPORT_GENERATED".equals(status)) return false;
        if (recipientId.equals(actorId)) return false;

        evidence.setCustodyStatus("TRANSFERRED");
        evidence.setCustodianId(recipientId);
        evidence.setCustodianName(recipientName);
        evidence.setCustodianRole(recipientRole);
        evidenceRepository.save(evidence);

        ChainOfCustody event = new ChainOfCustody(
                evidenceId, "TRANSFERRED", actorId, actorName, getClientIp(),
                purpose == null || purpose.isBlank() ? "Evidence custody transferred" : purpose
        );
        event.setPerformedByRole(actorRole);
        event.setNotes(notes);
        event.setHashAtAction(evidence.getSha256Hash());
        custodyRepository.save(event);
        return true;
    }

    public boolean advanceCustody(Long evidenceId, String action, Long actorId, String actorName,
                                  String actorRole, String purpose, String notes) {
        Evidence evidence = evidenceRepository.findById(evidenceId).orElse(null);
        if (evidence == null || actorId == null || action == null) return false;

        String from = evidence.getCustodyStatus();
        String to = action.trim().toUpperCase();
        boolean actorIsCustodian = actorId.equals(evidence.getCustodianId());
        boolean allowed = switch (from) {
            case "UPLOADED" -> "VERIFIED".equals(to);
            case "TRANSFERRED" -> "ACCEPTED".equals(to);
            case "ACCEPTED" -> "UNDER_EXAMINATION".equals(to);
            case "UNDER_EXAMINATION" -> "EXAMINED".equals(to);
            case "EXAMINED" -> "REPORT_GENERATED".equals(to);
            case "VERIFIED" -> false;
            case "REPORT_GENERATED" -> false;
            default -> false;
        };
        if (!allowed) return false;

        if ("ACCEPTED".equals(to) && !actorIsCustodian) return false;
        if ("UNDER_EXAMINATION".equals(to) || "EXAMINED".equals(to)) {
            if (!actorIsCustodian) return false;
        }

        evidence.setCustodyStatus(to);
        evidenceRepository.save(evidence);

        ChainOfCustody event = new ChainOfCustody(
                evidenceId, to, actorId, actorName, getClientIp(),
                purpose == null || purpose.isBlank() ? "Evidence custody action: " + to : purpose
        );
        event.setPerformedByRole(actorRole);
        event.setNotes(notes);
        event.setHashAtAction(evidence.getSha256Hash());
        custodyRepository.save(event);
        return true;
    }

    public void verifyEvidence(Long evidenceId, String username, Long verifiedBy) {
        Evidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new RuntimeException("Evidence not found"));
        if (Boolean.TRUE.equals(evidence.getVerified())) return;
        evidence.setVerified(true);
        evidence.setCustodyStatus("VERIFIED");
        evidence.setVerifiedBy(verifiedBy);
        evidence.setVerifiedAt(LocalDateTime.now());
        evidenceRepository.save(evidence);

        ChainOfCustody verifiedEvent = new ChainOfCustody(
                evidenceId, "VERIFIED", verifiedBy, username, getClientIp(),
                "Evidence verified by " + username + " | SHA-256: " + evidence.getSha256Hash()
        );
        verifiedEvent.setPerformedByRole("VERIFIER");
        verifiedEvent.setHashAtAction(evidence.getSha256Hash());
        custodyRepository.save(verifiedEvent);
    }
}