package com.tz.forensics.repository;

import com.tz.forensics.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {
    List<Evidence> findByIncidentIdOrderByUploadedAtDesc(Long incidentId);
}
