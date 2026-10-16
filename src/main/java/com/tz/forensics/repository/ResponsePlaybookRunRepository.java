package com.tz.forensics.repository;

import com.tz.forensics.entity.ResponsePlaybookRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResponsePlaybookRunRepository extends JpaRepository<ResponsePlaybookRun, Long> {
    List<ResponsePlaybookRun> findByCaseIdOrderByStartedAtDesc(Long caseId);
}
