package com.tz.forensics.repository;

import com.tz.forensics.entity.CaseTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CaseTimelineRepository extends JpaRepository<CaseTimeline, Long> {
    List<CaseTimeline> findByCaseIdOrderByCreatedAtDesc(Long caseId);
}