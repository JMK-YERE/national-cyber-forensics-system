package com.tz.forensics.repository;

import com.tz.forensics.entity.ResponsePlaybookStepRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ResponsePlaybookStepRunRepository extends JpaRepository<ResponsePlaybookStepRun, Long> {
    List<ResponsePlaybookStepRun> findByRunIdOrderByStepOrderAsc(Long runId);
    Optional<ResponsePlaybookStepRun> findByIdAndRunId(Long id, Long runId);
    long countByRunIdAndStatusNot(Long runId, String status);
}
