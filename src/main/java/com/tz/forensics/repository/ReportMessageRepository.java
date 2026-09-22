package com.tz.forensics.repository;

import com.tz.forensics.entity.ReportMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportMessageRepository extends JpaRepository<ReportMessage, Long> {
    List<ReportMessage> findByReportIdOrderByCreatedAtAsc(Long reportId);
    long countByReportIdAndIsReadFalse(Long reportId);
}
