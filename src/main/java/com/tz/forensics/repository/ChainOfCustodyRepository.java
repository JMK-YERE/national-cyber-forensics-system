package com.tz.forensics.repository;

import com.tz.forensics.entity.ChainOfCustody;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChainOfCustodyRepository extends JpaRepository<ChainOfCustody, Long> {
    List<ChainOfCustody> findByEvidenceIdOrderByTimestampDesc(Long evidenceId);
}
