package com.tz.forensics.repository;

import com.tz.forensics.entity.WhistleblowerMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WhistleblowerMessageRepository extends JpaRepository<WhistleblowerMessage, Long> {
    List<WhistleblowerMessage> findByReportIdOrderByCreatedAtAsc(Long reportId);
}
