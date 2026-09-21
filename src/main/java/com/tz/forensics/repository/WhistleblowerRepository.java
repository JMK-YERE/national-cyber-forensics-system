package com.tz.forensics.repository;

import com.tz.forensics.entity.WhistleblowerReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WhistleblowerRepository extends JpaRepository<WhistleblowerReport, Long> {
    Optional<WhistleblowerReport> findByTrackingCode(String trackingCode);
    List<WhistleblowerReport> findAllByOrderByCreatedAtDesc();
    List<WhistleblowerReport> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
}
