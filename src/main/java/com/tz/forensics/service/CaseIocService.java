package com.tz.forensics.service;

import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.entity.CaseIoc;
import com.tz.forensics.repository.CaseFileRepository;
import com.tz.forensics.repository.CaseIocRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class CaseIocService {
    private static final Set<String> TYPES = Set.of("IP", "DOMAIN", "URL", "HASH", "EMAIL", "MALWARE", "OTHER");
    private static final Set<String> CONFIDENCE = Set.of("LOW", "MEDIUM", "HIGH");
    private final CaseIocRepository repository;
    private final CaseFileRepository caseFileRepository;

    public CaseIocService(CaseIocRepository repository, CaseFileRepository caseFileRepository) {
        this.repository = repository;
        this.caseFileRepository = caseFileRepository;
    }

    public List<CaseIoc> findByCaseId(Long caseId) {
        return caseId == null ? List.of() : repository.findByCaseIdOrderByCreatedAtDesc(caseId);
    }

    @Transactional
    public CaseIoc create(Long caseId, String type, String value, String confidence, String source,
                          LocalDateTime firstSeen, LocalDateTime lastSeen, String notes,
                          Long userId, String userName) {
        if (caseId == null || userId == null || type == null || value == null || type.isBlank() || value.isBlank()) return null;
        CaseFile cf = caseFileRepository.findById(caseId).orElse(null);
        if (cf == null || isClosed(cf)) return null;

        String normalizedType = type.trim().toUpperCase();
        if (!TYPES.contains(normalizedType)) return null;
        String normalizedConfidence = confidence == null || confidence.isBlank() ? "MEDIUM" : confidence.trim().toUpperCase();
        if (!CONFIDENCE.contains(normalizedConfidence)) return null;
        String normalizedValue = value.trim();
        if (normalizedValue.length() > 2048) return null;
        if (source != null && source.trim().length() > 500) return null;
        if (notes != null && notes.trim().length() > 5000) return null;
        if (firstSeen != null && lastSeen != null && lastSeen.isBefore(firstSeen)) return null;

        CaseIoc i = new CaseIoc();
        i.setCaseId(caseId);
        i.setIocType(normalizedType);
        i.setValue(normalizedValue);
        i.setConfidence(normalizedConfidence);
        i.setSource(source == null ? null : source.trim());
        i.setFirstSeen(firstSeen);
        i.setLastSeen(lastSeen);
        i.setNotes(notes == null ? null : notes.trim());
        i.setCreatedBy(userId);
        i.setCreatedByName(userName);
        return repository.save(i);
    }

    @Transactional
    public boolean delete(Long id, Long caseId) {
        if (id == null || caseId == null) return false;
        CaseFile cf = caseFileRepository.findById(caseId).orElse(null);
        if (cf == null || isClosed(cf)) return false;
        CaseIoc i = repository.findById(id).orElse(null);
        if (i == null || !caseId.equals(i.getCaseId())) return false;
        repository.delete(i);
        return true;
    }

    private boolean isClosed(CaseFile cf) {
        String status = cf.getStatus() == null ? "OPEN" : cf.getStatus().trim().toUpperCase();
        return "CLOSED".equals(status) || "ARCHIVED".equals(status);
    }
}