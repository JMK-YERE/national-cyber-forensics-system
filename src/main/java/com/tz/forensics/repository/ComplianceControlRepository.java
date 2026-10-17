package com.tz.forensics.repository;

import com.tz.forensics.entity.ComplianceControl;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ComplianceControlRepository extends JpaRepository<ComplianceControl, Long> {
    List<ComplianceControl> findByReportIdOrderByControlCodeAsc(String reportId);
    Optional<ComplianceControl> findByIdAndReportId(Long id, String reportId);
    boolean existsByReportIdAndControlCode(String reportId, String controlCode);
}