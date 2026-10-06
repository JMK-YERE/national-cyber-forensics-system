package com.tz.forensics.service;

import com.tz.forensics.entity.CaseCommunication;
import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.repository.CaseCommunicationRepository;
import com.tz.forensics.repository.CaseFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class CaseCommunicationService {
    private static final Set<String> TLP = Set.of("CLEAR", "GREEN", "AMBER", "RED");
    private final CaseCommunicationRepository repository;
    private final CaseFileRepository caseFileRepository;

    public CaseCommunicationService(CaseCommunicationRepository repository, CaseFileRepository caseFileRepository) {
        this.repository = repository;
        this.caseFileRepository = caseFileRepository;
    }

    public List<CaseCommunication> findByCaseId(Long caseId) {
        return caseId == null ? List.of() : repository.findByCaseIdOrderByCreatedAtAsc(caseId);
    }

    @Transactional
    public CaseCommunication create(Long caseId, Long authorId, String authorName, String authorRole,
                                    String message, String tlp) {
        if (caseId == null || authorId == null || message == null || message.isBlank()) return null;
        CaseFile cf = caseFileRepository.findById(caseId).orElse(null);
        if (cf == null) return null;
        String caseStatus = cf.getStatus() == null ? "OPEN" : cf.getStatus().trim().toUpperCase();
        if ("CLOSED".equals(caseStatus) || "ARCHIVED".equals(caseStatus)) return null;

        String level = tlp == null ? "CLEAR" : tlp.trim().toUpperCase();
        if (!TLP.contains(level)) return null;
        String body = message.trim();
        if (body.length() > 10000) return null;

        return repository.save(new CaseCommunication(caseId, authorId, authorName, authorRole, body, level));
    }
}