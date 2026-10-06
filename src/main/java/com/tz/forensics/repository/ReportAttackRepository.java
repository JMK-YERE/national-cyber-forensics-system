package com.tz.forensics.repository;

import com.tz.forensics.entity.ReportAttack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportAttackRepository extends JpaRepository<ReportAttack, Long> {
    List<ReportAttack> findAllByOrderByCreatedAtDesc();
    List<ReportAttack> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ReportAttack> findByAssignedToOrderByCreatedAtDesc(Long assignedTo);
    List<ReportAttack> findByStatusOrderByCreatedAtDesc(String status);
    boolean existsByReportId(String reportId);
    long countByStatus(String status);
    long countByAssignedToAndStatus(Long assignedTo, String status);
    long countByCreatedAtAfter(LocalDateTime date);
    long countByAssignedToAndCreatedAtAfter(Long assignedTo, LocalDateTime date);
    long countByAssignedTo(Long assignedTo);
    List<ReportAttack> findTop10ByOrderByCreatedAtDesc();
}
