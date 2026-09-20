package com.tz.forensics.service;

import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.repository.CaseFileRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
public class CaseFileService {

    private final CaseFileRepository caseFileRepository;

    public CaseFileService(CaseFileRepository caseFileRepository) {
        this.caseFileRepository = caseFileRepository;
    }

    public CaseFile createCase(Long incidentId, String title, String description,
                                String priority, Long createdBy, String createdByName) {
        CaseFile caseFile = new CaseFile();
        String caseNumber = "CASE-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + (100 + new Random().nextInt(900));
        caseFile.setCaseNumber(caseNumber);
        caseFile.setIncidentId(incidentId);
        caseFile.setTitle(title);
        caseFile.setDescription(description);
        caseFile.setPriority(priority != null ? priority : "MEDIUM");
        caseFile.setStatus("OPEN");
        caseFile.setCreatedBy(createdBy);
        caseFile.setCreatedByName(createdByName);
        return caseFileRepository.save(caseFile);
    }

    public List<CaseFile> getAllCases() {
        return caseFileRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<CaseFile> getCasesByStatus(String status) {
        return caseFileRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<CaseFile> getMyCases(Long userId) {
        return caseFileRepository.findByAssignedToOrderByCreatedAtDesc(userId);
    }

    public CaseFile getById(Long id) {
        return caseFileRepository.findById(id).orElse(null);
    }

    public void updateStatus(Long caseId, String status, String closureReason) {
        CaseFile cf = caseFileRepository.findById(caseId).orElse(null);
        if (cf != null) {
            cf.setStatus(status);
            cf.setUpdatedAt(LocalDateTime.now());
            if ("CLOSED".equals(status)) {
                cf.setClosedAt(LocalDateTime.now());
                cf.setClosureReason(closureReason);
            }
            caseFileRepository.save(cf);
        }
    }

    public long countOpen() { return caseFileRepository.countByStatus("OPEN"); }
    public long countInvestigating() { return caseFileRepository.countByStatus("INVESTIGATING"); }
    public long countClosed() { return caseFileRepository.countByStatus("CLOSED"); }
}
