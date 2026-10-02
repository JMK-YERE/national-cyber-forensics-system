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
        evidence.setVerified(false);
        evidence.setUploadedBy(uploadedBy);
        evidence.setUploadedAt(LocalDateTime.now());

        Evidence saved = evidenceRepository.save(evidence);

        custodyRepository.save(new ChainOfCustody(
                saved.getId(), "UPLOADED", null, username, "system",
                "Initial evidence upload: " + dto.getDescription()
        ));
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

        custodyRepository.save(new ChainOfCustody(
                evidenceId, "VERIFIED", null, username, "system",
                "Evidence verified by " + username + " | SHA-256: " + evidence.getSha256Hash()
        ));
    }
}