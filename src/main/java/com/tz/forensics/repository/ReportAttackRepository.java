package com.tz.forensics.repository;

import com.tz.forensics.entity.ReportAttack;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ReportAttackRepository extends JpaRepository<ReportAttack, Long> {
    List<ReportAttack> findAllByOrderByCreatedAtDesc();
    List<ReportAttack> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ReportAttack> findByStatusOrderByCreatedAtDesc(String status);
    List<ReportAttack> findByRegionOrderByCreatedAtDesc(String region);
    List<ReportAttack> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime since);
    long countByStatus(String status);
    long countByCreatedAtAfter(LocalDateTime since);
}
