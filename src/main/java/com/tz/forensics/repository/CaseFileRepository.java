package com.tz.forensics.repository;

import com.tz.forensics.entity.CaseFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CaseFileRepository extends JpaRepository<CaseFile, Long> {
    List<CaseFile> findAllByOrderByCreatedAtDesc();
    List<CaseFile> findByStatusOrderByCreatedAtDesc(String status);
    List<CaseFile> findByAssignedToOrderByCreatedAtDesc(Long assignedTo);
    long countByStatus(String status);
}
