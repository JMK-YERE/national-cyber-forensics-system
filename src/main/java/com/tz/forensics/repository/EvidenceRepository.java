package com.tz.forensics.repository;

import com.tz.forensics.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {
    List<Evidence> findByIncidentIdOrderByUploadedAtDesc(Long incidentId);
    java.util.Optional<Evidence> findByIdAndTenantId(Long id, Long tenantId);
    List<Evidence> findByTenantIdAndIncidentIdOrderByUploadedAtDesc(Long tenantId, Long incidentId);
    List<Evidence> findByTenantIdAndCaseIdOrderByUploadedAtDesc(Long tenantId, Long caseId);
    List<Evidence> findByCaseIdOrderByUploadedAtDesc(Long caseId);
}
