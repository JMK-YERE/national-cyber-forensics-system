package com.tz.forensics.repository;
import com.tz.forensics.entity.CaseTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CaseTaskRepository extends JpaRepository<CaseTask,Long>{
    List<CaseTask> findByCaseIdOrderByCreatedAtDesc(Long caseId);
    List<CaseTask> findByAssignedToOrderByCreatedAtDesc(Long assignedTo);
    long countByAssignedToAndStatusNotIn(Long assignedTo,List<String> statuses);
}