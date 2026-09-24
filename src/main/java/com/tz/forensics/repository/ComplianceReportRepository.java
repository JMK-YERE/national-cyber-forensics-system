package com.tz.forensics.repository;

import com.tz.forensics.entity.ComplianceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplianceReportRepository extends JpaRepository<ComplianceReport, Long> {
    List<ComplianceReport> findAllByOrderByCreatedAtDesc();
    List<ComplianceReport> findByReportTypeOrderByCreatedAtDesc(String reportType);
}
